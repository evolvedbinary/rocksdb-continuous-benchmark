package com.evolvedbinary.rocksdb.cb.process;

import com.evolvedbinary.rocksdb.cb.common.ListUtil;
import com.evolvedbinary.rocksdb.cb.common.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.evolvedbinary.rocksdb.cb.common.ExitCodes.INTERRUPTED_EXIT_CODE;
import static com.evolvedbinary.rocksdb.cb.common.ExitCodes.NORMAL_EXIT_CODE;

public class ProcessHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessHelper.class);

    private static final DateTimeFormatter BASIC_ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static ProcessInfo start(final Path workingDirectory, @Nullable final Map<String, String> environmentVariables,
            final String command, final List<String> arguments, final Path logDir, final String logFilePrefix) throws IOException {

        final LocalDateTime localDateTime = LocalDateTime.now();
        final String dateTimeStamp = localDateTime.format(BASIC_ISO_DATE_TIME);

        final Path stdOutputLogFile = logDir.resolve(logFilePrefix + ".stdout." + dateTimeStamp + ".log");
        final Path stdErrorLogFile = logDir.resolve(logFilePrefix + ".stderr." + dateTimeStamp + ".log");

        final ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(workingDirectory.toFile());

        if (environmentVariables != null) {
            environmentVariables.forEach((name, value) -> processBuilder.environment().put(name, value));
        }

        final List<String> commandAndArgs = new ArrayList<>();
        commandAndArgs.add(command);
        commandAndArgs.addAll(arguments);
        processBuilder.command(commandAndArgs);

        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(stdOutputLogFile.toFile()));
        processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(stdErrorLogFile.toFile()));

        LOGGER.info("Executing: {} with environment [{}] and arguments [{}]", command, MapUtil.asString(environmentVariables), ListUtil.asString(arguments));

        final Process process = processBuilder.start();
        return new ProcessInfo(stdOutputLogFile, stdErrorLogFile, process);
    }

    public static int waitFor(final ProcessInfo processInfo) {
        int exitCode = NORMAL_EXIT_CODE;
        try {
            exitCode = processInfo.process.waitFor();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();  // reset the interrupted thread
            exitCode = INTERRUPTED_EXIT_CODE;
        }

        return exitCode;
    }
}
