package com.evolvedbinary.rocksdb.cb.runner.benchmarker;

import com.evolvedbinary.rocksdb.cb.process.ProcessHelper;
import com.evolvedbinary.rocksdb.cb.process.ProcessInfo;
import com.evolvedbinary.rocksdb.cb.runner.builder.BuildResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.evolvedbinary.rocksdb.cb.process.ProcessHelper.NORMAL_EXIT_CODE;

public class JavaProcessBenchmarkerImpl implements Benchmarker {

    private static final String DEFAULT_COMMAND = "db_bench";

    private final String command;
    private final List<String> arguments;

    public JavaProcessBenchmarkerImpl() {
        this.command = DEFAULT_COMMAND;
        this.arguments = Collections.emptyList();
    }

    JavaProcessBenchmarkerImpl(final String command, final List<String> arguments) {
        this.command = command;
        this.arguments = arguments;
    }

    @Override
    public BenchmarkResult benchmark(final UUID benchmarkId, final Path projectRepoDir, final Path projectLogDir, final List<String> benchmarkArgs) throws IOException {
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

        //TODO(AR) do we need such a check? maybe we always need at least one benchmarkArg?
//        if (Objects.requireNonNull(targets).isEmpty()) {
//            throw new IllegalArgumentException("At least one build target must be specified");
//        }

        final List<String> allArguments = new ArrayList<>(arguments);
        allArguments.addAll(benchmarkArgs);

        final long benchmarkStart = System.currentTimeMillis();

        final String logFilePrefix = benchmarkId.toString() + ".benchmark";
        final ProcessInfo processInfo = ProcessHelper.start(projectRepoDir, null, command, allArguments, projectLogDir, logFilePrefix);
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
