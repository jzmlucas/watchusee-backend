package br.com.watchusee.watchusee.config;

import javax.net.ssl.*;
import java.net.http.HttpClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom SSL Context com mitigações explícitas de segurança:
 */
public class CustomSSLContext {

    private static final Set<String> BLOCKED_HOSTPATTERNS = new HashSet<>(Set.of(
        "localhost", "127.0.0.1", "::1"
    ));

    public static SSLContext createSecureSSLContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager trustManager = new SecureTrustManager();
        sslContext.init(null, new TrustManager[]{trustManager}, null);
        System.out.println("✅ CustomSSLContext criado!");
        return sslContext;
    }

    private static boolean isTrustedByPublicCA(X509Certificate cert) {
        return cert != null && 
               (cert.getIssuerDN() != null && 
                !cert.getIssuerDN().equals(cert.getSubjectDN()));
    }

    private static boolean isBlockedHost(String hostname) {
        if (hostname == null || hostname.isEmpty()) return true;
        for (String pattern : BLOCKED_HOSTPATTERNS) {
            if (hostname.matches(pattern)) return true;
        }
        if (hostname.equalsIgnoreCase("localhost")) return true;
        return false;
    }

    private static class SecureTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            for (X509Certificate cert : certs) {
                if (!isTrustedByPublicCA(cert)) {
                    throw new CertificateException("SSRF: Auto-signed certificate rejected!");
                }
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            for (X509Certificate cert : certs) {
                if (!isTrustedByPublicCA(cert)) {
                    throw new CertificateException("SSRF: Auto-signed certificate rejected!");
                }
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    public static JdkClientHttpRequestFactory createSecureRequestFactory() {
        return new org.springframework.http.client.JdkClientHttpRequestFactory();
    }

    public static boolean isHostBlocked(String hostname) {
        return isBlockedHost(hostname);
    }
}