package br.com.watchusee.watchusee.movie.api;

import br.com.watchusee.watchusee.movie.api.dto.MovieResponse;
import br.com.watchusee.watchusee.movie.domain.Movie;
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
import jakarta.validation.constraints.NotBlank;
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
                                                "overview": "In his second year of fighting crime, Batman uncovers corruption in Gotham City that connects to his own family while facing a serial killer known as the Riddler.",
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
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2026-08-26T04:30:00Z",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "O termo de busca deve possuir entre 2 e 100 caracteres.",
                                              "path": "/api/v1/movies/search"
                                            }
                                            """
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
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2026-08-26T04:30:00Z",
                                              "status": 502,
                                              "error": "Bad Gateway",
                                              "message": "Erro ao buscar filmes no TMDB.",
                                              "path": "/api/v1/movies/search"
                                            }
                                            """
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

                    Os filmes em tendência são obtidos através do endpoint
                    de tendências do TMDB utilizando a janela semanal.

                    A aplicação seleciona aleatoriamente um filme entre os
                    resultados retornados pelo serviço externo.

                    Esse endpoint é destinado principalmente à descoberta
                    de conteúdo e pode ser utilizado pela tela inicial
                    da aplicação.
                    """,
            operationId = "getRandomTrendingMovie"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Filme em tendência encontrado com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = MovieResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 414906,
                                              "title": "The Batman",
                                              "overview": "In his second year of fighting crime, Batman uncovers corruption in Gotham City that connects to his own family while facing a serial killer known as the Riddler.",
                                              "releaseDate": "2022-03-01",
                                              "posterPath": "/74xTEgt7R36Fpooo50r9T25onhq.jpg",
                                              "rating": 7.668
                                            }
                                            """
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
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2026-08-26T04:30:00Z",
                                              "status": 502,
                                              "error": "Bad Gateway",
                                              "message": "Erro ao buscar filmes em tendência no TMDB.",
                                              "path": "/api/v1/movies/trending/random"
                                            }
                                            """
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
    public List<MovieResponse> getTrendingMovies() {

        List<Movie> movies =
                movieService.getTrendingMovies();

        return movies.stream()
                .map(movieResponseMapper::toResponse)
                .toList();
    }

    @GetMapping("/{movieId}/trailer")
    @Operation(
            summary = "Buscar trailer do filme",
            description = "Retorna o trailer principal do filme."
    )
    public ResponseEntity<MovieTrailerResponse> getMovieTrailer(
            @PathVariable Long movieId
    ) {

        MovieTrailerResponse trailer =
                movieService.getMovieTrailer(movieId);

        if (trailer == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(trailer);
    }

    @GetMapping("/top-rated")
    @Operation(
            summary = "Obter filmes mais bem avaliados",
            description = """
                Retorna uma lista de filmes mais bem avaliados
                de acordo com as avaliações disponíveis no TMDB.

                Os resultados são obtidos diretamente através do
                endpoint /3/movie/top_rated do TMDB.

                A página permite controlar a paginação dos resultados.

                Esse endpoint é público e pode ser utilizado por
                usuários autenticados ou visitantes.
                """,
            operationId = "getTopRatedMovies"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Filmes mais bem avaliados encontrados com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = MovieResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                        [
                                          {
                                            "id": 278,
                                            "title": "Um Sonho de Liberdade",
                                            "overview": "Um banqueiro é condenado...",
                                            "releaseDate": "1994-09-23",
                                            "posterPath": "/...",
                                            "rating": 8.7
                                          }
                                        ]
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Página inválida.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "timestamp": "2026-08-29T12:00:00Z",
                                          "status": 400,
                                          "error": "Bad Request",
                                          "message": "A página deve ser maior que zero.",
                                          "path": "/api/v1/movies/top-rated"
                                        }
                                        """
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
                            ),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "timestamp": "2026-08-29T12:00:00Z",
                                          "status": 502,
                                          "error": "Bad Gateway",
                                          "message": "Erro ao buscar filmes mais bem avaliados no TMDB.",
                                          "path": "/api/v1/movies/top-rated"
                                        }
                                        """
                            )
                    )
            )
    })
    public List<MovieResponse> getTopRatedMovies(

            @Parameter(
                    description = "Número da página de resultados.",
                    example = "1"
            )
            @RequestParam(
                    defaultValue = "1"
            )
            int page

    ) {

        List<Movie> movies =
                movieService.getTopRatedMovies(page);

        return movies.stream()
                .map(movieResponseMapper::toResponse)
                .toList();
    }

    @GetMapping("/{movieId}/similar")
    @Operation(
            summary = "Consultar filmes similares",
            description = """
                Retorna filmes similares ao filme informado.

                A busca é realizada através do endpoint de filmes
                similares do TMDB.

                Os resultados são baseados principalmente em gêneros
                e palavras-chave relacionadas ao enredo do filme.
                """,
            operationId = "getSimilarMovies"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Filmes similares encontrados com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = MovieResponse.class
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
    public List<MovieResponse> getSimilarMovies(

            @Parameter(
                    description = "ID do filme no TMDB.",
                    example = "550",
                    required = true
            )
            @PathVariable Long movieId
    ) {

        List<Movie> movies =
                movieService.getSimilarMovies(movieId);

        return movies.stream()
                .map(movieResponseMapper::toResponse)
                .toList();
    }

    @GetMapping("/{movieId}")
    @Operation(
            summary = "Consultar filme por ID",
            description = """
                    Retorna os detalhes de um filme utilizando o ID do TMDB.

                    O filme é consultado diretamente no serviço externo.
                    """,
            operationId = "getMovieById"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Filme encontrado com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = MovieResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 414906,
                                              "title": "The Batman",
                                              "overview": "In his second year of fighting crime, Batman uncovers corruption in Gotham City that connects to his own family while facing a serial killer known as the Riddler.",
                                              "releaseDate": "2022-03-01",
                                              "posterPath": "/74xTEgt7R36Fpooo50r9T25onhq.jpg",
                                              "rating": 7.668
                                            }
                                            """
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
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2026-08-26T04:30:00Z",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Filme não encontrado no TMDB: 999999999",
                                              "path": "/api/v1/movies/999999999"
                                            }
                                            """
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
                            ),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "timestamp": "2026-08-26T04:30:00Z",
                                              "status": 502,
                                              "error": "Bad Gateway",
                                              "message": "Erro ao buscar filme no TMDB.",
                                              "path": "/api/v1/movies/999999999"
                                            }
                                            """
                            )
                    )
            )
    })
    public MovieResponse getMovie(

            @Parameter(
                    description = "ID do filme no TMDB.",
                    example = "414906",
                    required = true
            )
            @PathVariable Long movieId
    ) {

        Movie movie = movieService.getMovie(movieId);

        return movieResponseMapper.toResponse(movie);
    }
}