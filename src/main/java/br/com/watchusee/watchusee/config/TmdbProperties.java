package br.com.watchusee.watchusee.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "tmdb")
public record TmdbProperties(

        @NotBlank
        String baseUrl,

        @NotBlank
        String apiKey,

        @NotBlank
        String language,

        Duration connectTimeout,

        Duration readTimeout,

        @Min(0)
        int maxRetries,

        Duration retryInitialDelay,

        Duration retryMaxDelay,

        Set<String> allowedHosts
) {
}