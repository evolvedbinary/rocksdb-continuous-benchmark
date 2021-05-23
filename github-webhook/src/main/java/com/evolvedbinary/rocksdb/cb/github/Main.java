package com.evolvedbinary.rocksdb.cb.github;

import com.evolvedbinary.rocksdb.cb.common.ExitCodes;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static se.softhouse.jargo.Arguments.*;

public class Main {

    private static final Argument<?> HELP_ARG = helpArgument("-h", "--help");
    private static final Argument<File> KEYSTORE_FILE_ARG = fileArgument("-k", "--keystore")
            .required()
            .description("Path to the keystore file")
            .build();
    private static final Argument<String> KEYSTORE_PASSWORD_ARG = stringArgument("-p", "--keystore-password")
            .description("Password for the keystore file")
            .build();
    private static final Argument<String> CERTIFICATE_PASSWORD_ARG = stringArgument("-s", "--certificate-password")
            .description("Password for the certificate")
            .build();
    private static final Argument<Integer> PORT_ARG = integerArgument("-l", "--listen-port")
            .defaultValue(443)
            .description("HTTPS port to listen on")
            .build();
    private static final Argument<String> QUEUE_NAME_ARG = stringArgument("-w", "--webhook-queue-name")
            .defaultValue("WebHookQueue")
            .description("The name of the JMS Queue for GitHub WebHook messages")
            .build();

    public static void main(final String args[]) throws InterruptedException {
        final CommandLineParser parser = CommandLineParser.withArguments(
                HELP_ARG,
                PORT_ARG,
                KEYSTORE_FILE_ARG,
                KEYSTORE_PASSWORD_ARG,
                CERTIFICATE_PASSWORD_ARG,
                QUEUE_NAME_ARG);

        try {
            final ParsedArguments parsedArguments = parser.parse(args);

            final Path keystore = Optional.ofNullable(parsedArguments.get(KEYSTORE_FILE_ARG)).map(File::toPath).orElse(null);
            if (keystore == null || !Files.exists(keystore)) {
                System.out.println("keystore does not exist: " + keystore);
                System.exit(ExitCodes.NO_SUCH_KEYSTORE);
            }
            final Optional<String> keystorePassword = Optional.ofNullable(parsedArguments.get(KEYSTORE_PASSWORD_ARG)).filter(s -> !s.isEmpty());
            final Optional<String> certificatePassword = Optional.ofNullable(parsedArguments.get(CERTIFICATE_PASSWORD_ARG)).filter(s -> !s.isEmpty());
            final Integer port = parsedArguments.get(PORT_ARG);

            final String queueName = parsedArguments.get(QUEUE_NAME_ARG);
            final JMSClient.Settings jmsClientSettings = new JMSClient.Settings(queueName);
            try (final JMSClient jmsClient = new JMSClient(jmsClientSettings)) {

                jmsClient.start();

                final Server.Settings serverSettings = new Server.Settings(keystore, keystorePassword, certificatePassword, port);
                final Server server = new Server(serverSettings, new WebHookPayloadSummaryJmsProcessor(jmsClient));
                server.runSync();
            }
        } catch (final ArgumentException e) {
            System.out.println(e.getMessageAndUsage());
            System.exit(ExitCodes.INVALID_ARGUMENT);

        } catch (final IOException e) {
            System.out.println(e.getMessage());
            System.exit(ExitCodes.UNABLE_TO_CONNECT_TO_JMS_BROKER);
        }
    }
}
