package com.evolvedbinary.rocksdb.cb.runner.builder;

import com.evolvedbinary.rocksdb.cb.process.ProcessHelper;
import com.evolvedbinary.rocksdb.cb.process.ProcessInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.evolvedbinary.rocksdb.cb.common.ExitCodes.NORMAL_EXIT_CODE;
import static com.evolvedbinary.rocksdb.cb.common.MapUtil.Entry;
import static com.evolvedbinary.rocksdb.cb.common.MapUtil.Map;


public class JavaProcessBuilderImpl implements Builder {

    private static final Map<String, String> DEFAULT_ENVIRONMENT_VARIABLES = Map(
      Entry("DEBUG_LEVEL", "0")
    );
    private static final String DEFAULT_COMMAND = "make";
    private static List<String> DEFAULT_ARGUMENTS = new ArrayList<>();
    static {
        final int nproc = Runtime.getRuntime().availableProcessors();
        DEFAULT_ARGUMENTS.add("-j" + nproc);
    }

    private final Map<String, String> environmentVariables;
    private final String command;
    private final List<String> arguments;

    public JavaProcessBuilderImpl() {
        this(DEFAULT_ENVIRONMENT_VARIABLES, DEFAULT_COMMAND, DEFAULT_ARGUMENTS);
    }

    public JavaProcessBuilderImpl(final String command) {
        this(DEFAULT_ENVIRONMENT_VARIABLES, command, DEFAULT_ARGUMENTS);
    }

    JavaProcessBuilderImpl(final Map<String, String> environmentVariables, final String command, final List<String> arguments) {
        this.environmentVariables = environmentVariables;
        this.command = command;
        this.arguments = arguments;
    }

    @Override
    public BuildResult build(final UUID buildId, final Path projectRepoDir, final Path projectLogDir, final List<String> targets) throws IOException {

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

        final String logFilePrefix = buildId.toString() + ".build";
        final ProcessInfo processInfo = ProcessHelper.start(projectRepoDir, environmentVariables, command, allArguments, projectLogDir, logFilePrefix);
        final int exitCode = ProcessHelper.waitFor(processInfo);

        final long buildEnd = System.currentTimeMillis();
        final long buildDuration = buildEnd - buildStart;

        if (exitCode == NORMAL_EXIT_CODE) {
            return BuildResult.ok(buildDuration, processInfo.stdOutputLogFile, processInfo.stdErrorLogFile);
        } else {
            return BuildResult.failure(exitCode, buildDuration, processInfo.stdOutputLogFile, processInfo.stdErrorLogFile);
        }
    }
}
