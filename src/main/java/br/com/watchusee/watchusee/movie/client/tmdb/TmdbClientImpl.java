package br.com.watchusee.watchusee.movie.client.tmdb;

import br.com.watchusee.watchusee.config.TmdbProperties;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbMovieResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbSearchResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbVideosResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.exception.TmdbException;
import br.com.watchusee.watchusee.movie.client.tmdb.exception.TmdbMovieNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TmdbClientImpl implements TmdbClient {

    private static final String TRENDING_TIME_WINDOW = "week";

    private final RestClient restClient;
    private final TmdbProperties tmdbProperties;

    public TmdbClientImpl(
            RestClient tmdbRestClient,
            TmdbProperties tmdbProperties
    ) {
        this.restClient = tmdbRestClient;
        this.tmdbProperties = tmdbProperties;
    }

    @Override
    public TmdbSearchResponse searchMovies(String query) {

        try {

            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/3/search/movie")
                            .queryParam(
                                    "api_key",
                                    tmdbProperties.apiKey()
                            )
                            .queryParam(
                                    "language",
                                    tmdbProperties.language()
                            )
                            .queryParam(
                                    "query",
                                    query
                            )
                            .build()
                    )
                    .retrieve()
                    .body(TmdbSearchResponse.class);

        } catch (RestClientException exception) {

            throw new TmdbException(
                    "Erro ao buscar filmes no TMDB.",
                    exception
            );
        }
    }

    @Override
    public TmdbMovieResponse getMovie(Long movieId) {

        try {

            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/3/movie/{movieId}")
                            .queryParam(
                                    "api_key",
                                    tmdbProperties.apiKey()
                            )
                            .queryParam(
                                    "language",
                                    tmdbProperties.language()
                            )
                            .build(movieId)
                    )
                    .retrieve()
                    .body(TmdbMovieResponse.class);

        } catch (HttpClientErrorException.NotFound exception) {

            throw new TmdbMovieNotFoundException(
                    "Filme não encontrado no TMDB: " + movieId
            );

        } catch (RestClientException exception) {

            throw new TmdbException(
                    "Erro ao buscar filme no TMDB.",
                    exception
            );
        }
    }

    @Override
    public TmdbSearchResponse getTrendingMovies() {

        try {

            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/3/trending/movie/{timeWindow}")
                            .queryParam(
                                    "api_key",
                                    tmdbProperties.apiKey()
                            )
                            .queryParam(
                                    "language",
                                    tmdbProperties.language()
                            )
                            .build(TRENDING_TIME_WINDOW)
                    )
                    .retrieve()
                    .body(TmdbSearchResponse.class);

        } catch (RestClientException exception) {

            throw new TmdbException(
                    "Erro ao buscar filmes em tendência no TMDB.",
                    exception
            );
        }
    }

    @Override
    public TmdbSearchResponse getTopRatedMovies(int page) {

        try {

            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/3/movie/top_rated")
                            .queryParam(
                                    "api_key",
                                    tmdbProperties.apiKey()
                            )
                            .queryParam(
                                    "language",
                                    tmdbProperties.language()
                            )
                            .queryParam(
                                    "page",
                                    page
                            )
                            .build()
                    )
                    .retrieve()
                    .body(TmdbSearchResponse.class);

        } catch (RestClientException exception) {

            throw new TmdbException(
                    "Erro ao buscar filmes mais bem avaliados no TMDB.",
                    exception
            );
        }
    }

    @Override
    public TmdbSearchResponse getSimilarMovies(Long movieId, int page) {
        return null;
    }

    @Override
    public TmdbSearchResponse getSimilarMovies(Long movieId) {

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/3/movie/{movieId}/similar")
                            .queryParam(
                                    "api_key",
                                    tmdbProperties.apiKey()
                            )
                            .queryParam(
                                    "language",
                                    tmdbProperties.language()
                            )
                            .build(movieId)
                    )
                    .retrieve()
                    .body(TmdbSearchResponse.class);

        } catch (RestClientException exception) {

            throw new TmdbException(
                    "Erro ao buscar filmes similares no TMDB.",
                    exception
            );
        }
    }

    @Override
    public TmdbVideosResponse getMovieVideos(Long movieId) {

        try {

            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/3/movie/{movieId}/videos")
                            .queryParam(
                                    "api_key",
                                    tmdbProperties.apiKey()
                            )
                            .queryParam(
                                    "language",
                                    "pt-BR"
                            )
                            .build(movieId)
                    )
                    .retrieve()
                    .body(TmdbVideosResponse.class);

        } catch (RestClientException exception) {

            throw new TmdbException(
                    "Erro ao buscar vídeos do filme no TMDB.",
                    exception
            );
        }
    }

}