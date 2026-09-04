package br.com.watchusee.watchusee.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI watchuSeeOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("WatchuSee API")
                        .version("v1")
                        .description("""
                                API REST para gerenciamento de filmes e watchlist.

                                A API permite:

                                • Pesquisar filmes utilizando o TMDB.
                                • Consultar detalhes de um filme.
                                • Adicionar filmes à lista "assistir".
                                • Remover filmes da lista "assistir".
                                • Marcar filmes como assistidos.
                                • Remover filmes da lista de assistidos.
                                • Consultar o status de um filme na watchlist.

                                ## Arquitetura

                                A aplicação utiliza uma arquitetura em camadas,
                                separando Controller, Service, Domain, Mapper,
                                Repository e Client externo.

                                ## Persistência

                                Neste MVP a watchlist utiliza armazenamento
                                exclusivamente em memória. Os dados não são
                                persistidos após a reinicialização da aplicação.

                                ## Integração externa

                                Os dados dos filmes são obtidos através da API
                                do The Movie Database (TMDB).
                                """)
                        .contact(new Contact()
                                .name("WatchuSee")
                                .url("https://github.com/")
                        )
                        .license(new License()
                                .name("MIT License")
                        )
                )
                .tags(List.of(
                        new Tag()
                                .name("Movies")
                                .description(
                                        "Operações relacionadas à consulta de filmes."
                                ),

                        new Tag()
                                .name("Watchlist")
                                .description(
                                        "Gerenciamento das listas de filmes para assistir e assistidos."
                                )
                ))
                .components(new Components())
                .externalDocs(new ExternalDocumentation()
                        .description("The Movie Database API")
                        .url("https://developer.themoviedb.org/docs")
                );
    }
}