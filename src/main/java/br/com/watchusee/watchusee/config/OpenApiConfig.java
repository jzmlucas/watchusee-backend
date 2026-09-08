package br.com.watchusee.watchusee.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI watchuSeeOpenAPI() {

        String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("WatchuSee API")
                        .version("v1.0.0")
                        .description("""
                                # WatchuSee API

                                API REST responsável pelo backend da plataforma WatchuSee,
                                uma aplicação para descoberta, organização e gerenciamento
                                de filmes.

                                ## Sobre o projeto

                                O WatchuSee permite que usuários pesquisem filmes,
                                organizem seus filmes em uma watchlist e acompanhem
                                seu histórico de filmes assistidos.

                                A aplicação utiliza o **TMDB (The Movie Database)** como
                                fonte externa de informações sobre filmes.

                                ## Principais funcionalidades

                                ### Filmes
                                • Pesquisa de filmes por título.
                                • Consulta de informações detalhadas de filmes.
                                • Integração com a API do TMDB.

                                ### Watchlist
                                • Adicionar filmes à lista de filmes para assistir.
                                • Remover filmes da lista.
                                • Marcar filmes como assistidos.
                                • Remover filmes da lista de assistidos.
                                • Consultar o status de um filme na watchlist.

                                ### Usuários
                                • Cadastro de usuários.
                                • Autenticação.
                                • Gerenciamento de perfil.
                                • Alteração de senha.
                                • Consulta de estatísticas do usuário.

                                ### Amizades
                                • Adicionar usuários como amigos.
                                • Consultar a quantidade de amigos.
                                • Gerenciar relacionamentos entre usuários.

                                ## Segurança

                                Os endpoints protegidos utilizam autenticação baseada
                                em **JWT (JSON Web Token)**.

                                Para acessar endpoints protegidos, o token deve ser
                                enviado no header HTTP:

                                `Authorization: Bearer {token}`

                                ## Arquitetura

                                O backend foi desenvolvido utilizando **Java e Spring Boot**,
                                seguindo uma arquitetura organizada em camadas.

                                Principais responsabilidades:

                                • **Controller** — exposição dos endpoints REST.
                                • **Service** — implementação das regras de negócio.
                                • **Domain** — entidades e regras do domínio.
                                • **Repository** — acesso e gerenciamento dos dados.
                                • **Mapper** — conversão entre entidades e DTOs.
                                • **DTO** — objetos utilizados na comunicação da API.
                                • **Client** — integração com serviços externos.

                                ## Integrações externas

                                O WatchuSee utiliza a API do **The Movie Database (TMDB)**
                                para obter informações sobre filmes.

                                ## Projeto

                                O código-fonte do backend está disponível no GitHub:
                                https://github.com/jzmlucas/watchusee-backend
                                """)
                        .contact(new Contact()
                                .name("Github do Projeto")
                                .url("https://github.com/jzmlucas/watchusee-backend")
                        )
                        .license(new License()
                                .name("MIT License")
                        )
                )

                .tags(List.of(
                        new Tag()
                                .name("Movies")
                                .description(
                                        "Pesquisa e consulta de informações sobre filmes."
                                ),

                        new Tag()
                                .name("Watchlist")
                                .description(
                                        "Gerenciamento dos filmes que o usuário deseja assistir ou já assistiu."
                                ),

                        new Tag()
                                .name("Users")
                                .description(
                                        "Cadastro, autenticação e gerenciamento de usuários e seus perfis."
                                ),

                        new Tag()
                                .name("Friends")
                                .description(
                                        "Gerenciamento de amizades entre usuários."
                                ),

                        new Tag()
                                .name("Authentication")
                                .description(
                                        "Autenticação e gerenciamento de acesso à API."
                                )
                ))

                .components(new Components()
                        .addSecuritySchemes(
                                securitySchemeName,
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(securitySchemeName)
                )

                .externalDocs(new ExternalDocumentation()
                        .description("Documentação oficial da API do TMDB")
                        .url("https://developer.themoviedb.org/docs")
                );
    }
}