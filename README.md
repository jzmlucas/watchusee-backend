# WatchuSee Backend

## Tecnologias

[![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.8-brightgreen?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-brightgreen?style=for-the-badge&logo=springsecurity)](https://spring.io/projects/spring-security)
[![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-Hibernate-brightgreen?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-data-jpa)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/Auth-JWT-black?style=for-the-badge&logo=jsonwebtokens)](https://jwt.io/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0.2-6BA539?style=for-the-badge&logo=openapiinitiative)](https://www.openapis.org/)
[![Swagger](https://img.shields.io/badge/Swagger-Documentation-85EA2D?style=for-the-badge&logo=swagger)](https://swagger.io/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker)](https://www.docker.com/)
[![JUnit 5](https://img.shields.io/badge/JUnit%205-25A162?style=for-the-badge&logo=junit5)](https://junit.org/junit5/)

**API REST do WatchuSee**, responsável por descoberta de filmes via TMDB, watchlist pessoal, perfis, amizades e compartilhamento de recomendações entre usuários, com autenticação JWT e persistência em PostgreSQL.  

A documentação completa de endpoints, parâmetros, DTOs, status HTTP e regras de negócio está em **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

**API ao vivo:** [watchusee-backend.onrender.com](https://watchusee-backend.onrender.com)  
**Swagger UI:** [watchusee-backend.onrender.com/swagger-ui/index.html](https://watchusee-backend.onrender.com/swagger-ui/index.html)  
**Aplicativo Android:** [watchusee-android](https://github.com/jzmlucas/watchusee-android)

## Sobre o projeto

O WatchuSee reúne três funções que são separadas em outros aplicativos:

- **Descoberta de filmes** através da API do TMDB.
- **Watchlist pessoal**, com os estados `TO_WATCH` e `WATCHED`.
- **Recursos sociais**, incluindo amizades e compartilhamento de recomendações.

A API mantém o contrato do TMDB isolado do cliente através de DTOs e mapeadores próprios.

## Funcionalidades

- Cadastro e login com JWT.
- Logout com revogação de token.
- Troca de senha e invalidação de sessões anteriores.
- Bloqueio temporário após tentativas de login inválidas.
- Busca e perfis de usuários.
- Avatar por ícones predefinidos.
- Filme favorito.
- Busca, detalhes, tendências, populares, em cartaz, próximos lançamentos, similares, recomendações, avaliações, listas e trailer.
- Watchlist paginada e filtrável por status.
- Consulta da watchlist de outros usuários.
- Solicitações de amizade, aceite/recusa, listagem, contagem e status do relacionamento.
- Compartilhamento de filmes entre usuários.
- Rate limiting para autenticação e endpoints públicos de filmes.
- Tratamento global e consistente de erros.
- OpenAPI/Swagger.

##  Arquitetura

O backend utiliza **Layered Architecture**, organizada por **package-by-feature**.

<details>
<summary>Ver arquitetura</summary>

```mermaid
flowchart TD
    Client["Cliente HTTP"] --> RL["RateLimitingFilter"]
    RL --> JWT["JwtAuthenticationFilter"]
    JWT --> Security["SecurityContext"]
    Security --> Controller["Controllers REST"]
    Controller --> DTO["DTO + Bean Validation"]
    DTO --> Service["Services"]
    Service --> Domain["Entidades JPA"]
    Domain --> Repository["Spring Data JPA"]
    Repository --> DB[("PostgreSQL")]

    Service --> TMDBClient["TmdbClient"]
    TMDBClient --> TMDB[("TMDB API")]
```

</details>

### Fluxo de autenticação

<details>
<summary>Ver fluxo de autenticação</summary>

```mermaid
flowchart TD
    Login["POST /api/v1/auth/login"] --> Check{"Conta bloqueada?"}
    Check -->|Sim| Locked["AccountLockedException"]
    Check -->|Não| Password{"Senha confere?"}
    Password -->|Não| Failed["Incrementa tentativas"]
    Password -->|Sim| Token["JwtService.generateToken()"]
    Token --> Response["JWT"]
    Response --> Client["Cliente"]
    Client --> Request["Authorization: Bearer <token>"]
    Request --> RL["RateLimitingFilter"]
    RL --> JWT["JwtAuthenticationFilter"]
    JWT --> Context["SecurityContext"]
    Context --> Controller["Controller"]
```

</details>

## API

Prefixo: `/api/v1`

| Recurso | Principais endpoints |
|---|---|
|  Auth | `/auth/login`, `/auth/logout` |
|  Users | `/users`, `/users/search`, `/users/me/profile`, `/users/{userId}/profile` |
|  Movies | `/movies/search`, `/movies/{movieId}`, `/movies/trending/*`, `/movies/popular`, `/movies/upcoming` |
|  Watchlist | `/watchlist`, `/users/{userId}/watchlist` |
|  Friends | `/friends`, `/friends/requests/*`, `/friends/status/{userId}` |
|  Shares | `/shares`, `/shares/received`, `/shares/pending`, `/shares/sent` |

A documentação completa de endpoints, parâmetros, DTOs, status HTTP e regras de negócio está em **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

##  Banco de dados

- PostgreSQL hospedado no Supabase.
- Spring Data JPA + Hibernate.
- Schema `app`.
- `spring.jpa.open-in-view=false`.
- Persistência de usuários, filmes referenciados, watchlists, amizades e compartilhamentos.

## Integração externa

O TMDB é utilizado como fonte de dados de filmes.

A aplicação possui um `TmdbClient` próprio e mantém os DTOs do TMDB separados dos DTOs expostos pela API.

## Como executar

### Pré-requisitos

- Java 21
- Maven Wrapper incluído no projeto
- PostgreSQL/Supabase
- API Key do TMDB

### Configuração

Defina as variáveis de ambiente utilizadas pela aplicação:

```bash
export TMDB_API_KEY="sua_chave"
export SUPABASE_URL="jdbc:postgresql://<host>:<porta>/<database>"
export SUPABASE_USERNAME="seu_usuario"
export SUPABASE_PASSWORD="sua_senha"
export JWT_SECRET="sua_senha_jwt"
```

Depois:

```bash
./mvnw spring-boot:run
```

### Docker

```bash
docker build -t watchusee-backend .
docker run -p 8080:8080   -e TMDB_API_KEY="sua_chave"   -e SUPABASE_URL="jdbc:postgresql://<host>:<porta>/<database>"   -e SUPABASE_USERNAME="seu_usuario"   -e SUPABASE_PASSWORD="sua_senha"   -e JWT_SECRET="sua_chave"   watchusee-backend
```

## Documentação detalhada

A documentação técnica aprofundada está separada para manter este README objetivo:

 **[Arquitetura e documentação técnica](docs/ARCHITECTURE.md)**

Ela contém:

- classes e responsabilidades;
- entidades e relacionamentos;
- DTOs;
- Services;
- Repositories;
- Controllers;
- segurança e JWT;
- banco de dados;
- integração TMDB;
- todos os endpoints;
- validações;
- tratamento de erros;
- dependências;
- testes;
- decisões arquiteturais;
- problemas encontrados;
- melhorias futuras.

## Testes

Execute:

```bash
./mvnw clean test
```

## Autor

Desenvolvido por **Lucas Joly**.

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://linkedin.com/in/jzmlucas)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/jzmlucas)
