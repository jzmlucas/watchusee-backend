# WatchuSee Backend

[![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.8-brightgreen?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-336791?style=for-the-badge&logo=postgresql)](https://supabase.com/)
[![JWT](https://img.shields.io/badge/Auth-JWT-black?style=for-the-badge&logo=jsonwebtokens)](https://jwt.io/)
[![Swagger](https://img.shields.io/badge/OpenAPI-Swagger-blue?style=for-the-badge&logo=openapiinitiative)](https://swagger.io/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](#licença)

**API REST que centraliza descoberta de filmes (via TMDB), watchlist pessoal e compartilhamento de recomendações entre usuários — com autenticação JWT e persistência em PostgreSQL.**

🔗 **API ao vivo:** [watchusee-backend.onrender.com](https://watchusee-backend.onrender.com)  
📄 **Swagger UI:** [watchusee-backend.onrender.com/swagger-ui/index.html](https://watchusee-backend.onrender.com/swagger-ui/index.html)  
📱 **App Android consumindo esta API:** [watchusee-android](https://github.com/jzmlucas/watchusee-android)  

---

## O QUE ESSE PROJETO RESOLVE

Os aplicativos de lista organizada de cinema não permitem que eu compartilhe os filmes de modo dinâmico com meus amigos. Esse projeto resolve isso.

1. Agrega dados de filmes do TMDB sem expor o contrato da API externa para o cliente;
2. Mantém o estado de uma watchlist pessoal por usuário autenticado;
3. Permite que usuários **recomendem filmes uns aos outros**, transformando a watchlist em algo social, não só uma lista pessoal.
4. Perfil personalizado para o usuário.


---

## VISÃO GERAL

A API disponibiliza recursos centralizados para:

- **Autenticação:** Cadastro de usuários e login stateless via **JWT**, com senhas protegidas por hash (`PasswordEncoder`).
- **Busca e Descoberta:** Pesquisa por título, filmes em alta (tendência semanal e aleatória), mais bem avaliados e filmes similares — todos via TMDB.
- **Detalhamento:** Detalhes e trailer de um filme específico.
- **Gerenciamento de Watchlist:** Transição do ciclo de vida de filmes no acervo do usuário autenticado (`TO_WATCH` → `WATCHED`).
- **Compartilhamento Social:** Envio, aceite e recusa de recomendações de filmes entre usuários cadastrados.
- **Perfil:** Consulta do perfil do usuário autenticado, com contadores de filmes assistidos e pendentes.
- **Validação de Entrada:** Tratamento defensivo de dados via Jakarta Bean Validation.
- **Resiliência e Padronização:** Manipulação global de exceções, garantindo respostas de erro padronizadas e com contexto de requisição (`timestamp`, `status`, `error`, `message`, `path`).
- **Documentação Viva:** Contratos da API expostos dinamicamente via OpenAPI / Swagger UI.

---

## DECISÕES TÉCNICAS

Algumas escolhas de design e os trade-offs por trás delas:

**In-memory → JPA/PostgreSQL.** A primeira versão usava repositórios em memória para validar rapidamente o domínio e os contratos da API sem o custo de configurar infraestrutura. A migração para Spring Data JPA + PostgreSQL (Supabase) só foi feita depois que o modelo de domínio já estava estável — trocando as implementações de repositório sem tocar nas camadas de serviço e controller, graças ao isolamento por interfaces.

**JWT stateless em vez de sessão.** Como o consumidor principal é um cliente mobile (Android) que pode ter conexões instáveis e múltiplos dispositivos, sessão fixada em servidor adicionaria complexidade de escalabilidade sem benefício real. JWT permite validar a identidade a cada requisição sem estado compartilhado, o que também simplifica o deploy horizontal.

**Isolamento do domínio TMDB.** Os DTOs do TMDB (`TmdbMovieResponse`, `TmdbSearchResponse`) nunca chegam ao cliente. Existe uma camada de mapeamento (`Movie` → `MovieResponse`) que blinda a API contra mudanças no contrato externo — se o TMDB mudar um campo, o impacto fica isolado no `TmdbClientImpl` e no mapper, sem vazar para os consumidores da API.

**Compartilhamento como fluxo de estado, não notificação simples.** Um `Share` tem um ciclo de vida (`PENDING` → `ACCEPTED`/`REJECTED`), o que permite ao destinatário decidir se aquele filme entra ou não na sua watchlist, em vez de forçar a recomendação.

---

## ARQUITETURA DE SOFTWARE

O projeto segue uma **Arquitetura em Camadas (Layered Architecture)** organizada por módulos de negócio (*movie*, *watchlist*, *share*, *user*), alinhada aos princípios da *Clean Architecture*, garantindo baixo acoplamento, alta coesão e independência da API externa.

```
┌───────────────────────────────────────────────────────────────────┐
│                       Camada Mobile / Client                      │
│               (WatchuSee Android - Kotlin / Compose)              │
└───────────────────────────┬───────────────────────────────────────┘
                            │ HTTP / JSON (Authorization: Bearer <jwt>)
                            ▼
┌───────────────────────────────────────────────────────────────────┐
│                     Camada de Apresentação                        │
│     (Controllers REST, JwtAuthenticationFilter, Swagger/OpenAPI)  │
└───────────────────────────┬───────────────────────────────────────┘
                            │ DTOs / Interfaces
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│                       Camada de Negócio                          │
│   (Services, Domain Entities, Business Exceptions, JwtService)   │
└──────────────┬──────────────────────────────────┬────────────────┘
               │                                  │
               ▼                                  ▼
┌───────────────────────────────┐   ┌───────────────────────────────┐
│      Camada de Persistência   │   │   Camada de Integração Externa│
│  (Spring Data JPA + Postgres) │   │  (TmdbClient, RestClient)     │
└───────────────────────────────┘   └───────────────┬───────────────┘
                                                    │ HTTP / JSON
                                                    ▼
                                     ┌──────────────────────────────┐
                                     │            TMDB API          │
                                     └──────────────────────────────┘
```

### Isolamento de Modelo de Domínio

O sistema **não expõe os DTOs do TMDB** para as camadas superiores. O fluxo de dados de filmes preserva a integridade do domínio interno através da seguinte pipeline de transformação:

```
TMDB Response → TmdbMovieResponse (DTO) → Movie (Domain Model) → MovieResponse (Client JSON)
```

### Módulos de Negócio

| Módulo       | Responsabilidade                                                         |
| ------------ | ------------------------------------------------------------------------ |
| `user`       | Cadastro, autenticação (login) e perfil do usuário.                      |
| `movie`      | Busca, detalhamento e descoberta de filmes via integração com o TMDB.    |
| `watchlist`  | Gerenciamento da lista pessoal de filmes (`TO_WATCH` / `WATCHED`).       |
| `share`      | Compartilhamento de filmes entre usuários (envio, aceite, recusa).       |
| `shared`     | Infraestrutura transversal: segurança (JWT), tratamento global de erros. |

---

## TECNOLOGIAS E FERRAMENTAS

### Core & Frameworks

- **Java 21:** Uso de *Records*, *Pattern Matching* e melhorias de concorrência.
- **Spring Boot 4:** Framework base da aplicação.
- **Spring Web MVC:** Construção de rotas RESTful.
- **Spring Validation:** Validação declarativa via anotações `@Valid`.
- **Spring Data JPA / Hibernate:** Persistência relacional dos dados da aplicação.
- **PostgreSQL (Supabase):** Banco de dados relacional em produção.
- **Spring Security:** Controle de acesso e filtro de autenticação stateless.
- **JJWT (io.jsonwebtoken):** Geração e validação de tokens JWT.
- **RestClient:** Cliente HTTP moderno e síncrono do Spring para consumo do TMDB.
- **Spring Boot Actuator:** Exposição do endpoint de *health check*.
- **Spring Cache:** Camada de cache para otimizar chamadas repetidas.

### Documentação & Infraestrutura

- **OpenAPI 3 / Swagger UI (springdoc):** Geração automática da documentação da API.
- **Maven:** Gerenciamento de dependências e build pipeline.
- **Docker:** Build multi-stage (Maven → JRE Alpine) para imagem de produção.
- **Render:** Ambiente de deploy da API (com plano gratuito sujeito a cold start).

### Testes

- **JUnit 5 & Spring Boot Test:** Suporte a testes de contexto da aplicação.
- **MockWebServer (OkHttp):** Disponível para simulação de chamadas HTTP ao TMDB sem consumir a API externa real.

> A suíte de testes está em reconstrução após a migração para persistência relacional e autenticação JWT — atualmente cobre apenas o carregamento do contexto Spring (`WatchuseeApplicationTests`). A cobertura por camada (controllers, services, client TMDB) faz parte do roadmap de curto prazo.

---

## ENDPOINTS DA API

Todos os endpoints estão sob o prefixo `/api/v1`. Endpoints marcados com 🔒 exigem o header `Authorization: Bearer <token>`.

### Autenticação (*Auth*)

| Método | Endpoint          | Descrição                          | Acesso   |
| ------ | ------------------ | ----------------------------------- | -------- |
| `POST` | `/auth/login`       | Autentica um usuário e retorna JWT  | Público  |

### Usuários (*Users*)

| Método | Endpoint              | Descrição                                        | Acesso   |
| ------ | ---------------------- | ------------------------------------------------- | -------- |
| `POST` | `/users`               | Cadastra um novo usuário                          | Público  |
| `GET`  | `/users/me/profile`    | Consulta o perfil do usuário autenticado          | 🔒       |

### Filmes (*Movies*)

| Método | Endpoint                       | Descrição                                     | Parâmetros                    |
| ------ | ------------------------------- | ----------------------------------------------- | ------------------------------ |
| `GET`  | `/movies/search`                | Pesquisa filmes por título no TMDB             | `query` (String, obrigatório) |
| `GET`  | `/movies/{movieId}`             | Detalhes de um filme                            | `movieId` (Long, path)        |
| `GET`  | `/movies/{movieId}/trailer`     | Trailer principal do filme                      | `movieId` (Long, path)        |
| `GET`  | `/movies/{movieId}/similar`     | Filmes similares ao informado                   | `movieId` (Long, path)        |
| `GET`  | `/movies/top-rated`             | Filmes mais bem avaliados (paginado)            | `page` (int, opcional, padrão `1`) |
| `GET`  | `/movies/trending/week`         | Filmes em alta na semana                        | —                               |
| `GET`  | `/movies/trending/random`       | Filme aleatório dentre os em alta na semana     | —                               |

### Watchlist 🔒

| Método   | Endpoint                             | Descrição                                 |
| -------- | -------------------------------------- | ------------------------------------------ |
| `POST`   | `/watchlist/to-watch/{movieId}`        | Adiciona filme à lista "Para Assistir"     |
| `DELETE` | `/watchlist/to-watch/{movieId}`        | Remove filme da lista "Para Assistir"      |
| `GET`    | `/watchlist/to-watch`                  | Lista todos os filmes pendentes            |
| `POST`   | `/watchlist/watched/{movieId}`         | Marca o filme como "Assistido"             |
| `DELETE` | `/watchlist/watched/{movieId}`         | Remove o filme da lista de "Assistidos"    |
| `GET`    | `/watchlist/watched`                   | Lista todos os filmes assistidos           |
| `GET`    | `/watchlist/{movieId}/status`          | Retorna o status atual do filme no acervo  |

### Compartilhamento (*Shares*) 🔒

| Método  | Endpoint                | Descrição                                       |
| ------- | ------------------------- | -------------------------------------------------- |
| `POST`  | `/shares`                 | Compartilha um filme com outro usuário (por nick)  |
| `GET`   | `/shares/received`        | Lista compartilhamentos recebidos                  |
| `GET`   | `/shares/pending`         | Lista compartilhamentos pendentes de resposta      |
| `GET`   | `/shares/sent`            | Lista compartilhamentos enviados                   |
| `PATCH` | `/shares/{shareId}/accept`| Aceita um compartilhamento recebido                |
| `PATCH` | `/shares/{shareId}/reject`| Recusa um compartilhamento recebido                |

---

## EXEMPLOS DE USO

Exemplos prontos para copiar e colar (usando a API em produção; troque a URL base por `http://localhost:8080` para rodar localmente).

**1. Cadastrar um usuário**

```bash
curl -X POST https://watchusee-backend.onrender.com/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"nick": "lucas", "password": "senha123"}'
```

**2. Fazer login e obter o token**

```bash
curl -X POST https://watchusee-backend.onrender.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"nick": "lucas", "password": "senha123"}'

# Resposta:
# { "id": 1, "nick": "lucas", "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

**3. Buscar filmes no TMDB**

```bash
curl "https://watchusee-backend.onrender.com/api/v1/movies/search?query=Batman"
```

**4. Adicionar um filme à watchlist (rota protegida)**

```bash
curl -X POST https://watchusee-backend.onrender.com/api/v1/watchlist/to-watch/272 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**5. Compartilhar um filme com outro usuário**

```bash
curl -X POST https://watchusee-backend.onrender.com/api/v1/shares \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"movieId": 272, "recipientNick": "amigo", "message": "Você precisa ver isso"}'
```

Para o catálogo completo de endpoints com schemas e exemplos de resposta, use o [Swagger UI](https://watchusee-backend.onrender.com/swagger-ui/index.html).

---

## AUTENTICAÇÃO

A API utiliza autenticação **stateless** baseada em **JWT**:

1. Crie uma conta em `POST /api/v1/users` informando `nick` e `password`.
2. Faça login em `POST /api/v1/auth/login` para receber o token.
3. Envie o token nas requisições protegidas através do header:

```
Authorization: Bearer <token>
```

Os endpoints de leitura de filmes (`GET /api/v1/movies/**`), cadastro, login, Swagger UI e o *health check* do Actuator são públicos; todos os demais exigem autenticação.

---

## TRATAMENTO DE ERROS E VALIDAÇÃO

A API implementa um manipulador global de exceções via `@RestControllerAdvice`. Respostas de erro retornam um payload JSON consistente em todas as falhas operacionais ou de validação:

```json
{
  "timestamp": "2026-08-28T03:45:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Nick ou senha inválidos.",
  "path": "/api/v1/auth/login"
}
```

Exemplo de resposta para validação de entrada inválida (`POST /api/v1/users` com senha curta):

```json
{
  "timestamp": "2026-08-28T03:40:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "A senha deve possuir entre 6 e 100 caracteres.",
  "path": "/api/v1/users"
}
```

---

## ESTRUTURA DO PROJETO

<details>
<summary>Ver árvore completa de diretórios</summary>

```
watchusee-backend/
├── src/
│   ├── main/
│   │   ├── java/br/com/watchusee/watchusee/
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   ├── PasswordEncoderConfig.java
│   │   │   │   ├── RestClientConfig.java
│   │   │   │   └── TmdbProperties.java
│   │   │   │
│   │   │   ├── movie/
│   │   │   │   ├── api/
│   │   │   │   │   ├── MovieController.java
│   │   │   │   │   └── dto/
│   │   │   │   ├── client/tmdb/
│   │   │   │   │   ├── TmdbClient.java
│   │   │   │   │   ├── TmdbClientImpl.java
│   │   │   │   │   ├── dto/
│   │   │   │   │   └── exception/
│   │   │   │   ├── domain/Movie.java
│   │   │   │   ├── dto/MovieTrailerResponse.java
│   │   │   │   ├── mapper/
│   │   │   │   ├── repository/MovieRepository.java
│   │   │   │   └── service/MovieService.java
│   │   │   │
│   │   │   ├── watchlist/
│   │   │   │   ├── api/
│   │   │   │   │   ├── WatchlistController.java
│   │   │   │   │   └── dto/
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Watchlist.java
│   │   │   │   │   └── WatchlistStatus.java
│   │   │   │   ├── exception/WatchlistItemNotFoundException.java
│   │   │   │   ├── repository/WatchlistRepository.java
│   │   │   │   └── service/WatchlistService.java
│   │   │   │
│   │   │   ├── share/
│   │   │   │   ├── api/dto/
│   │   │   │   ├── controller/ShareController.java
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Share.java
│   │   │   │   │   └── ShareStatus.java
│   │   │   │   ├── exception/ShareRecipientNotFoundException.java
│   │   │   │   ├── repository/ShareRepository.java
│   │   │   │   └── service/ShareService.java
│   │   │   │
│   │   │   ├── user/
│   │   │   │   ├── api/dto/
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   └── UserProfileController.java
│   │   │   │   ├── domain/User.java
│   │   │   │   ├── exception/
│   │   │   │   ├── repository/UserRepository.java
│   │   │   │   └── service/
│   │   │   │       ├── AuthService.java
│   │   │   │       ├── UserProfileService.java
│   │   │   │       └── UserService.java
│   │   │   │
│   │   │   ├── shared/
│   │   │   │   ├── api/
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   └── dto/ErrorResponse.java
│   │   │   │   └── security/
│   │   │   │       ├── AuthenticatedUser.java
│   │   │   │       ├── JwtAuthenticationFilter.java
│   │   │   │       ├── JwtService.java
│   │   │   │       └── SecurityConfig.java
│   │   │   │
│   │   │   └── WatchuseeApplication.java
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│       └── java/br/com/watchusee/watchusee/
│           └── WatchuseeApplicationTests.java
│
├── .mvn/wrapper/
├── Dockerfile
├── pom.xml
├── mvnw / mvnw.cmd
└── README.md
```

</details>

---

## CONFIGURAÇÃO E EXECUÇÃO LOCAL

### Pré-requisitos

- **Java 21** ou superior instalado.
- **PostgreSQL** acessível (local ou uma instância [Supabase](https://supabase.com/) gratuita).
- **TMDB API Key** (obtenha gratuitamente no portal do desenvolvedor do [TMDB](https://www.themoviedb.org/)).

### Passos para Execução

1. **Clone o repositório:**

```bash
git clone https://github.com/jzmlucas/watchusee-backend.git
cd watchusee-backend
```

2. **Configure as variáveis de ambiente** necessárias para a integração com o TMDB, o banco de dados e a autenticação:

```bash
export TMDB_API_KEY="sua_chave"

export SUPABASE_URL="jdbc:postgresql://<host>:<porta>/<database>"
export SUPABASE_USERNAME="seu_usuario"
export SUPABASE_PASSWORD="sua_senha"

export JWT_SECRET="sua_chave"
```

3. **Inicie a aplicação via Maven Wrapper:**

```bash
./mvnw spring-boot:run
```

4. **Acesse a documentação da API.** Com a aplicação em execução:

  - **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
  - **OpenAPI Specs:** `http://localhost:8080/v3/api-docs`
  - **Health Check:** `http://localhost:8080/actuator/health`

### Executando com Docker

```bash
docker build -t watchusee-backend .
docker run -p 8080:8080 \
  -e TMDB_API_KEY="sua_chave" \
  -e SUPABASE_URL="jdbc:postgresql://<host>:<porta>/<database>" \
  -e SUPABASE_USERNAME="seu_usuario" \
  -e SUPABASE_PASSWORD="sua_senha" \
  -e JWT_SECRET="sua_chave" \
  watchusee-backend
```

### Executando os testes

```bash
./mvnw clean test
```

---

## ROADMAP DE EVOLUÇÃO

- [x] **MVP:** Busca, detalhes e manipulação da watchlist em memória.
- [x] **Persistência:** Migração para Spring Data JPA + PostgreSQL (Supabase).
- [x] **Segurança:** Autenticação stateless via Spring Security + JWT.
- [x] **Compartilhamento Social:** Envio, aceite e recusa de filmes entre usuários.
- [x] **Descoberta Avançada:** Tendências, mais bem avaliados, similares e trailers.
- [x] **Integração Mobile:** Cliente Android nativo em Kotlin e Jetpack Compose ([watchusee-android](https://github.com/jzmlucas/watchusee-android)).
- [x] **Infraestrutura:** Containerização via Docker e deploy contínuo no Render.
- [ ] **Cobertura de Testes:** Reconstrução da suíte de testes unitários e de integração após a migração de persistência.
- [ ] **Multi-tenancy avançado:** Papéis e permissões diferenciadas entre usuários.
- [ ] **Notificações:** Alertas de novos compartilhamentos recebidos.
- [ ] **Website:** Além de um aplicativo Android, o usuário também pode acessar via website com dados integrados.

---

## AUTOR

Desenvolvido por **Lucas Joly**.

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://linkedin.com/in/jzmlucas)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/jzmlucas)
