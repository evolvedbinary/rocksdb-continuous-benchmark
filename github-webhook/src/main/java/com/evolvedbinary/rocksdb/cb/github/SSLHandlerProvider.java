package com.evolvedbinary.rocksdb.cb.github;

import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Optional;

public class SSLHandlerProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSLHandlerProvider.class);

    private static final String PROTOCOL = "TLS";
    private static final String[] ENABLED_PROTOCOLS = new String[] { "TLSv1.3", "TLSv1.2" };
    private static final String ALGORITHM_SUN_X509 = "SunX509";
    private static final String ALGORITHM = "ssl.KeyManagerFactory.algorithm";
    private static final String KEYSTORE_TYPE = "PKCS12";

    private final Path keystore;
    private final Optional<String> keystorePassword;
    private final Optional<String> certificatePassword;
    private SSLContext serverSSLContext = null;

    public SSLHandlerProvider(final Path keystore, final Optional<String> keystorePassword, final Optional<String> certificatePassword) {
        this.keystore = keystore;
        this.keystorePassword = keystorePassword;
        this.certificatePassword = certificatePassword;
    }

    public void init() {
        LOGGER.info("Initiating SSL context from keystore: " + keystore.toAbsolutePath());
        LOGGER.info("Enabled protocols: " + Arrays.toString(ENABLED_PROTOCOLS));

        String algorithm = Security.getProperty(ALGORITHM);
        if (algorithm == null) {
            algorithm = ALGORITHM_SUN_X509;
        }
        KeyStore ks = null;
        try (final InputStream is = Files.newInputStream(keystore)) {
            ks = KeyStore.getInstance(KEYSTORE_TYPE);
            ks.load(is, keystorePassword.map(String::toCharArray).orElse(null));
        } catch (final IOException e) {
            LOGGER.error("Cannot load the keystore file", e);
        } catch (final CertificateException e) {
            LOGGER.error("Cannot get the certificate", e);
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.error("Somthing wrong with the SSL algorithm", e);
        } catch (final KeyStoreException e) {
            LOGGER.error("Cannot initialize keystore", e);
        }

        try {

            // Set up key manager factory to use our key store
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, certificatePassword.map(String::toCharArray).orElse(null));
            final KeyManager[] keyManagers = kmf.getKeyManagers();

            // Setting trust store null since we don't need a CA certificate or Mutual Authentication
            TrustManager[] trustManagers = null;

            serverSSLContext = SSLContext.getInstance(PROTOCOL);
            serverSSLContext.init(keyManagers, trustManagers, null);


        } catch (Exception e) {
            LOGGER.error("Failed to initialize the server-side SSLContext", e);
        }
    }

    public SslHandler getSSLHandler() {
        SSLEngine sslEngine = null;
        if (serverSSLContext == null) {
            LOGGER.error("Server SSL context is null");
            System.exit(-1);
        } else {
            sslEngine = serverSSLContext.createSSLEngine();
            sslEngine.setEnabledProtocols(ENABLED_PROTOCOLS);
            sslEngine.setUseClientMode(false);
            sslEngine.setNeedClientAuth(false);

        }
        return new SslHandler(sslEngine);
    }
}
