# Referência de Endpoints

Referência completa de todos os endpoints REST expostos pela API, extraída diretamente dos controllers e DTOs atuais do código-fonte (não do Swagger nem de versões anteriores da documentação). Para arquitetura, entidades, segurança e débito técnico, ver **[ARCHITECTURE.md](ARCHITECTURE.md)**.

## Convenções

- **Base URL**: `/api/v1`
- **Autenticação**: header `Authorization: Bearer <token>` — 🔒 na tabela indica que o endpoint exige autenticação. Endpoints sem 🔒 são públicos.
- **Formato de erro** padrão (`ErrorResponse`), retornado pelo `GlobalExceptionHandler` em qualquer falha:
  ```json
  {
    "timestamp": "2026-08-28T03:45:00Z",
    "status": 401,
    "error": "Unauthorized",
    "message": "Nick ou senha inválidos.",
    "path": "/api/v1/auth/login"
  }
  ```
- **Paginação**: onde houver, os parâmetros de query são `page` (0-based, exceto nos endpoints de filmes, que usam `page` 1-based vindo direto do TMDB) e `size`. A resposta paginada segue o formato:
  ```json
  {
    "content": [ /* itens */ ],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3,
    "first": true,
    "last": false
  }
  ```
- Status de sucesso sem corpo de resposta usam **204 No Content**. Criação de recurso usa **201 Created**.

---

## Índice

* [Autenticação](#autenticação)
* [Usuários](#usuários)
* [Perfil](#perfil)
* [Filmes](#filmes)
* [Watchlist](#watchlist)
* [Watchlist de outro usuário](#watchlist-de-outro-usuário)
* [Amigos](#amigos)
* [Compartilhamentos (Shares)](#compartilhamentos-shares)

---

## Autenticação

### `POST /auth/login`

Autentica um usuário por nick e senha.

- **Auth**: pública.
- **Body** (`LoginRequest`):

  | Campo | Tipo | Validação |
    |---|---|---|
  | `nick` | string | obrigatório, 3–30 caracteres |
  | `password` | string | obrigatório, 6–100 caracteres |

- **200 OK** (`LoginResponse`):
  ```json
  { "id": 1, "nick": "lucas", "token": "eyJhbGciOiJIUzI1NiJ9..." }
  ```
- **400** dados inválidos · **401** nick ou senha incorretos · **423** conta bloqueada (`AccountLockedException`, após `security.login.max-attempts` tentativas inválidas).

### `POST /auth/logout` 🔒

Revoga o token JWT atual (via blacklist em memória), impedindo seu reuso mesmo antes da expiração natural.

- **204 No Content** em sucesso (mesmo se o header `Authorization` não trouxer um token válido — o endpoint não falha nesse caso, apenas não revoga nada).
- **401** token ausente ou inválido (barrado pelo `JwtAuthenticationFilter`/`anyRequest().authenticated()` antes de chegar no controller).

---

## Usuários

### `POST /users`

Cadastra um novo usuário.

- **Auth**: pública.
- **Body** (`CreateUserRequest`):

  | Campo | Tipo | Validação |
    |---|---|---|
  | `nick` | string | obrigatório, 3–30 caracteres, apenas `a-zA-Z0-9_.` |
  | `password` | string | obrigatório, 6–100 caracteres, ao menos 1 letra e 1 número |

- **201 Created** (`UserResponse`): `{ "id": 1, "nick": "lucas" }`
- **400** dados inválidos · **409** nick já cadastrado.

### `GET /users/search?query=` 🔒

Busca usuários por nick (substring, case-insensitive).

- **Query**: `query` (string, obrigatório, até 30 caracteres).
- **200 OK** (`List<UserSearchResponse>`, máx. 20 itens, exclui o próprio usuário autenticado):
  ```json
  [ { "id": 7, "nick": "maria" } ]
  ```
- **400** query inválida · **401** não autenticado.

### `PUT /users/me/password` 🔒

Troca a senha do usuário autenticado e invalida todas as sessões emitidas antes deste momento (`tokensValidAfter`).

- **Body** (`ChangePasswordRequest`):

  | Campo | Tipo | Validação |
    |---|---|---|
  | `currentPassword` | string | obrigatório, 6–100 caracteres |
  | `newPassword` | string | obrigatório, 8–100 caracteres, ao menos 1 letra e 1 número |

- **204 No Content** em sucesso.
- **400** dados inválidos · **401** senha atual incorreta ou não autenticado · **404** usuário não encontrado.

### `GET /users/avatar-icons` 🔒

Lista o conjunto fixo de ícones disponíveis como avatar (não há upload de imagem).

- **200 OK** (`List<AvatarIconResponse>`):
  ```json
  [ { "value": "POPCORN", "label": "Popcorn" } ]
  ```
- **401** não autenticado.

### `PUT /users/me/avatar` 🔒

Define o ícone de avatar do usuário autenticado.

- **Body** (`UpdateAvatarIconRequest`): `{ "icon": "POPCORN" }` — `icon` obrigatório, deve ser um dos valores de `AvatarIcon`.
- **204 No Content** · **400** ícone inválido · **401** não autenticado.

### `PUT /users/me/favorite-movie` 🔒

Define o filme favorito do usuário autenticado. Se o filme ainda não existir localmente, é buscado e persistido a partir do TMDB (`findOrCreateMovie`).

- **Body** (`UpdateFavoriteMovieRequest`): `{ "movieId": 414906 }` — `movieId` obrigatório e positivo.
- **204 No Content** · **400** ID inválido · **401** não autenticado · **404** filme não encontrado no TMDB.

### `DELETE /users/me/favorite-movie` 🔒

Remove o filme favorito do usuário autenticado.

- **204 No Content** · **401** não autenticado.

---

## Perfil

### `GET /users/me/profile` 🔒

Perfil do usuário autenticado.

- **200 OK** (`UserProfileResponse`):
  ```json
  {
    "id": 1,
    "nick": "lucas",
    "createdAt": "2026-01-10T12:00:00Z",
    "watchedMovies": 12,
    "toWatchMovies": 5,
    "friendsCount": 3,
    "avatarIcon": "POPCORN",
    "favoriteMovie": { "id": 414906, "title": "The Batman", "posterPath": "/74xTEgt7R36Fpooo50r9T25onhq.jpg" }
  }
  ```
  `favoriteMovie` é `null` se não houver filme favorito definido.
- **401** não autenticado · **404** usuário não encontrado.

### `GET /users/{userId}/profile` 🔒

Perfil público de qualquer usuário cadastrado. Não exige amizade — pensado para o fluxo "buscar usuário → ver perfil → enviar solicitação de amizade".

- **Path**: `userId` (long, positivo).
- **200 OK**: mesmo formato de `UserProfileResponse` acima.
- **401** não autenticado · **404** usuário não encontrado.

---

## Filmes

Todos os endpoints deste grupo são **públicos** (sujeitos ao rate limit de `public-api`, padrão 60 req/min por IP). Dados vêm ao vivo do TMDB — não há cache.

### `GET /movies/search`

- **Query**: `query` (string, obrigatório, 2–100 caracteres).
- **200 OK**: `List<MovieResponse>`.
- **400** termo inválido · **502** erro na comunicação com o TMDB.

### `GET /movies/trending/week`

Filmes em alta na semana (TMDB `/trending/movie/week`).

- **200 OK**: `List<MovieResponse>` · **502** erro TMDB.

### `GET /movies/trending/random`

Um filme aleatório dentre os atualmente em tendência.

- **200 OK**: `MovieResponse` (objeto único) · **502** erro TMDB.

### `GET /movies/popular`, `/movies/now-playing`, `/movies/upcoming`, `/movies/top-rated`

Listas paginadas espelhando os respectivos endpoints do TMDB.

- **Query**: `page` (int, opcional, default `1`, mín. `1`, máx. `1000`).
- **200 OK** (`MoviePageResponse`):
  ```json
  { "page": 1, "totalPages": 500, "totalResults": 10000, "results": [ /* MovieResponse[] */ ] }
  ```
- **502** erro TMDB.
- ⚠️ **`/movies/upcoming` tem um bug conhecido**: o parâmetro `page` não é repassado ao TMDB pelo `TmdbClientImpl.getUpcomingMovies` — a resposta sempre traz a página 1 do TMDB, independentemente do `page` enviado. Ver [ARCHITECTURE.md — Débito técnico](ARCHITECTURE.md#débito-técnico-e-bugs-conhecidos).

### `GET /movies/{movieId}`

Detalhes completos de um filme.

- **Path**: `movieId` (long, positivo).
- **200 OK**: `MovieDetailsResponse` (título, título original, sinopse, tagline, data/ano de lançamento, duração, idioma original, idiomas falados, gêneros, nota, votos, popularidade, status, pôster, backdrop, homepage, orçamento, receita, produtoras e países de produção).
- **400** ID inválido · **404** filme não encontrado no TMDB · **502** erro TMDB.

### `GET /movies/{movieId}/similar`

- **Path**: `movieId`. **Query**: `page` (opcional, default `1`).
- **200 OK**: `MoviePageResponse`.

### `GET /movies/{movieId}/recommendations`

- **Path**: `movieId`. **Query**: `page` (opcional, default `1`).
- **200 OK**: `MoviePageResponse`.

### `GET /movies/{movieId}/reviews`

- **Path**: `movieId`. **Query**: `page` (opcional, default `1`).
- **200 OK** (`MovieReviewsResponse`): página de avaliações (`id`, `author`, `username`, `avatarPath`, `rating`, `content`, `createdAt`, `updatedAt`, `url` por item).

### `GET /movies/{movieId}/lists`

- **Path**: `movieId`. **Query**: `page` (opcional, default `1`).
- **200 OK** (`MovieListResponse`): listas públicas do TMDB que contêm o filme.

### `GET /movies/{movieId}/trailer`

Trailer principal do filme.

- **Path**: `movieId`.
- **200 OK** (`MovieTrailerResponse`): `{ "key": "...", "name": "...", "site": "YouTube", "type": "Trailer" }`
- **204 No Content** se não houver trailer disponível.
- **404** filme não encontrado · **502** erro TMDB.

---

## Watchlist

Todos exigem autenticação. Operam sempre sobre a watchlist do **próprio** usuário autenticado (`userId` nunca vem de parâmetro do cliente).

### `GET /watchlist` 🔒

- **Query**: `status` (opcional, `TO_WATCH` ou `WATCHED`), `page` (default `0`), `size` (default `20`, máx. `100`).
- Ordenado por `createdAt` desc.
- **200 OK**: página de `WatchlistResponse` (`{ movie: MovieResponse, status, createdAt }`).

### `GET /watchlist/{movieId}` 🔒

- **Path**: `movieId` (positivo).
- **200 OK**: `WatchlistResponse`.
- **400** filme não está na watchlist do usuário (ver nota de débito técnico: deveria ser 404 — `WatchlistItemNotFoundException` existe mas nunca é lançada).

### `PUT /watchlist/{movieId}` 🔒

Cria ou atualiza (upsert) o status do filme na watchlist. Se o filme ainda não existir localmente, é buscado e persistido a partir do TMDB.

- **Path**: `movieId` (positivo). **Body** (`UpdateWatchlistRequest`): `{ "status": "TO_WATCH" }` ou `"WATCHED"` — obrigatório.
- **200 OK**: `WatchlistResponse` atualizado.
- **400** status inválido ou filme não encontrado no TMDB.

### `DELETE /watchlist/{movieId}` 🔒

- **Path**: `movieId` (positivo).
- **204 No Content**.

---

## Watchlist de outro usuário

### `GET /users/{userId}/watchlist` 🔒

- **Path**: `userId` (positivo). **Query**: `status` (opcional), `page` (default `0`), `size` (default `20`, máx. `100`).
- **Regra de acesso**: só é permitido consultar a **própria** watchlist ou a de um usuário com quem exista **amizade aceita**. Caso contrário, `WatchlistService.findAllForViewer` lança `AccessDeniedException` → **403 Forbidden**.
- **200 OK**: página de `WatchlistResponse`, mesmo formato do endpoint acima.
- **403** sem amizade com o usuário-alvo · **404** usuário-alvo não encontrado.

---

## Amigos

Todos exigem autenticação (regra implícita via `anyRequest().authenticated()` — não há matcher explícito para `/friends/**`).

### `POST /friends/requests/{userId}` 🔒

Envia uma solicitação de amizade.

- **Path**: `userId` (positivo, destinatário).
- **201 Created** (`FriendRequestResponse`): `{ "id": 10, "userId": 7, "nick": "maria", "requestedAt": "..." }`
- **400** `userId` inválido ou é o próprio usuário · **404** destinatário não encontrado · **409** já existe uma relação entre os dois usuários (em qualquer status).

### `GET /friends/requests` 🔒

Lista solicitações **recebidas** e pendentes, paginado.

- **Query**: `page` (default `0`), `size` (default `20`, máx. `100`). Ordenado por `createdAt` desc.
- **200 OK**: página de `FriendRequestResponse`.

### `GET /friends/requests/sent` 🔒

Lista solicitações **enviadas** e pendentes, paginado (mesmo formato/paginação do endpoint acima).

### `POST /friends/requests/{requestId}/accept` 🔒

Aceita uma solicitação recebida. Só o destinatário pode aceitar.

- **Path**: `requestId` (positivo).
- **204 No Content** · **403** usuário autenticado não é o destinatário · **404** solicitação não encontrada · **409** solicitação não está mais `PENDING`.

### `POST /friends/requests/{requestId}/reject` 🔒

Recusa uma solicitação recebida (mesma regra de dono do endpoint acima).

- **204 No Content** · **403** · **404** · **409**.

### `POST /friends/requests/{requestId}/cancel` 🔒

Cancela uma solicitação que o próprio usuário enviou (antes de ser aceita ou recusada). Só quem enviou pode cancelar.

- **Path**: `requestId` (positivo).
- **204 No Content** · **403** usuário autenticado não é quem enviou · **404** solicitação não encontrada · **409** solicitação não está mais `PENDING`.

### `DELETE /friends/{userId}` 🔒

Desfaz uma amizade já existente (remove um amigo).

- **Path**: `userId` (positivo, do amigo a remover).
- **204 No Content** · **404** não existe amizade `ACCEPTED` entre os dois usuários.

### `GET /friends` 🔒

Lista amigos (relações `ACCEPTED`, em qualquer direção), paginado, ordenado por `respondedAt` desc.

- **Query**: `page` (default `0`), `size` (default `20`, máx. `100`).
- **200 OK**: página de `FriendResponse`: `{ "id": 7, "nick": "maria", "friendsSince": "..." }`

### `GET /friends/count` 🔒

- **200 OK**: número inteiro (`long`) — quantidade de amigos.

### `GET /friends/status/{userId}` 🔒

Relacionamento entre o usuário autenticado e o usuário informado — pensado para decidir qual botão mostrar na tela de perfil público.

- **Path**: `userId` (positivo).
- **200 OK** (`FriendshipStatusResponse`): `{ "userId": 7, "status": "REQUEST_RECEIVED", "friendshipId": 10 }`
- `status` é um de: `SELF`, `NONE`, `FRIENDS`, `REQUEST_SENT`, `REQUEST_RECEIVED`, `REJECTED`, `CANCELLED`.
    - Quando `REQUEST_RECEIVED`, `friendshipId` pode ser usado direto em `.../accept` ou `.../reject`.
    - Quando `REQUEST_SENT`, o mesmo id serve para `.../cancel`.
- **401** não autenticado · **404** usuário não encontrado.

---

## Compartilhamentos (Shares)

Todos exigem autenticação (mesma observação de `/friends/**`: sem matcher explícito, cai em `anyRequest().authenticated()`).

### `POST /shares` 🔒

Compartilha um filme com outro usuário, identificado por nick.

- **Body** (`CreateShareRequest`):

  | Campo | Tipo | Validação |
    |---|---|---|
  | `movieId` | long | obrigatório, positivo |
  | `recipientNick` | string | obrigatório, 3–30 caracteres |
  | `message` | string | opcional, até 500 caracteres |

- **201 Created** (`ShareResponse`):
  ```json
  {
    "id": 5, "movieId": 414906,
    "senderId": 1, "senderNick": "lucas",
    "recipientId": 7, "recipientNick": "maria",
    "message": "bora ver esse!", "status": "PENDING", "createdAt": "..."
  }
  ```
- **400** já existe um share `PENDING` idêntico (via `IllegalArgumentException`) · **404** `recipientNick` não existe (`ShareRecipientNotFoundException`).

### `GET /shares/received` 🔒

Todos os shares recebidos, qualquer status.

- **200 OK**: `List<ShareResponse>`.

### `GET /shares/pending` 🔒

Apenas os shares recebidos com status `PENDING`.

- **200 OK**: `List<ShareResponse>`.

### `GET /shares/sent` 🔒

Todos os shares enviados pelo usuário autenticado.

- **200 OK**: `List<ShareResponse>`.

### `PATCH /shares/{shareId}/accept` 🔒

Aceita um share recebido. Só o destinatário pode aceitar, e só se ainda estiver `PENDING`.

- **Path**: `shareId` (positivo).
- **200 OK**: `ShareResponse` atualizado.
- **404** share não encontrado ou não pertence ao destinatário autenticado (`findByIdAndRecipientId` não diferencia os dois casos).

### `PATCH /shares/{shareId}/reject` 🔒

Recusa um share recebido (mesma regra de dono e status do endpoint acima).

- **200 OK**: `ShareResponse` atualizado · **404** mesma observação acima.