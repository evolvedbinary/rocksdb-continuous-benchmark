package com.evolvedbinary.rocksdb.cb.publisher;

import com.evolvedbinary.rocksdb.cb.common.ExitCodes;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static se.softhouse.jargo.Arguments.*;

public class Main {

    private static final Argument<?> HELP_ARG = helpArgument("-h", "--help");
    private static final Argument<String> OUTPUT_QUEUE_NAME_ARG = stringArgument("-o", "--output-queue-name")
            .defaultValue("OutputQueue")
            .description("The name of the JMS Queue for Build Output messages")
            .build();
    private static final Argument<File> DATA_DIR_ARG = fileArgument("-d", "--data-dir")
            .required()
            .description("The path to the data directory where the Publisher should keep its data")
            .build();
    private static final Argument<String> REPO = stringArgument("-r", "--repository")
            .required()
            .description("The GitHub repository to publish the results to, e.g. facebook/rocksdb")
            .build();
    private static final Argument<String> REPO_BRANCH = stringArgument("-b", "--repository-branch")
            .required()
            .description("The GitHub repository branch to publish the results to, e.g. gh-pages")
            .build();
    private static final Argument<String> REPO_USERNAME = stringArgument("-u", "--username")
            .description("The username for the GitHub repository to publish the results to. If no username is provided, then .netrc will be consulted")
            .build();
    private static final Argument<String> REPO_PASSWORD = stringArgument("-p", "--password")
            .description("The password for the GitHub repository to publish the results to. Can be a Personal Access Token if 2FA is in use")
            .build();

    public static void main(final String args[]) {
        final CommandLineParser parser = CommandLineParser.withArguments(
                HELP_ARG,
                OUTPUT_QUEUE_NAME_ARG,
                DATA_DIR_ARG,
                REPO,
                REPO_BRANCH,
                REPO_USERNAME,
                REPO_PASSWORD);

        try {
            final ParsedArguments parsedArguments = parser.parse(args);

            final String outputQueueName = parsedArguments.get(OUTPUT_QUEUE_NAME_ARG);
            final Path dataDir = parsedArguments.get(DATA_DIR_ARG).toPath();
            try {
                // make sure the data dir exists
                Files.createDirectories(dataDir);
            } catch (final IOException e) {
                System.out.println("Unable to create data dir: " + e.getMessage());
                System.exit(ExitCodes.INVALID_PATH);
            }
            final String repo = parsedArguments.get(REPO);
            final String repoBranch = parsedArguments.get(REPO_BRANCH);
            @Nullable final String repoUsername = parsedArguments.get(REPO_USERNAME);
            @Nullable final String repoPassword = parsedArguments.get(REPO_PASSWORD);

            final Publisher.Settings publisherSettings = new Publisher.Settings(outputQueueName, dataDir, repo, repoBranch, repoUsername, repoPassword);
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
