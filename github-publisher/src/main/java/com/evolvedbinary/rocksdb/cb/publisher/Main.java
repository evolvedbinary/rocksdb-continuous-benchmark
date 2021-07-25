package com.evolvedbinary.rocksdb.cb.publisher;

import com.evolvedbinary.rocksdb.cb.common.ExitCodes;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

import static se.softhouse.jargo.Arguments.helpArgument;
import static se.softhouse.jargo.Arguments.stringArgument;

public class Main {

    private static final Argument<?> HELP_ARG = helpArgument("-h", "--help");
    private static final Argument<String> OUTPUT_QUEUE_NAME_ARG = stringArgument("-o", "--output-queue-name")
            .defaultValue("OutputQueue")
            .description("The name of the JMS Queue for Build Output messages")
            .build();
    private static final Argument<String> REPO = stringArgument("-r", "--repository")
            .required()
            .description("The GitHub repository to publish the results to")
            .build();
    private static final Argument<String> REPO_BRANCH = stringArgument("-b", "--repository-branch")
            .required()
            .description("The GitHub repository branch to publish the results to")
            .build();

    public static void main(final String args[]) {
        final CommandLineParser parser = CommandLineParser.withArguments(
                HELP_ARG,
                OUTPUT_QUEUE_NAME_ARG,
                REPO,
                REPO_BRANCH);

        try {
            final ParsedArguments parsedArguments = parser.parse(args);

            final String outputQueueName = parsedArguments.get(OUTPUT_QUEUE_NAME_ARG);
            final String repo = parsedArguments.get(REPO);
            final String repoBranch = parsedArguments.get(REPO_BRANCH);

            final Publisher.Settings publisherSettings = new Publisher.Settings(outputQueueName, repo, repoBranch);
            final Publisher publisher = new Publisher(publisherSettings);
            publisher.runSync();

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
