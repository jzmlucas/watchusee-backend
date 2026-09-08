package br.com.watchusee.watchusee.movie.api;

import br.com.watchusee.watchusee.movie.api.dto.MovieDetailsResponse;
import br.com.watchusee.watchusee.movie.api.dto.MovieListResponse;
import br.com.watchusee.watchusee.movie.api.dto.MoviePageResponse;
import br.com.watchusee.watchusee.movie.api.dto.MovieResponse;
import br.com.watchusee.watchusee.movie.api.dto.MovieReviewsResponse;
import br.com.watchusee.watchusee.movie.domain.Movie;
import br.com.watchusee.watchusee.movie.dto.MoviePageResult;
import br.com.watchusee.watchusee.movie.dto.MovieTrailerResponse;
import br.com.watchusee.watchusee.movie.mapper.MovieResponseMapper;
import br.com.watchusee.watchusee.movie.service.MovieService;
import br.com.watchusee.watchusee.shared.api.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/movies")
@Tag(
        name = "Movies",
        description = "Operações de consulta, pesquisa e descoberta de filmes."
)
public class MovieController {

    private static final int DEFAULT_PAGE = 1;
    private static final int MAX_PAGE = 1000;

    private final MovieService movieService;
    private final MovieResponseMapper movieResponseMapper;

    public MovieController(
            MovieService movieService,
            MovieResponseMapper movieResponseMapper
    ) {
        this.movieService = movieService;
        this.movieResponseMapper = movieResponseMapper;
    }

    @GetMapping("/search")
    @Operation(
            summary = "Pesquisar filmes",
            description = """
                    Pesquisa filmes utilizando um termo informado pelo cliente.

                    A busca é realizada através da API do TMDB.
                    O resultado contém os filmes encontrados pelo serviço externo.

                    O termo deve possuir entre 2 e 100 caracteres.
                    """,
            operationId = "searchMovies"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Filmes encontrados com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = MovieResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            [
                                              {
                                                "id": 414906,
                                                "title": "The Batman",
                                                "overview": "In his second year of fighting crime, Batman uncovers corruption in Gotham City.",
                                                "releaseDate": "2022-03-01",
                                                "posterPath": "/74xTEgt7R36Fpooo50r9T25onhq.jpg",
                                                "rating": 7.668
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Termo de busca inválido.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Erro na comunicação com o TMDB.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    public List<MovieResponse> searchMovies(
            @Parameter(
                    description = "Título ou termo utilizado na pesquisa.",
                    example = "Batman",
                    required = true
            )
            @RequestParam
            @NotBlank(message = "O termo de busca é obrigatório.")
            @Size(
                    min = 2,
                    max = 100,
                    message = "O termo de busca deve possuir entre 2 e 100 caracteres."
            )
            String query
    ) {
        List<Movie> movies = movieService.searchMovies(query);

        return movies.stream()
                .map(movieResponseMapper::toResponse)
                .toList();
    }

    @GetMapping("/trending/random")
    @Operation(
            summary = "Obter filme aleatório em tendência",
            description = """
                    Retorna aleatoriamente um filme atualmente em tendência
                    no TMDB.
                    """,
            operationId = "getRandomTrendingMovie"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Filme encontrado com sucesso."
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Erro na comunicação com o TMDB.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    public MovieResponse getRandomTrendingMovie() {
        Movie movie = movieService.getRandomTrendingMovie();

        return movieResponseMapper.toResponse(movie);
    }

    @GetMapping("/trending/week")
    @Operation(
            summary = "Consultar filmes em alta da semana",
            description = """
                    Retorna os filmes que estão em alta no TMDB
                    durante a semana atual.
                    """,
            operationId = "getTrendingMovies"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Filmes em alta retornados com sucesso."
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Erro na comunicação com o TMDB.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    public List<MovieResponse> getTrendingMovies() {
        List<Movie> movies = movieService.getTrendingMovies();

        return movies.stream()
                .map(movieResponseMapper::toResponse)
                .toList();
    }

    @GetMapping("/popular")
    @Operation(
            summary = "Obter filmes populares",
            description = """
                    Retorna os filmes atualmente mais populares
                    de acordo com o TMDB.

                    Os resultados são paginados.
                    """,
            operationId = "getPopularMovies"
    )
    public MoviePageResponse getPopularMovies(
            @Parameter(
                    description = "Número da página de resultados.",
                    example = "1"
            )
            @RequestParam(defaultValue = "1")
            @Min(1)
            @Max(MAX_PAGE)
            int page
    ) {
        MoviePageResult result = movieService.getPopularMovies(page);

        return toMoviePageResponse(result);
    }

    @GetMapping("/now-playing")
    @Operation(
            summary = "Obter filmes em cartaz",
            description = """
                    Retorna filmes atualmente em cartaz nos cinemas,
                    segundo o TMDB.

                    Os resultados são paginados.
                    """,
            operationId = "getNowPlayingMovies"
    )
    public MoviePageResponse getNowPlayingMovies(
            @Parameter(
                    description = "Número da página de resultados.",
                    example = "1"
            )
            @RequestParam(defaultValue = "1")
            @Min(1)
            @Max(MAX_PAGE)
            int page
    ) {
        MoviePageResult result = movieService.getNowPlayingMovies(page);

        return toMoviePageResponse(result);
    }

    @GetMapping("/upcoming")
    @Operation(
            summary = "Obter próximos lançamentos",
            description = """
                    Retorna filmes com lançamento previsto
                    de acordo com o TMDB.

                    Os resultados são paginados.
                    """,
            operationId = "getUpcomingMovies"
    )
    public MoviePageResponse getUpcomingMovies(
            @Parameter(
                    description = "Número da página de resultados.",
                    example = "1"
            )
            @RequestParam(defaultValue = "1")
            @Min(1)
            @Max(MAX_PAGE)
            int page
    ) {
        MoviePageResult result = movieService.getUpcomingMovies(page);

        return toMoviePageResponse(result);
    }

    @GetMapping("/top-rated")
    @Operation(
            summary = "Obter filmes mais bem avaliados",
            description = """
                    Retorna filmes mais bem avaliados
                    de acordo com o TMDB.

                    Os resultados são paginados.
                    """,
            operationId = "getTopRatedMovies"
    )
    public MoviePageResponse getTopRatedMovies(
            @Parameter(
                    description = "Número da página de resultados.",
                    example = "1"
            )
            @RequestParam(defaultValue = "1")
            @Min(1)
            @Max(MAX_PAGE)
            int page
    ) {
        MoviePageResult result = movieService.getTopRatedMovies(page);

        return toMoviePageResponse(result);
    }

    @GetMapping("/{movieId}/similar")
    @Operation(
            summary = "Consultar filmes similares",
            description = """
                    Retorna filmes similares ao filme informado
                    pelo ID do TMDB.

                    Os resultados são paginados.
                    """,
            operationId = "getSimilarMovies"
    )
    public MoviePageResponse getSimilarMovies(
            @Parameter(
                    description = "ID do filme no TMDB.",
                    example = "550",
                    required = true
            )
            @PathVariable
            @Positive
            Long movieId,

            @Parameter(
                    description = "Número da página de resultados.",
                    example = "1"
            )
            @RequestParam(defaultValue = "1")
            @Min(1)
            @Max(MAX_PAGE)
            int page
    ) {
        MoviePageResult result =
                movieService.getSimilarMovies(movieId, page);

        return toMoviePageResponse(result);
    }

    @GetMapping("/{movieId}/recommendations")
    @Operation(
            summary = "Consultar filmes recomendados",
            description = """
                    Retorna filmes recomendados pelo TMDB
                    com base no filme informado.

                    Os resultados são paginados.
                    """,
            operationId = "getMovieRecommendations"
    )
    public MoviePageResponse getMovieRecommendations(
            @Parameter(
                    description = "ID do filme no TMDB.",
                    example = "550",
                    required = true
            )
            @PathVariable
            @Positive
            Long movieId,

            @Parameter(
                    description = "Número da página de resultados.",
                    example = "1"
            )
            @RequestParam(defaultValue = "1")
            @Min(1)
            @Max(MAX_PAGE)
            int page
    ) {
        MoviePageResult result =
                movieService.getMovieRecommendations(movieId, page);

        return toMoviePageResponse(result);
    }

    @GetMapping("/{movieId}/reviews")
    @Operation(
            summary = "Consultar avaliações do filme",
            description = """
                    Retorna avaliações realizadas por usuários
                    para o filme informado.

                    Os resultados são paginados.
                    """,
            operationId = "getMovieReviews"
    )
    public MovieReviewsResponse getMovieReviews(
            @Parameter(
                    description = "ID do filme no TMDB.",
                    example = "550",
                    required = true
            )
            @PathVariable
            @Positive
            Long movieId,

            @Parameter(
                    description = "Número da página de resultados.",
                    example = "1"
            )
            @RequestParam(defaultValue = "1")
            @Min(1)
            @Max(MAX_PAGE)
            int page
    ) {
        return movieService.getMovieReviews(movieId, page);
    }

    @GetMapping("/{movieId}/lists")
    @Operation(
            summary = "Consultar listas do filme",
            description = """
                    Retorna listas públicas do TMDB
                    que contêm o filme informado.

                    Os resultados são paginados.
                    """,
            operationId = "getMovieLists"
    )
    public MovieListResponse getMovieLists(
            @Parameter(
                    description = "ID do filme no TMDB.",
                    example = "550",
                    required = true
            )
            @PathVariable
            @Positive
            Long movieId,

            @Parameter(
                    description = "Número da página de resultados.",
                    example = "1"
            )
            @RequestParam(defaultValue = "1")
            @Min(1)
            @Max(MAX_PAGE)
            int page
    ) {
        return movieService.getMovieLists(movieId, page);
    }

    @GetMapping("/{movieId}/trailer")
    @Operation(
            summary = "Buscar trailer do filme",
            description = """
                    Retorna o trailer principal disponível
                    para o filme informado.

                    Caso não exista trailer disponível,
                    a API retorna HTTP 204.
                    """,
            operationId = "getMovieTrailer"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Trailer encontrado."
            ),
            @ApiResponse(
                    responseCode = "204",
                    description = "Nenhum trailer disponível."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Filme não encontrado.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Erro na comunicação com o TMDB.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    public ResponseEntity<MovieTrailerResponse> getMovieTrailer(
            @Parameter(
                    description = "ID do filme no TMDB.",
                    example = "414906",
                    required = true
            )
            @PathVariable
            @Positive
            Long movieId
    ) {
        MovieTrailerResponse trailer =
                movieService.getMovieTrailer(movieId);

        if (trailer == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(trailer);
    }

    @GetMapping("/{movieId}")
    @Operation(
            summary = "Consultar detalhes completos de um filme",
            description = """
                    Retorna as informações completas de um filme
                    utilizando seu ID no TMDB.

                    As informações incluem título, título original,
                    sinopse, tagline, data de lançamento, ano,
                    duração, idioma original, idiomas falados,
                    gêneros, avaliação, quantidade de votos,
                    popularidade, status, pôster, backdrop,
                    página oficial, orçamento, receita,
                    produtoras e países de produção.
                    """,
            operationId = "getMovieDetails"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalhes do filme encontrados com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = MovieDetailsResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 414906,
                                              "title": "The Batman",
                                              "originalTitle": "The Batman",
                                              "overview": "In his second year of fighting crime, Batman uncovers corruption in Gotham City.",
                                              "tagline": "Unmask the truth.",
                                              "releaseDate": "2022-03-01",
                                              "runtime": 176,
                                              "releaseYear": 2022,
                                              "originalLanguage": "en",
                                              "spokenLanguages": [
                                                {
                                                  "code": "en",
                                                  "name": "English",
                                                  "englishName": "English"
                                                }
                                              ],
                                              "genres": [
                                                {
                                                  "id": 80,
                                                  "name": "Crime"
                                                },
                                                {
                                                  "id": 18,
                                                  "name": "Drama"
                                                }
                                              ],
                                              "rating": 7.668,
                                              "voteCount": 8500,
                                              "popularity": 123.45,
                                              "status": "Released",
                                              "posterPath": "/74xTEgt7R36Fpooo50r9T25onhq.jpg",
                                              "backdropPath": "/tmU7GeKVybMWFButWEGl2M4GeiP.jpg",
                                              "homepage": "https://www.thebatman.com/",
                                              "adult": false,
                                              "budget": 185000000,
                                              "revenue": 772000000,
                                              "productionCompanies": [],
                                              "productionCountries": []
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "ID do filme inválido.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Filme não encontrado no TMDB.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Erro na comunicação com o TMDB.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    public MovieDetailsResponse getMovie(
            @Parameter(
                    description = "ID do filme no TMDB.",
                    example = "414906",
                    required = true
            )
            @PathVariable
            @Positive
            Long movieId
    ) {
        return movieService.getMovieDetails(movieId);
    }

    private MoviePageResponse toMoviePageResponse(
            MoviePageResult result
    ) {
        List<MovieResponse> responses =
                result.results()
                        .stream()
                        .map(movieResponseMapper::toResponse)
                        .toList();

        return new MoviePageResponse(
                result.page(),
                result.totalPages(),
                result.totalResults(),
                responses
        );
    }
}