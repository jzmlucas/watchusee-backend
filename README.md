# WatchuSee Backend

[![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-brightgreen?style=for-the-badge&logo=springsecurity)](https://spring.io/projects/spring-security)
[![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-Hibernate-brightgreen?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-data-jpa)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/Auth-JWT-black?style=for-the-badge&logo=jsonwebtokens)](https://jwt.io/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0.2-6BA539?style=for-the-badge&logo=openapiinitiative)](https://www.openapis.org/)
[![Swagger](https://img.shields.io/badge/Swagger-Documentation-85EA2D?style=for-the-badge&logo=swagger)](https://swagger.io/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker)](https://www.docker.com/)
[![JUnit 5](https://img.shields.io/badge/JUnit%205-25A162?style=for-the-badge&logo=junit5)](https://junit.org/junit5/)

**API REST do WatchuSee**, responsável por descoberta de filmes via TMDB, watchlist pessoal, perfis, amizades e compartilhamento de recomendações entre usuários, com autenticação JWT própria e persistência em PostgreSQL.

A documentação técnica completa — entidades, DTOs, services, segurança, débito técnico conhecido e decisões arquiteturais — está em **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**. Este README cobre apenas o essencial para rodar o projeto e entender o que ele faz.

**API ao vivo:** [watchusee-backend.onrender.com](https://watchusee-backend.onrender.com)
**Swagger UI:** [watchusee-backend.onrender.com/swagger-ui/index.html](https://watchusee-backend.onrender.com/swagger-ui/index.html)
**Aplicativo Android:** [watchusee-android](https://github.com/jzmlucas/watchusee-android)

> A API roda em uma instância gratuita do Render, que hiberna sem tráfego. A primeira requisição depois de um período ocioso pode levar alguns segundos a mais para responder.

## Sobre o projeto

O WatchuSee reúne três funções que costumam ser separadas em outros aplicativos:

- **Descoberta de filmes** através da API do TMDB.
- **Watchlist pessoal**, com os estados `TO_WATCH` e `WATCHED`.
- **Recursos sociais**: amizades e compartilhamento de recomendações entre usuários.

A API mantém o contrato do TMDB isolado do cliente através de DTOs e mapeadores próprios — o cliente do WatchuSee nunca fala diretamente o "formato" do TMDB.

## Funcionalidades

- Cadastro e login com JWT.
- Logout com revogação de token.
- Troca de senha e invalidação de sessões anteriores.
- Bloqueio temporário de conta após tentativas de login inválidas.
- Busca e perfis de usuários.
- Avatar por ícones predefinidos e filme favorito.
- Busca, detalhes, tendências, populares, em cartaz, próximos lançamentos, similares, recomendações, avaliações, listas e trailer de filmes.
- Watchlist paginada e filtrável por status, incluindo consulta da watchlist de outros usuários.
- Solicitações de amizade: enviar, aceitar/recusar, listar, contar e consultar status do relacionamento.
- Compartilhamento de filmes entre usuários.
- Rate limiting para autenticação, cadastro e endpoints públicos de filmes.
- Tratamento global e consistente de erros (`GlobalExceptionHandler`).
- OpenAPI/Swagger disponível em `/swagger-ui/index.html`.

## Arquitetura

Layered architecture organizada por **package-by-feature**: cada módulo de negócio (`user`, `movie`, `watchlist`, `friend`, `share`) concentra sua própria entidade, repository, service, controller e DTOs.

<details>
<summary>Ver diagrama de arquitetura</summary>

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

Detalhes de componentes, autorização e mitigação de SSRF na integração com o TMDB estão em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#autenticação-e-segurança).

## API

Prefixo: `/api/v1`

| Recurso | Principais endpoints |
|---|---|
| Auth | `/auth/login`, `/auth/logout` |
| Users | `/users`, `/users/search`, `/users/me/profile`, `/users/{userId}/profile` |
| Movies | `/movies/search`, `/movies/{movieId}`, `/movies/trending/*`, `/movies/popular`, `/movies/upcoming` |
| Watchlist | `/watchlist`, `/users/{userId}/watchlist` |
| Friends | `/friends`, `/friends/requests/*`, `/friends/status/{userId}` |
| Shares | `/shares`, `/shares/received`, `/shares/pending`, `/shares/sent` |

A lista completa de endpoints, parâmetros, DTOs e status HTTP está em **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#api--endpoints)**.

## Banco de dados

- PostgreSQL hospedado no Supabase.
- Spring Data JPA + Hibernate, schema `app`.
- `spring.jpa.open-in-view=false` — sem lazy loading implícito na camada web.
- `ddl-auto=update` (sem Flyway/Liquibase — ver limitações abaixo).
- Persistência de usuários, filmes referenciados, watchlists, amizades e compartilhamentos.

## Integração externa

O TMDB é a única fonte externa de dados de filmes. A aplicação tem um `TmdbClient` próprio, com whitelist de hosts, validação de certificado TLS e retry com backoff exponencial contra falhas transitórias — detalhes em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#integração-externa-com-tmdb).

## Como executar

### Pré-requisitos

- Java 21
- Maven Wrapper incluído no projeto (não precisa de Maven instalado globalmente)
- PostgreSQL/Supabase
- API Key do TMDB

### Configuração

Defina as variáveis de ambiente utilizadas pela aplicação:

```bash
export TMDB_API_KEY="sua_chave"
export SUPABASE_URL="jdbc:postgresql://<host>:<porta>/<database>"
export SUPABASE_USERNAME="seu_usuario"
export SUPABASE_PASSWORD="sua_senha"
export JWT_SECRET="sua_chave_jwt_com_pelo_menos_32_caracteres"
export DUMMY_PASSWORD_HASH="hash_bcrypt_qualquer"
```

`DUMMY_PASSWORD_HASH` é obrigatória: é um hash BCrypt fixo usado para igualar o tempo de resposta do login quando o nick não existe (mitigação de enumeração de usuários). **Sem ela, a aplicação não sobe.** A lista completa de variáveis, com as opcionais e seus defaults, está em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#variáveis-de-ambiente).

Depois:

```bash
./mvnw spring-boot:run
```

### Docker

```bash
docker build -t watchusee-backend .
docker run -p 8080:8080 \
  -e TMDB_API_KEY="sua_chave" \
  -e SUPABASE_URL="jdbc:postgresql://<host>:<porta>/<database>" \
  -e SUPABASE_USERNAME="seu_usuario" \
  -e SUPABASE_PASSWORD="sua_senha" \
  -e JWT_SECRET="sua_chave_jwt" \
  -e DUMMY_PASSWORD_HASH="hash_bcrypt_qualquer" \
  watchusee-backend
```

> O `docker-compose.yml` do repositório ainda não repassa `DUMMY_PASSWORD_HASH` ao serviço `app` — adicione-a manualmente (ex. via `docker-compose.override.yml`) antes de rodar `docker compose up`, ou o container sobe e cai imediatamente no boot.

## Testes

```bash
./mvnw clean test
```

Cobertura atual: unit tests de todos os services (Mockito + AssertJ) e testes dedicados de `JwtService`/blacklist de token. Não há ainda testes de controller (`@WebMvcTest`) nem de integração ponta a ponta — detalhes em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#testes).

## Notas de segurança

- A documentação OpenAPI está disponível publicamente em `/swagger-ui/index.html` e `/v3/api-docs`, desabilite em produção se isso não for desejado.
- `ddl-auto=update` aplica alterações de schema automaticamente a cada boot; não é recomendado para produção com dados reais sem uma estratégia de migração (Flyway/Liquibase).
- Autorização de recurso (dono vs. terceiros) é feita manualmente em cada service, não via `@PreAuthorize`, ver [Autorização](docs/ARCHITECTURE.md#autorização--como-o-sistema-impede-acesso-a-dados-de-outros-usuários) no documento técnico.
- Rate limiting e blacklist de token são em memória, por instância — não sobrevivem a reinício e não são compartilhados entre réplicas.

## Documentação detalhada

A documentação técnica aprofundada está separada para manter este README objetivo:

**[Arquitetura e documentação técnica](docs/ARCHITECTURE.md)**

Ela contém: entidades e relacionamentos, DTOs, services, repositories, controllers, segurança e JWT, banco de dados, integração TMDB, todos os endpoints, validações, tratamento de erros, dependências, testes, decisões arquiteturais, débito técnico/bugs conhecidos e melhorias futuras.

## Autor

Desenvolvido por **Lucas Joly**.

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://linkedin.com/in/jzmlucas)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/jzmlucas)