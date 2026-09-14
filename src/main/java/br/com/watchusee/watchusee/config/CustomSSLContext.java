package br.com.watchusee.watchusee.config;

import java.net.http.HttpClient;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.http.client.JdkClientHttpRequestFactory;

public final class CustomSSLContext {

    private static final Set<String> BLOCKED_HOSTS = new HashSet<>(Set.of(
            "localhost",
            "127.0.0.1",
            "::1"
    ));

    private CustomSSLContext() {
    }

    public static SSLContext createSecureSSLContext() throws Exception {

        SSLContext sslContext = SSLContext.getInstance("TLS");

        TrustManager[] trustManagers = new TrustManager[]{
                new SecureTrustManager()
        };

        sslContext.init(null, trustManagers, null);

        return sslContext;
    }

    private static boolean isBlockedHost(String hostname) {

        if (hostname == null || hostname.isBlank()) {
            return true;
        }

        String normalizedHost = hostname
                .trim()
                .toLowerCase(Locale.ROOT);

        return BLOCKED_HOSTS.contains(normalizedHost);
    }

    private static final class SecureTrustManager implements X509TrustManager {

        private final X509TrustManager defaultTrustManager;

        private SecureTrustManager() throws Exception {
            this.defaultTrustManager = createDefaultTrustManager();
        }

        private static X509TrustManager createDefaultTrustManager()
                throws Exception {

            var factory = javax.net.ssl.TrustManagerFactory.getInstance(
                    javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
            );

            factory.init((java.security.KeyStore) null);

            for (TrustManager trustManager : factory.getTrustManagers()) {

                if (trustManager instanceof X509TrustManager x509TrustManager) {
                    return x509TrustManager;
                }
            }

            throw new IllegalStateException(
                    "Nenhum X509TrustManager padrão encontrado."
            );
        }

        @Override
        public void checkClientTrusted(
                X509Certificate[] chain,
                String authType
        ) throws CertificateException {

            defaultTrustManager.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(
                X509Certificate[] chain,
                String authType
        ) throws CertificateException {

            defaultTrustManager.checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {

            return defaultTrustManager.getAcceptedIssuers();
        }
    }

    public static JdkClientHttpRequestFactory createSecureRequestFactory()
            throws Exception {

        SSLContext sslContext = createSecureSSLContext();

        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        return new JdkClientHttpRequestFactory(httpClient);
    }

    public static boolean isHostBlocked(String hostname) {

        return isBlockedHost(hostname);
    }
}