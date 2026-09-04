package br.com.watchusee.watchusee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient tmdbRestClient(
            TmdbProperties tmdbProperties
    ) {
        return RestClient.builder()
                .baseUrl(tmdbProperties.baseUrl())
                .build();
    }
}