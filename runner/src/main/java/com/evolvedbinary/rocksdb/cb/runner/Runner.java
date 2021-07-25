package com.evolvedbinary.rocksdb.cb.runner;

import com.evolvedbinary.rocksdb.cb.common.PathUtil;
import com.evolvedbinary.rocksdb.cb.dataobject.*;
import com.evolvedbinary.rocksdb.cb.jms.AbstractJMSService;
import com.evolvedbinary.rocksdb.cb.jms.JMSServiceState;
import com.evolvedbinary.rocksdb.cb.runner.benchmarker.BenchmarkResult;
import com.evolvedbinary.rocksdb.cb.runner.benchmarker.Benchmarker;
import com.evolvedbinary.rocksdb.cb.runner.benchmarker.JavaProcessBenchmarkerImpl;
import com.evolvedbinary.rocksdb.cb.runner.builder.BuildResult;
import com.evolvedbinary.rocksdb.cb.runner.builder.Builder;
import com.evolvedbinary.rocksdb.cb.runner.builder.JavaProcessBuilderImpl;
import com.evolvedbinary.rocksdb.cb.scm.GitHelper;
import com.evolvedbinary.rocksdb.cb.scm.GitHelperException;
import com.evolvedbinary.rocksdb.cb.scm.JGitGitHelperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.jms.*;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.evolvedbinary.rocksdb.cb.common.MapUtil.Entry;
import static com.evolvedbinary.rocksdb.cb.common.MapUtil.Map;

class Runner extends AbstractJMSService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);
    private static final AtomicReference<JMSServiceState> STATE = new AtomicReference<>(JMSServiceState.IDLE);

    private static final String MAIN_GIT_BRANCH = "master";
    private static final String REPO_DIR_NAME = "repo";
    private static final String LOG_DIR_NAME = "log";
    private static final String DB_DIR_NAME = "db";
    private static final String WAL_DIR_NAME = "wal";
    private static final List<String> DEFAULT_MAKE_TARGETS = Arrays.asList("db_bench");
    private static final Map<String, String> DEFAULT_BENCHMARK_ENV = Map(Entry("NUM_KEYS", "10000"));
    private static final List<String> DEFAULT_BENCHMARK_ARGS = Arrays.asList("fillseq_enable_wal");

    private final Settings settings;
    private final String clientId;
    private final BuildRequestQueueMessageListener buildRequestQueueMessageListener = new BuildRequestQueueMessageListener();

    public Runner(final Settings settings) {
        this.settings = settings;
        this.clientId = "runner" + UUID.randomUUID();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected AtomicReference<JMSServiceState> getState() {
        return STATE;
    }

    @Override
    protected String getClientId() {
        return clientId ;
    }

    @Override
    protected List<String> getQueueNames() {
        return Arrays.asList(
                settings.buildRequestQueueName,
                settings.buildResponseQueueName
        );
    }

    @Nullable
    @Override
    protected MessageListener getListener(final String queueName) {
        if (settings.buildRequestQueueName.equals(queueName)) {
            return buildRequestQueueMessageListener;

        }

        return null;
    }

    private class BuildRequestQueueMessageListener implements MessageListener {
        @Override
        public void onMessage(final Message message) {
            if (!(message instanceof TextMessage)) {
                // acknowledge invalid message so that it is removed from the queue
                if (acknowledgeMessage(message)) {
                    LOGGER.error("Discarded message with unexpected type {} from Queue: {}.", message.getClass().getName(), settings.buildRequestQueueName);
                }

                // can't process non-text message, so DONE
                return;
            }

            final TextMessage textMessage = (TextMessage) message;
            final String content;
            try {
                content = textMessage.getText();
            } catch (final JMSException e) {
                LOGGER.error("Could not get content of TextMessage from Queue: {}. Error: {}", settings.buildRequestQueueName, e.getMessage(), e);

                // can't access message content, so DONE
                return;
            }

            // attempt to parse as BuildRequest
            final BuildRequest buildRequest;
            try {
                buildRequest = new BuildRequest().deserialize(content);
            } catch (final IOException e) {
                // unable to deserialize, acknowledge invalid message so that it is removed from the queue
                if (acknowledgeMessage(message)) {
                    LOGGER.error("Discarded message with unexpected format from Queue: {}. Error: {}. Content: '{}'", settings.buildRequestQueueName, e.getMessage(), content);
                }
                return;
            }

            // TODO(AR) if we are already building, should we start another build (i.e. ack/no-ack)... or isn't this already controlled in the orchestrator already?

            if (!acknowledgeMessage(message)) {
                LOGGER.error("Unable to acknowledge message from Queue: {}. Content: '{}'. Skipping...", settings.buildRequestQueueName, content);
                return;
            }

            // 1) do some sanity checks?
            // TODO(AR)

            final BuildStats buildStats = new BuildStats();

            // 2) Send UPDATING_SOURCE
            if (!sendUpdatedBuildStatus(BuildState.UPDATING_SOURCE, buildRequest, buildStats)) {
                return;  // nothing more can be done!
            }

            final Path repoDir = settings.dataDir.resolve(REPO_DIR_NAME);
            final Path projectRepoDir = repoDir.resolve(buildRequest.getRepository());

            // 3) Checkout/Fetch repo with JGit to update the source
            final long updateSourceStartTime = System.currentTimeMillis();
            GitHelper gitHelper = null;
            try {
                if (!Files.exists(projectRepoDir)) {
                    // clone the remote repo
                    final String repoUri = "https://github.com/" + buildRequest.getRepository();
                    gitHelper = JGitGitHelperImpl.clone(repoUri, projectRepoDir, MAIN_GIT_BRANCH);

                } else {
                    // fetch the latest from the remote repo
                    gitHelper = JGitGitHelperImpl.open(projectRepoDir).fetch();
                }

                // discard any unstaged changes (perhaps accumulated in a previous run), i.e. `git clean -fdx`
                gitHelper = gitHelper.cleanAll();

                // checkout the branch
                gitHelper = gitHelper.checkout(buildRequest.getCommit());

            } catch (final GitHelperException e) {
                LOGGER.error("Unable to open/update Git repo: {}. Error: {}",buildRequest.getRepository(), e.getMessage(), e);

                // send UPDATING_SOURCE_FAILED
                buildStats.setUpdateSourceTime(System.currentTimeMillis() - updateSourceStartTime);
                sendFailureBuildStatus(BuildState.UPDATING_SOURCE_FAILED, buildRequest, buildStats, e);

                return;  // nothing more can be done!

            } finally {
                if (gitHelper != null) {
                    gitHelper.close();
                }
            }

            // record the total time taken for updating the source code
            buildStats.setUpdateSourceTime(System.currentTimeMillis() - updateSourceStartTime);

            // 4) Send UPDATING_SOURCE_COMPLETE
            if (!sendUpdatedBuildStatus(BuildState.UPDATING_SOURCE_COMPLETE, buildRequest, buildStats)) {
                return;  // nothing more can be done!
            }

            // 5) Send BUILDING
            if (!sendUpdatedBuildStatus(BuildState.BUILDING, buildRequest, buildStats)) {
                return;  // nothing more can be done!
            }

            // 6) build the repo
            final long compileSourceStartTime = System.currentTimeMillis();
            final Path logDir = settings.dataDir.resolve(LOG_DIR_NAME);
            final Path projectLogDir = logDir.resolve(buildRequest.getRepository());

            final Builder builder;
            if (settings.buildCommand != null) {
                builder = new JavaProcessBuilderImpl(settings.buildCommand);
            } else {
                builder = new JavaProcessBuilderImpl();
            }
            final BuildResult buildResult;
            try {
                buildResult = builder.build(buildRequest.getId(), projectRepoDir, projectLogDir, DEFAULT_MAKE_TARGETS);
            } catch (final IOException e) {
                LOGGER.error("Unable to build source code repo: {}. Error: {}", projectRepoDir, e.getMessage(), e);

                // send BUILDING_FAILED
                buildStats.setCompilationTime(System.currentTimeMillis() - compileSourceStartTime);
                sendFailureBuildStatus(BuildState.BUILDING_FAILED, buildRequest, buildStats, e);

                return;  // nothing more can be done!
            }

            // record the total time taken for building the source code
            buildStats.setCompilationTime(System.currentTimeMillis() - compileSourceStartTime);


            // 7) did the builder succeed in building the source code?
            if (!buildResult.ok) {
                // build FAILED

                // get build logs
                final List<BuildDetail> buildDetails = convertLogsToBuildDetails(buildResult.stdOutputLogFile, buildResult.stdErrorLogFile);

                // 7.1) Send BUILDING_FAILED
                sendFailureBuildStatus(BuildState.BUILDING_FAILED, buildRequest, buildStats, buildDetails);

            } else {
                // build OK

                // 7.2) Send BUILDING_COMPLETE
                if (!sendUpdatedBuildStatus(BuildState.BUILDING_COMPLETE, buildRequest, buildStats)) {
                    return;  // nothing more can be done!
                }
            }


            // 8) Send BENCHMARKING
            if (!sendUpdatedBuildStatus(BuildState.BENCHMARKING, buildRequest, buildStats)) {
                return;  // nothing more can be done!
            }

            // 9) run the benchmarks
            final Path dbDir = settings.dataDir.resolve(DB_DIR_NAME);
            final Path walDir = settings.dataDir.resolve(WAL_DIR_NAME);
            final Path projectDbDir = dbDir.resolve(buildRequest.getRepository());
            final Path projectWalDir = walDir.resolve(buildRequest.getRepository());

            final long benchmarkStartTime = System.currentTimeMillis();

            final Benchmarker benchmarker;
            if (settings.benchmarkCommand != null) {
                benchmarker = new JavaProcessBenchmarkerImpl(settings.benchmarkCommand);
            } else {
                benchmarker = new JavaProcessBenchmarkerImpl();
            }

            final BenchmarkResult benchmarkResult;
            try {
                benchmarkResult = benchmarker.benchmark(buildRequest.getId(), projectRepoDir, projectLogDir, projectDbDir, projectWalDir, DEFAULT_BENCHMARK_ENV, DEFAULT_BENCHMARK_ARGS);
            } catch (final IOException e) {
                LOGGER.error("Unable to benchmark source code repo: {}. Error: {}", projectRepoDir, e.getMessage(), e);

                // send BUILDING_FAILED
                buildStats.setBenchmarkTime(System.currentTimeMillis() - benchmarkStartTime);
                sendFailureBuildStatus(BuildState.BENCHMARKING_FAILED, buildRequest, buildStats, e);

                return;  // nothing more can be done!
            } finally {
                if (!settings.keepData) {
                    try {
                        PathUtil.delete(projectWalDir);
                    } catch (final IOException e) {
                        LOGGER.warn("Unable to remove projectWalDir: {}: {}", projectWalDir.toAbsolutePath(), e.getMessage());
                    }
                    try {
                        PathUtil.delete(projectDbDir);
                    } catch (final IOException e) {
                        LOGGER.warn("Unable to remove projectDbDir: {}: {}", projectDbDir.toAbsolutePath(), e.getMessage());
                    }
                }
            }

            // record the total time taken for building the source code
            buildStats.setBenchmarkTime(System.currentTimeMillis() - benchmarkStartTime);

            // get benchmark logs
            final List<BuildDetail> buildDetails;

            // 10) did the builder succeed in building the source code?
            if (!benchmarkResult.ok) {
                // benchmark FAILED

                 buildDetails = convertLogsToBuildDetails(benchmarkResult.stdOutputLogFile, benchmarkResult.stdErrorLogFile);

                // 10.1) Send BENCHMARKING_FAILED
                sendFailureBuildStatus(BuildState.BENCHMARKING_FAILED, buildRequest, buildStats, buildDetails);

            } else {
                // benchmark OK

                buildDetails = convertLogsToBuildDetails(benchmarkResult.stdOutputLogFile, null);

                // 10.2) Send BENCHMARKING_COMPLETE
                if (!sendUpdatedBuildStatus(BuildState.BENCHMARKING_COMPLETE, buildRequest, buildStats, buildDetails)) {
                    return;  // nothing more can be done!
                }
            }

            // DONE!
        }
    }

    private boolean sendUpdatedBuildStatus(final BuildState newBuildState, final BuildRequest buildRequest, final BuildStats buildStats) {
        return sendUpdatedBuildStatus(newBuildState, buildRequest, buildStats, null);
    }

    private boolean sendUpdatedBuildStatus(final BuildState newBuildState, final BuildRequest buildRequest, final BuildStats buildStats, @Nullable final List<BuildDetail> buildDetails) {
        if (!BuildState.isStateUpdateSuccessState(newBuildState) && !BuildState.isStateFinalSuccessState(newBuildState)) {
            throw new IllegalStateException("Cannot send update build status message for non-update state: " + newBuildState);
        }

        final BuildResponse benchmarkingBuildResponse = new BuildResponse(newBuildState, buildRequest, buildStats, buildDetails);
        try {
            sendBuildResponseOutput(benchmarkingBuildResponse);
            return true;
        } catch (final IOException | JMSException e) {
            LOGGER.error("Unable to send updated build state {}. Error: {}",  benchmarkingBuildResponse.getBuildState(), e.getMessage(), e);
            return false;
        }
    }

    private void sendFailureBuildStatus(final BuildState failureBuildState, final BuildRequest buildRequest, final BuildStats buildStats, @Nullable final Exception e) {
        final List<BuildDetail> buildDetails = e == null ? null : Arrays.asList(BuildDetail.forException(e));
        sendFailureBuildStatus(failureBuildState, buildRequest, buildStats, buildDetails);
    }

    private void sendFailureBuildStatus(final BuildState failureBuildState, final BuildRequest buildRequest, final BuildStats buildStats, @Nullable final List<BuildDetail> buildDetails) {
        if (!BuildState.isStateFailureState(failureBuildState)) {
            throw new IllegalStateException("Cannot send failure build status message for non-failure state: " + failureBuildState);
        }

        final BuildResponse failedResponse = new BuildResponse(failureBuildState, buildRequest, buildStats, buildDetails);
        try {
            sendBuildResponseOutput(failedResponse);
        } catch (final IOException | JMSException ee) {
            LOGGER.error("Unable to send build failure message for state {}. Error: {}", failedResponse.getBuildState(), ee.getMessage(), ee);
        }
    }

    private void sendBuildResponseOutput(final BuildResponse buildResponse) throws IOException, JMSException {
        // send the message
        final Queue buildResponseQueue = getQueue(settings.buildResponseQueueName);
        sendMessage(buildResponse, buildResponseQueue);
    }

    private @Nullable List<BuildDetail> convertLogsToBuildDetails(@Nullable final Path stdOutputLogFile, @Nullable final Path stdErrorLogFile) {
        List<BuildDetail> buildDetails = null;

        if (stdOutputLogFile != null) {
            final byte[] stdOutLog = readFile(stdOutputLogFile);
            if (stdOutLog != null) {
                buildDetails = new ArrayList<>();
                buildDetails.add(BuildDetail.forStdOut(stdOutLog));
            }
        }

        if (stdErrorLogFile != null) {
            final byte[] stdErrLog = readFile(stdErrorLogFile);
            if (stdErrLog != null) {
                if (buildDetails == null) {
                    buildDetails = new ArrayList<>();
                }
                buildDetails.add(BuildDetail.forStdErr(stdErrLog));
            }
        }

        return buildDetails;
    }

    private static byte[] readFile(@Nullable final Path path) {
        if (path == null) {
            return null;
        }

        try {
            if (Files.exists(path) && Files.size(path) > 0) {
                return Files.readAllBytes(path);
            }
            return null;
        } catch (final IOException e) {
            LOGGER.error("Unable to read file: {}. {}", path.toAbsolutePath().toString(), e.getMessage(), e);
            return null;
        }
    }

    static class Settings {
        final String buildRequestQueueName;
        final String buildResponseQueueName;
        final Path dataDir;
        @Nullable final String buildCommand;
        @Nullable final String benchmarkCommand;
        final boolean keepLogs;
        final boolean keepData;

        public Settings(final String buildRequestQueueName, final String buildResponseQueueName, final Path dataDir, @Nullable final String buildCommand, @Nullable final String benchmarkCommand, final boolean keepLogs, final boolean keepData) {
            this.buildRequestQueueName = buildRequestQueueName;
            this.buildResponseQueueName = buildResponseQueueName;
            this.dataDir = dataDir;
            this.benchmarkCommand = benchmarkCommand;
            this.buildCommand = buildCommand;
            this.keepLogs = keepLogs;
            this.keepData = keepData;
        }
    }
}
