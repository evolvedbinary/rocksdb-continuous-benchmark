package com.evolvedbinary.rocksdb.cb.orchestrator;

import com.evolvedbinary.rocksdb.cb.Constants;
import com.evolvedbinary.rocksdb.cb.common.ExitCodes;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static se.softhouse.jargo.Arguments.*;

public class Main {

    private static final Argument<?> HELP_ARG = helpArgument("-h", "--help");
    private static final Argument<String> ARTEMIS_BROKER_HOST_ARG = stringArgument("--artemis-broker-host")
            .defaultValue(Constants.DEFAULT_ARTEMIS_HOST)
            .description("The hostname or IP address of the Artemis Broker")
            .build();
    private static final Argument<Integer> ARTEMIS_BROKER_PORT_ARG = integerArgument("--artemis-broker-port")
            .defaultValue(Constants.DEFAULT_ARTEMIS_PORT)
            .description("The port of the Artemis Broker")
            .build();
    private static final Argument<String> WEBHOOK_QUEUE_NAME_ARG = stringArgument("-w", "--webhook-queue-name")
            .defaultValue("WebHookQueue")
            .description("The name of the JMS Queue for GitHub WebHook messages")
            .build();
    private static final Argument<String> BUILD_REQUEST_QUEUE_NAME_ARG = stringArgument("-b", "--build-request-queue-name")
            .defaultValue("BuildRequestQueue")
            .description("The name of the JMS Queue for Build request messages")
            .build();
    private static final Argument<String> BUILD_RESPONSE_QUEUE_NAME_ARG = stringArgument("-B", "--build-response-queue-name")
            .defaultValue("BuildResponseQueue")
            .description("The name of the JMS Queue for Build response messages")
            .build();
    private static final Argument<String> PUBLISH_REQUEST_QUEUE_NAME_ARG = stringArgument("-p", "--publish-request-queue-name")
            .defaultValue("PublishRequestQueue")
            .description("The name of the JMS Queue for Publish request messages")
            .build();
    private static final Argument<String> PUBLISH_RESPONSE_QUEUE_NAME_ARG = stringArgument("-P", "--publish-response-queue-name")
            .defaultValue("PublishResponseQueue")
            .description("The name of the JMS Queue for Publish response messages")
            .build();
    private static final Argument<List<String>> REF_PATTERN_ARG = stringArgument("-r", "--ref-pattern")
            .repeated()
            .description("The Git ref patterns to filter on, by default no filtering will take place and all refs will be built. The patterns are java.util.regex.Pattern.")
            .build();
    private static final Argument<Boolean> ALL_BUILDS_ARG = booleanArgument("-a", "--all-builds")
            .description("Causes every request to be built. By default when a build is in progress, any incoming commits apart from the latest for the same ref are discarded.")
            .build();

    public static void main(final String args[]) {
        final CommandLineParser parser = CommandLineParser.withArguments(
                HELP_ARG,
                ARTEMIS_BROKER_HOST_ARG,
                ARTEMIS_BROKER_PORT_ARG,
                WEBHOOK_QUEUE_NAME_ARG,
                BUILD_REQUEST_QUEUE_NAME_ARG,
                BUILD_RESPONSE_QUEUE_NAME_ARG,
                PUBLISH_REQUEST_QUEUE_NAME_ARG,
                PUBLISH_RESPONSE_QUEUE_NAME_ARG,
                REF_PATTERN_ARG,
                ALL_BUILDS_ARG);

        try {
            final ParsedArguments parsedArguments = parser.parse(args);

            final String artemisBrokerHost = parsedArguments.get(ARTEMIS_BROKER_HOST_ARG);
            final int artemisBrokerPort = parsedArguments.get(ARTEMIS_BROKER_PORT_ARG).intValue();
            final String webHookQueueName = parsedArguments.get(WEBHOOK_QUEUE_NAME_ARG);
            final String buildRequestQueueName = parsedArguments.get(BUILD_REQUEST_QUEUE_NAME_ARG);
            final String buildResponseQueueName = parsedArguments.get(BUILD_RESPONSE_QUEUE_NAME_ARG);
            final String publishRequestQueueName = parsedArguments.get(PUBLISH_REQUEST_QUEUE_NAME_ARG);
            final String publishResponseQueueName = parsedArguments.get(PUBLISH_RESPONSE_QUEUE_NAME_ARG);

            final List<String> strRefPatterns = parsedArguments.get(REF_PATTERN_ARG);
            final List<Pattern> refPatterns;
            if (strRefPatterns == null || strRefPatterns.isEmpty()) {
                refPatterns = Collections.emptyList();
            } else {
                refPatterns = new ArrayList<>();
                for (final String strRefPattern : strRefPatterns) {
                    try {
                        final Pattern refPattern = Pattern.compile(strRefPattern);
                        refPatterns.add(refPattern);
                    } catch (final PatternSyntaxException e) {
                        System.out.println("Invalid --ref-pattern specified: " + strRefPattern);
                        System.out.println(e.getMessage());
                        System.exit(ExitCodes.INVALID_ARGUMENT);
                    }
                }
            }

            final boolean allBuilds = parsedArguments.get(ALL_BUILDS_ARG);

            final Orchestrator.Settings orchestratorSettings = new Orchestrator.Settings(artemisBrokerHost, artemisBrokerPort, webHookQueueName, buildRequestQueueName, buildResponseQueueName, publishRequestQueueName, publishResponseQueueName, refPatterns, allBuilds);
            final Orchestrator orchestrator = new Orchestrator(orchestratorSettings);
            orchestrator.runSync();

        } catch (final ArgumentException e) {
            System.out.println(e.getMessageAndUsage());
            System.exit(ExitCodes.INVALID_ARGUMENT);

        } catch (final InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(ExitCodes.INTERRUPTED_EXIT_CODE);
        }
    }
}
