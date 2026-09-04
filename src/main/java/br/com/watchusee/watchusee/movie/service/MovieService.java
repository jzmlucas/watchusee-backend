package br.com.watchusee.watchusee.movie.service;

import br.com.watchusee.watchusee.movie.client.tmdb.TmdbClient;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbMovieResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbSearchResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbVideosResponse;
import br.com.watchusee.watchusee.movie.domain.Movie;
import br.com.watchusee.watchusee.movie.dto.MovieTrailerResponse;
import br.com.watchusee.watchusee.movie.mapper.MovieMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MovieService {

    private final TmdbClient tmdbClient;
    private final MovieMapper movieMapper;

    public MovieService(
            TmdbClient tmdbClient,
            MovieMapper movieMapper
    ) {
        this.tmdbClient = tmdbClient;
        this.movieMapper = movieMapper;
    }

    public List<Movie> searchMovies(String query) {

        String normalizedQuery =
                normalizeQuery(query);

        TmdbSearchResponse response =
                tmdbClient.searchMovies(normalizedQuery);

        if (response == null ||
                response.results() == null) {

            return List.of();
        }

        return response.results()
                .stream()
                .map(movieMapper::toDomain)
                .toList();
    }

    public Movie getMovie(Long movieId) {

        validateMovieId(movieId);

        TmdbMovieResponse response =
                tmdbClient.getMovie(movieId);

        if (response == null) {

            throw new IllegalStateException(
                    "O TMDB retornou uma resposta vazia para o filme: "
                            + movieId
            );
        }

        return movieMapper.toDomain(response);
    }

    public Movie getRandomTrendingMovie() {

        TmdbSearchResponse response =
                tmdbClient.getTrendingMovies();

        if (response == null) {

            throw new IllegalStateException(
                    "Não foi possível obter os filmes em tendência."
            );
        }

        if (response.results() == null ||
                response.results().isEmpty()) {

            throw new IllegalStateException(
                    "Nenhum filme em tendência foi encontrado."
            );
        }

        int randomIndex =
                ThreadLocalRandom.current()
                        .nextInt(response.results().size());

        TmdbMovieResponse randomMovie =
                response.results().get(randomIndex);

        return movieMapper.toDomain(randomMovie);
    }

    public List<Movie> getTrendingMovies() {

        TmdbSearchResponse response =
                tmdbClient.getTrendingMovies();

        if (response == null ||
                response.results() == null) {

            return List.of();
        }

        return response.results()
                .stream()
                .map(movieMapper::toDomain)
                .toList();
    }

    public List<Movie> getSimilarMovies(Long movieId) {

        validateMovieId(movieId);

        TmdbSearchResponse response =
                tmdbClient.getSimilarMovies(movieId);

        if (response == null ||
                response.results() == null) {

            return List.of();
        }

        return response.results()
                .stream()
                .map(movieMapper::toDomain)
                .toList();
    }

    public List<Movie> getTopRatedMovies(int page) {

        validatePage(page);

        TmdbSearchResponse response =
                tmdbClient.getTopRatedMovies(page);

        if (response == null ||
                response.results() == null) {

            return List.of();
        }

        return response.results()
                .stream()
                .map(movieMapper::toDomain)
                .toList();
    }

    public MovieTrailerResponse getMovieTrailer(Long movieId) {

        TmdbVideosResponse response =
                tmdbClient.getMovieVideos(movieId);

        if (response == null ||
                response.results() == null) {

            return null;
        }

        return response.results()
                .stream()
                .filter(video ->
                        "Trailer".equalsIgnoreCase(video.type())
                )
                .filter(video ->
                        "YouTube".equalsIgnoreCase(video.site())
                )
                .findFirst()
                .map(video ->
                        new MovieTrailerResponse(
                                video.key(),
                                video.name(),
                                video.site(),
                                video.type()
                        )
                )
                .orElse(null);
    }

    private String normalizeQuery(String query) {

        if (query == null) {

            throw new IllegalArgumentException(
                    "A busca do filme não pode ser nula."
            );
        }

        String normalizedQuery =
                query.trim();

        if (normalizedQuery.isBlank()) {

            throw new IllegalArgumentException(
                    "A busca do filme não pode estar vazia."
            );
        }

        return normalizedQuery;
    }

    private void validateMovieId(Long movieId) {

        if (movieId == null ||
                movieId <= 0) {

            throw new IllegalArgumentException(
                    "O ID do filme deve ser maior que zero."
            );
        }
    }

    private void validatePage(int page) {

        if (page <= 0) {

            throw new IllegalArgumentException(
                    "A página deve ser maior que zero."
            );
        }
    }
}