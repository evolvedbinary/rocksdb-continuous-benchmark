package com.evolvedbinary.rocksdb.cb.runner.benchmarker;

import com.evolvedbinary.rocksdb.cb.process.ProcessHelper;
import com.evolvedbinary.rocksdb.cb.process.ProcessInfo;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.evolvedbinary.rocksdb.cb.common.ExitCodes.NORMAL_EXIT_CODE;
import static com.evolvedbinary.rocksdb.cb.common.MapUtil.Map;

public class JavaProcessBenchmarkerImpl implements Benchmarker {

    private static final String DB_DIR_ENV_VAR_NAME = "DB_DIR";
    private static final String WAL_DIR_ENV_VAR_NAME = "WAL_DIR";

    private static final Map<String, String> DEFAULT_ENVIRONMENT_VARIABLES = Map();
    private static final String DEFAULT_COMMAND = "tools" + File.separator + "benchmark.sh";
    private static List<String> DEFAULT_ARGUMENTS = Collections.emptyList();

    private final Map<String, String> environmentVariables;
    private final String command;
    private final List<String> arguments;

    public JavaProcessBenchmarkerImpl() {
        this(DEFAULT_ENVIRONMENT_VARIABLES, DEFAULT_COMMAND, Collections.emptyList());
    }

    public JavaProcessBenchmarkerImpl(final String command) {
        this(DEFAULT_ENVIRONMENT_VARIABLES, command, Collections.emptyList());
    }

    JavaProcessBenchmarkerImpl(final Map<String, String> environmentVariables, final String command, final List<String> arguments) {
        this.environmentVariables = environmentVariables;
        this.command = command;
        this.arguments = arguments;
    }

    @Override
    public BenchmarkResult benchmark(final UUID benchmarkId, final Path projectRepoDir, final Path projectLogDir, final Path projectDbDir, @Nullable final Path projectWalDir, final Map<String, String> benchmarkEnvironmentVariables, final List<String> benchmarkArgs) throws IOException {
        if (!Files.exists(Objects.requireNonNull(projectRepoDir))) {
            throw new IllegalArgumentException("The projectRepoDir does not exist: " + projectRepoDir.toAbsolutePath());
        }

        if (!Files.exists(Objects.requireNonNull(projectLogDir))) {
            try {
                Files.createDirectories(projectLogDir);
            } catch (final IOException ioe) {
                throw new IllegalArgumentException("The projectLogDir could not be created: " + projectLogDir.toAbsolutePath(), ioe);
            }
        }

        final Map<String, String> allEnvironmentVariables = new HashMap<>(environmentVariables);
        if (benchmarkEnvironmentVariables != null) {
            allEnvironmentVariables.putAll(benchmarkEnvironmentVariables);
        }

        if (!Files.exists(Objects.requireNonNull(projectDbDir))) {
            try {
                Files.createDirectories(projectDbDir);
            } catch (final IOException ioe) {
                throw new IllegalArgumentException("The projectDbDir could not be created: " + projectDbDir.toAbsolutePath(), ioe);
            }
        }
        allEnvironmentVariables.put(DB_DIR_ENV_VAR_NAME, projectDbDir.toAbsolutePath().toString());

        if (projectWalDir != null) {
            if (!Files.exists(Objects.requireNonNull(projectWalDir))) {
                try {
                    Files.createDirectories(projectWalDir);
                } catch (final IOException ioe) {
                    throw new IllegalArgumentException("The projectWalDir could not be created: " + projectWalDir.toAbsolutePath(), ioe);
                }
            }
            allEnvironmentVariables.put(WAL_DIR_ENV_VAR_NAME, projectWalDir.toAbsolutePath().toString());
        }

        final List<String> allArguments = new ArrayList<>(arguments);
        allArguments.addAll(benchmarkArgs);

        final long benchmarkStart = System.currentTimeMillis();

        final String logFilePrefix = benchmarkId.toString() + ".benchmark";
        final ProcessInfo processInfo = ProcessHelper.start(projectRepoDir, allEnvironmentVariables, command, allArguments, projectLogDir, logFilePrefix);
        final int exitCode = ProcessHelper.waitFor(processInfo);

        final long benchmarkEnd = System.currentTimeMillis();
        final long benchmarkDuration = benchmarkEnd - benchmarkStart;

        if (exitCode == NORMAL_EXIT_CODE) {
            return BenchmarkResult.ok(benchmarkDuration, processInfo.stdOutputLogFile, processInfo.stdErrorLogFile);
        } else {
            return BenchmarkResult.failure(exitCode, benchmarkDuration, processInfo.stdOutputLogFile, processInfo.stdErrorLogFile);
        }
    }
}
