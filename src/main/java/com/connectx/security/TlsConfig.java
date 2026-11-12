package com.connectx.security;

import javax.net.ssl.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

public final class TlsConfig {

    private TlsConfig() {
    }

    private static String setting(
            String environmentVariable,
            String defaultValue) {

        String value = System.getenv(environmentVariable);

        return value == null || value.isBlank()
                ? defaultValue
                : value;
    }

    public static SSLContext createServerContext() throws Exception {

        String path = setting(
                "CONNECTX_KEYSTORE_PATH",
                "certs/connectx-local.p12"
        );

        char[] password = setting(
                "CONNECTX_KEYSTORE_PASSWORD",
                "changeit"
        ).toCharArray();

        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream input = Files.newInputStream(Path.of(path))) {
            keyStore.load(input, password);
        }

        KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(
                        KeyManagerFactory.getDefaultAlgorithm()
                );

        keyManagerFactory.init(keyStore, password);

        SSLContext context = SSLContext.getInstance("TLS");

        context.init(
                keyManagerFactory.getKeyManagers(),
                null,
                null
        );

        return context;
    }

    public static SSLContext createClientContext() throws Exception {

        String path = setting(
                "CONNECTX_TRUSTSTORE_PATH",
                "certs/connectx-truststore.p12"
        );

        char[] password = setting(
                "CONNECTX_TRUSTSTORE_PASSWORD",
                "changeit"
        ).toCharArray();

        KeyStore trustStore = KeyStore.getInstance("PKCS12");

        try (InputStream input = Files.newInputStream(Path.of(path))) {
            trustStore.load(input, password);
        }

        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm()
                );

        trustManagerFactory.init(trustStore);

        SSLContext context = SSLContext.getInstance("TLS");

        context.init(
                null,
                trustManagerFactory.getTrustManagers(),
                null
        );

        return context;
    }
}
