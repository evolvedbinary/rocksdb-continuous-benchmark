package com.evolvedbinary.rocksdb.cb.runner;

import com.evolvedbinary.rocksdb.cb.process.ProcessHelper;
import com.evolvedbinary.rocksdb.cb.process.ProcessHelper.ProcessInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.evolvedbinary.rocksdb.cb.process.ProcessHelper.NORMAL_EXIT_CODE;

class Builder {

    private static final Map<String, String> DEFAULT_ENVIRONMENT_VARIABLES = new HashMap<>();
    static {
        DEFAULT_ENVIRONMENT_VARIABLES.put("DEBUG_LEVEL", "0");
    }
    private static final String DEFAULT_COMMAND = "make";
    private static List<String> DEFAULT_ARGUMENTS = new ArrayList<>();
    static {
        final int nproc = Runtime.getRuntime().availableProcessors();
        DEFAULT_ARGUMENTS.add("-j" + nproc);
    }

    private final Map<String, String> environmentVariables;
    private final String command;
    private final List<String> arguments;

    public Builder() {
        this.environmentVariables = DEFAULT_ENVIRONMENT_VARIABLES;
        this.command = DEFAULT_COMMAND;
        this.arguments = DEFAULT_ARGUMENTS;
    }

    Builder(final Map<String, String> environmentVariables, final String command, final List<String> arguments) {
        this.environmentVariables = environmentVariables;
        this.command = command;
        this.arguments = arguments;
    }

    /**
     * Build the Source Code.
     *
     * @param projectRepoDir the location of the source code
     * @param projectLogDir the location to write build log files to
     * @param targets the make build targets
     *
     * @return the result of the build
     *
     * @throws IllegalArgumentException if the projectRepoDir does not exist,
     *     if the projectLog dir does not exist or is not writable, or if less than
     *     one target is specified.
     *
     * @throws IOException if the build cannot be started
     */
    public BuildResult build(final Path projectRepoDir, final Path projectLogDir, final List<String> targets) throws IOException {

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

        if (Objects.requireNonNull(targets).isEmpty()) {
            throw new IllegalArgumentException("At least one build target must be specified");
        }

        final List<String> allArguments = new ArrayList<>(arguments);
        allArguments.addAll(targets);

        final long buildStart = System.currentTimeMillis();

        final ProcessInfo processInfo = ProcessHelper.start(projectRepoDir, environmentVariables, command, allArguments, projectLogDir);
        final int exitCode = ProcessHelper.waitFor(processInfo);

        final long buildEnd = System.currentTimeMillis();

        if (exitCode == NORMAL_EXIT_CODE) {
            return BuildResult.ok(buildEnd - buildStart, processInfo.stdOutputLogFile, processInfo.stdErrorLogFile);
        } else {
            return BuildResult.failure(exitCode, buildEnd - buildStart, processInfo.stdOutputLogFile, processInfo.stdErrorLogFile);
        }
    }

    static class BuildResult {
        final boolean ok;
        final int exitCode;
        final long duration;
        final Path stdOutputLogFile;
        final Path stdErrorLogFile;

        static BuildResult ok(final long duration, final Path stdOutputLogFile, final Path stdErrorLogFile) {
            return new BuildResult(true, NORMAL_EXIT_CODE, duration, stdOutputLogFile, stdErrorLogFile);
        }

        static BuildResult failure(final int exitCode, final long duration, final Path stdOutputLogFile, final Path stdErrorLogFile) {
            return new BuildResult(false, exitCode, duration, stdOutputLogFile, stdErrorLogFile);
        }

        private BuildResult(final boolean ok, final int exitCode, final long duration, final Path stdOutputLogFile, final Path stdErrorLogFile) {
            this.ok = ok;
            this.exitCode = exitCode;
            this.duration = duration;
            this.stdOutputLogFile = stdOutputLogFile;
            this.stdErrorLogFile = stdErrorLogFile;
        }
    }
}
