package com.evolvedbinary.rocksdb.cb.publisher;

import com.evolvedbinary.rocksdb.cb.dataobject.BuildResponse;
import com.evolvedbinary.rocksdb.cb.dataobject.BuildStats;
import com.evolvedbinary.rocksdb.cb.jms.AbstractJMSService;
import com.evolvedbinary.rocksdb.cb.jms.JMSServiceState;
import com.evolvedbinary.rocksdb.cb.scm.GitHelper;
import com.evolvedbinary.rocksdb.cb.scm.GitHelperException;
import com.evolvedbinary.rocksdb.cb.scm.JGitGitHelperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Publisher extends AbstractJMSService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Publisher.class);
    private static final AtomicReference<JMSServiceState> STATE = new AtomicReference<>(JMSServiceState.IDLE);

    private static final String REPO_DIR_NAME = "repo";

    private final Settings settings;
    private final OutputQueueMessageListener outputQueueMessageListener = new OutputQueueMessageListener();

    public Publisher(final Settings settings) {
        this.settings = settings;
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
        return "publisher";
    }

    @Override
    protected List<String> getQueueNames() {
        return List.of(
                settings.outputQueueName
        );
    }

    @Nullable
    @Override
    protected MessageListener getListener(final String queueName) {
        if (settings.outputQueueName.equals(queueName)) {
            return outputQueueMessageListener;
        }

        return null;
    }

    private class OutputQueueMessageListener implements MessageListener {
        @Override
        public void onMessage(final Message message) {
            if (!(message instanceof TextMessage)) {
                // acknowledge invalid message so that it is removed from the queue
                if (acknowledgeMessage(message)) {
                    LOGGER.error("Discarded message with unexpected type {} from Queue: {}.", message.getClass().getName(), settings.outputQueueName);
                }

                // can't process non-text message, so DONE
                return;
            }

            final TextMessage textMessage = (TextMessage) message;
            final String content;
            try {
                content = textMessage.getText();
            } catch (final JMSException e) {
                LOGGER.error("Could not get content of TextMessage from Queue: {}. Error: {}", settings.outputQueueName, e.getMessage(), e);

                // can't access message content, so DONE
                return;
            }

            // TODO(AR) do we want this as a BuildResponse, or would something more "publishing" specific be better?

            // attempt to parse as BuildResponse
            final BuildResponse buildResponse;
            try {
                buildResponse = new BuildResponse().deserialize(content);
            } catch (final IOException e) {
                // unable to deserialize, acknowledge invalid message so that it is removed from the queue
                if (acknowledgeMessage(message)) {
                    LOGGER.error("Discarded message with unexpected format from Queue: {}. Error: {}. Content: '{}'", settings.outputQueueName, e.getMessage(), content);
                }
                return;
            }

            final Path repoDir = settings.dataDir.resolve(REPO_DIR_NAME);
            final Path projectRepoDir = repoDir.resolve(settings.repo);

            // 1) Checkout/Fetch gh-pages branch from repo with JGit to update the source
            GitHelper gitHelper = null;
            try {
                if (!Files.exists(projectRepoDir)) {
                    // clone the remote repo
                    final String repoUri = "https://github.com/" + settings.repo;
                    gitHelper = JGitGitHelperImpl.clone(repoUri, projectRepoDir, settings.repoBranch);

                } else {
                    // fetch the latest from the remote repo
                    gitHelper = JGitGitHelperImpl.open(projectRepoDir).fetch();
                }

                // hard reset our copy to the HEAD of the remote
                gitHelper = gitHelper.reset(true, "origin", settings.repoBranch);


                // 2) update HTML assets (if needed)
                try {
                    updateWebAssets(gitHelper, projectRepoDir);
                } catch (final IOException e) {
                    LOGGER.error("Unable to Update Web Assets. Error: {}", e.getMessage(), e);
                    return;  // nothing more can be done!
                }


                // 3) update the CSV file with the new stats
                final Path csvFile = projectRepoDir.resolve("rocksdb-benchmarks.csv");
                if (!Files.exists(csvFile)) {
                    // if there is no CSV file - copy in the empty CSV file (just contains the column headings)
                    try (final InputStream is = getClass().getResourceAsStream("/rocksdb-benchmarks.empty.csv")) {
                        if (is == null) {
                            throw new IOException("File 'rocksdb-benchmarks.empty.csv' is missing from the application");
                        }
                        Files.copy(is, csvFile);
                    } catch (final IOException e) {
                        LOGGER.error("Unable to store initial empty CSV files. Error: {}", e.getMessage(), e);
                        return;  // nothing more can be done!
                    }

                    // stage the new CSV file
                    gitHelper = gitHelper.add(csvFile.getFileName().toString());
                }

                // append to the CSV file
                try (final BufferedWriter bufferedWriter = Files.newBufferedWriter(csvFile, UTF_8, StandardOpenOption.APPEND);
                     final PrintWriter printWriter = new PrintWriter(bufferedWriter)) {

                    final String commit = buildResponse.getBuildRequest().getCommit();
                    final String date = buildResponse.getBuildRequest().getTimeStamp().format(DateTimeFormatter.ISO_DATE_TIME); // TODO(AR) is this correct
                    final BuildStats buildStats = buildResponse.getBuildStats();
                    if (buildStats == null) {
                        LOGGER.error("BuildStats are missing from message with id: {} from Queue: {}", buildResponse.getId(), settings.outputQueueName);
                        if (acknowledgeMessage(message)) {
                            LOGGER.error("Discarded message with missing BuildStats with id: {} from Queue: {}.", buildResponse.getId(), settings.outputQueueName);
                        }
                        return;  // nothing more can be done!
                    }

                    // write source update time
                    String task = "Update Source";
                    long time = buildResponse.getBuildStats().getUpdateSourceTime();
                    writeCsvLine(printWriter, task, commit, date, time);

                    // write compilation time
                    task = "Compile Source";
                    time = buildResponse.getBuildStats().getCompilationTime();
                    writeCsvLine(printWriter, task, commit, date, time);

                    // write benchmark time
                    task = "Benchmark 1"; // TODO(AR) add different benchmarks
                    time = buildResponse.getBuildStats().getBenchmarkTime();
                    writeCsvLine(printWriter, task, commit, date, time);
                } catch (final IOException e) {
                    LOGGER.error("Unable to append data to CSV file. Error: {}", e.getMessage(), e);
                    return;  // nothing more can be done!
                }

                // stage the changed file
                gitHelper = gitHelper.add(csvFile.getFileName().toString());

                // 4) commit and push the CSV file
                gitHelper = gitHelper.commit("Added Benchmark Stats");
                gitHelper = gitHelper.push(settings.repoUsername, settings.repoPassword);

            } catch (final GitHelperException e) {
                LOGGER.error("Unable to open/update Git repo: {}. Error: {}", settings.repo, e.getMessage(), e);
                return;  // nothing more can be done!

            } finally {
                if (gitHelper != null) {
                    gitHelper.close();
                }
            }

            // Yay! We are done :-) So we can acknowledge the message...
            if (!acknowledgeMessage(message)) {
                LOGGER.error("Unable to acknowledge message from Queue: {}. Content: '{}'. Skipping...", settings.outputQueueName, content);
                return;
            }

            // DONE!
        }
    }

    private void writeCsvLine(final PrintWriter printWriter, final String task, final String commit, final String date, final long time) {
        final String csvLine = String.format("%s,%s,%s,%d\r\n", task, commit, date, time);  // RFC 4180 for CSV specifies that EOL should always be CRLF
        printWriter.print(csvLine);
    }

    private void updateWebAssets(final GitHelper gitHelper, final Path projectRepoDir) throws IOException, GitHelperException {
        boolean updated = false;

        final Path htmlPage = projectRepoDir.resolve("rocksdb-benchmarks.html");
        if (!isLatestVersion(htmlPage)) {
            try (final InputStream is = getClass().getResourceAsStream("/" + htmlPage.getFileName().toString())) {
                if (is == null) {
                    throw new IOException("File 'rocksdb-benchmarks.html' is missing from the application");
                }
                Files.copy(is, htmlPage, StandardCopyOption.REPLACE_EXISTING);
            }
            // stage the changed file
            gitHelper.add(htmlPage.getFileName().toString());
            updated = true;
        }

        final Path vgJsonFile = projectRepoDir.resolve("rocksdb-benchmarks.vg.json");
        if (!isLatestVersion(vgJsonFile)) {
            try (final InputStream is = getClass().getResourceAsStream("/" + vgJsonFile.getFileName().toString())) {
                if (is == null) {
                    throw new IOException("File 'rocksdb-benchmarks.vg.json' is missing from the application");
                }
                Files.copy(is, vgJsonFile, StandardCopyOption.REPLACE_EXISTING);
            }
            // stage the changed file
            gitHelper.add(vgJsonFile.getFileName().toString());
            updated = true;
        }

        // commit the updates
        if (updated) {
            gitHelper.commit("Updated Web Assets");
        }
    }

    private boolean isLatestVersion(final Path existingFile) throws IOException {
        if (!Files.exists(existingFile)) {
            return false;
        }

        final MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA256");
        } catch (final NoSuchAlgorithmException e) {
            throw new IOException(e);
        }

        // calculate the checksum of the existing file
        final byte[] existingFileChecksum;
        try (final InputStream is = new BufferedInputStream(Files.newInputStream(existingFile))) {
            existingFileChecksum = calculateChecksum(messageDigest, is);
        }

        // reset the message digester
        messageDigest.reset();

        // calculate the checks of the latest file
        final byte[] latestFileChecksum;
        final String filename = existingFile.getFileName().toString();
        try (final InputStream is = getClass().getResourceAsStream("/" + filename)) {
            if (is == null) {
                throw new IOException("File '" + filename + "' is missing from the application");
            }
            latestFileChecksum = calculateChecksum(messageDigest, is);
        }

        return Arrays.equals(existingFileChecksum, latestFileChecksum);
    }

    private byte[] calculateChecksum(final MessageDigest messageDigest, final InputStream is) throws IOException {
        int read = -1;
        final byte[] buf = new byte[4096];
        while ((read = is.read(buf)) != -1) {
            messageDigest.update(buf, 0, read);
        }
        return messageDigest.digest();
    }

    static class Settings {
        final String outputQueueName;
        final Path dataDir;
        final String repo;
        final String repoBranch;
        @Nullable final String repoUsername;
        @Nullable final String repoPassword;

        public Settings(final String outputQueueName, final Path dataDir, final String repo, final String repoBranch,
                        @Nullable final String repoUsername, @Nullable final String repoPassword) {
            this.outputQueueName = outputQueueName;
            this.dataDir = dataDir;
            this.repo = repo;
            this.repoBranch = repoBranch;
            this.repoUsername = repoUsername;
            this.repoPassword = repoPassword;
        }
    }
}
