package br.com.watchusee.watchusee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
@Configuration
public class RestClientConfig {

    @Bean
    public IpAllowlistConfig ipAllowlistConfig(TmdbProperties tmdbProperties) {
        Set<String> hosts = tmdbProperties.allowedHosts();
        
        if (hosts == null || hosts.isEmpty()) {
            throw new IllegalStateException(
                "❗ SSRF Mitigation: tmdb.allowed-hosts é obrigatório em application.properties!"
            );
        }
        
        return new IpAllowlistConfig(hosts, true);
    }

    @Bean
    public RestClient tmdbRestClient(
            TmdbProperties tmdbProperties,
            IpAllowlistConfig ipAllowlistConfig
    ) {
        
        JdkClientHttpRequestFactory requestFactory = CustomSSLContext.createSecureRequestFactory();

        return RestClient.builder()

                .baseUrl(URI.create(tmdbProperties.baseUrl()))
                .requestFactory(requestFactory)

                .requestInterceptor((request, body, execution) -> 
                    executeWithAllowlistCheckAndRetry(
                        request,
                        body,
                        execution,
                        tmdbProperties,
                        ipAllowlistConfig
                    )
                )
                .build();
    }

    private org.springframework.http.client.ClientHttpResponse executeWithAllowlistCheckAndRetry(
            org.springframework.http.HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution,
            TmdbProperties properties,
            IpAllowlistConfig allowlistConfig
    ) throws IOException {

        int attempt = 0;

        while (true) {

            String urlString = request.getURI().toString();
            String host = IpAllowlistConfig.extractHostFromUrl(urlString);
            if (!allowlistConfig.isAllowed(host)) {
                throw new SecurityException(
                    "SSRF Mitigation: Access denied to host: " + host 
                    + (allowlistConfig.logViolationAttempts() ? " - Logged violation!" : "")
                );
            }

            try {
                
                var response =
                        execution.execute(request, body);

                HttpStatusCode status =
                        response.getStatusCode();

                if (!isRetryableStatus(status)
                        || attempt >= properties.maxRetries()) {

                    return response;
                }

                response.close();

            } catch (IOException exception) {

                if (attempt >= properties.maxRetries()) {
                    throw exception;
                }
            }

            attempt++;

            sleepBeforeRetry(
                    attempt,
                    properties
            );
        }
    }

    private boolean isRetryableStatus(
            HttpStatusCode status
    ) {

        return status.value() == 429
                || status.is5xxServerError();
    }

    private void sleepBeforeRetry(
            int attempt,
            TmdbProperties properties
    ) {

        Duration initialDelay =
                properties.retryInitialDelay();

        Duration maxDelay =
                properties.retryMaxDelay();

        long initialMillis =
                initialDelay.toMillis();

        long maxMillis =
                maxDelay.toMillis();

        long delayMillis =
                initialMillis * (1L << Math.min(attempt - 1, 10));

        delayMillis =
                Math.min(
                        delayMillis,
                        maxMillis
                );

        try {

            Thread.sleep(delayMillis);

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Thread interrompida durante retry do TMDB.",
                    exception
            );
        }
    }
}