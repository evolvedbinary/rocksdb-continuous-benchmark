package com.evolvedbinary.rocksdb.cb.runner;

import com.evolvedbinary.rocksdb.cb.common.ExitCodes;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static se.softhouse.jargo.Arguments.*;

public class Main {
    private static final Argument<?> HELP_ARG = helpArgument("-h", "--help");
    private static final Argument<String> BUILD_REQUEST_QUEUE_NAME_ARG = stringArgument("-b", "--build-request-queue-name")
            .defaultValue("BuildRequestQueue")
            .description("The name of the JMS Queue for Build request messages")
            .build();
    private static final Argument<String> BUILD_RESPONSE_QUEUE_NAME_ARG = stringArgument("-B", "--build-response-queue-name")
            .defaultValue("BuildResponseQueue")
            .description("The name of the JMS Queue for Build response messages")
            .build();
    private static final Argument<File> DATA_DIR_NAME_ARG = fileArgument("-d", "--data-dir")
            .required()
            .description("The path to the data directory where the runner should keep its data")
            .build();
    private static final Argument<Boolean> KEEP_LOGS_ARG = optionArgument("--keep-logs")
            .description("Keep logs from builds. Without this flag successful build logs are removed, whilst failed builds logs are sent to the orchestrator and then removed.")
            .build();
    private static final Argument<Boolean> KEEP_DATA_ARG = optionArgument("--keep-data")
            .description("Keep data and wal files from benchmarks. Without this flag data and WAL files are removed.")
            .build();

    public static void main(final String args[]) throws InterruptedException {
        final CommandLineParser parser = CommandLineParser.withArguments(
                HELP_ARG,
                BUILD_REQUEST_QUEUE_NAME_ARG,
                BUILD_RESPONSE_QUEUE_NAME_ARG,
                DATA_DIR_NAME_ARG,
                KEEP_LOGS_ARG,
                KEEP_DATA_ARG);

        try {
            final ParsedArguments parsedArguments = parser.parse(args);

            final String buildRequestQueueName = parsedArguments.get(BUILD_REQUEST_QUEUE_NAME_ARG);
            final String buildResponseQueueName = parsedArguments.get(BUILD_RESPONSE_QUEUE_NAME_ARG);
            final Path dataDir = parsedArguments.get(DATA_DIR_NAME_ARG).toPath();
            try {
                // make sure the data dir exists
                Files.createDirectories(dataDir);
            } catch (final IOException e) {
                System.out.println("Unable to create data dir: " + e.getMessage());
                System.exit(ExitCodes.INVALID_PATH);
            }
            final boolean keepLogs = parsedArguments.get(KEEP_LOGS_ARG);
            final boolean keepData = parsedArguments.get(KEEP_DATA_ARG);

            final Runner.Settings runnerSettings = new Runner.Settings(buildRequestQueueName, buildResponseQueueName, dataDir, keepLogs, keepData);
            final Runner runner = new Runner(runnerSettings);
            runner.runSync();

        } catch (final ArgumentException e) {
            System.out.println(e.getMessageAndUsage());
            System.exit(ExitCodes.INVALID_ARGUMENT);
        }
    }
}
