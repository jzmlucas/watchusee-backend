package br.com.watchusee.watchusee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient tmdbRestClient(
            TmdbProperties tmdbProperties
    ) {

        HttpClient httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                tmdbProperties.connectTimeout()
                        )
                        .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                tmdbProperties.readTimeout()
        );

        return RestClient.builder()
                .baseUrl(tmdbProperties.baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(
                        (request, body, execution) ->
                                executeWithRetry(
                                        request,
                                        body,
                                        execution,
                                        tmdbProperties
                                )
                )
                .build();
    }

    private org.springframework.http.client.ClientHttpResponse executeWithRetry(
            org.springframework.http.HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution,
            TmdbProperties properties
    ) throws IOException {

        int attempt = 0;

        while (true) {

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