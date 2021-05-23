package com.evolvedbinary.rocksdb.cb.runner;

import com.evolvedbinary.rocksdb.cb.common.ExitCodes;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

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

    public static void main(final String args[]) throws InterruptedException {
        final CommandLineParser parser = CommandLineParser.withArguments(
                HELP_ARG,
                BUILD_REQUEST_QUEUE_NAME_ARG,
                BUILD_RESPONSE_QUEUE_NAME_ARG);

        try {
            final ParsedArguments parsedArguments = parser.parse(args);

            final String buildRequestQueueName = parsedArguments.get(BUILD_REQUEST_QUEUE_NAME_ARG);
            final String buildResponseQueueName = parsedArguments.get(BUILD_RESPONSE_QUEUE_NAME_ARG);

            final Runner.Settings runnerSettings = new Runner.Settings(buildRequestQueueName, buildResponseQueueName);
            final Runner runner = new Runner(runnerSettings);
            runner.runSync();

        } catch (final ArgumentException e) {
            System.out.println(e.getMessageAndUsage());
            System.exit(ExitCodes.INVALID_ARGUMENT);
        }
    }
}
