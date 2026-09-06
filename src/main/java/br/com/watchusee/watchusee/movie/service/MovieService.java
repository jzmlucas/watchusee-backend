package br.com.watchusee.watchusee.movie.service;

import br.com.watchusee.watchusee.movie.api.dto.MovieListItemResponse;
import br.com.watchusee.watchusee.movie.api.dto.MovieListResponse;
import br.com.watchusee.watchusee.movie.api.dto.MovieReviewItemResponse;
import br.com.watchusee.watchusee.movie.api.dto.MovieReviewsResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.TmdbClient;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbListsResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbMovieResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbReviewResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbSearchResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbVideosResponse;
import br.com.watchusee.watchusee.movie.domain.Movie;
import br.com.watchusee.watchusee.movie.dto.MoviePageResult;
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

        String normalizedQuery = normalizeQuery(query);

        TmdbSearchResponse response =
                tmdbClient.searchMovies(normalizedQuery);

        return mapMovies(response);
    }

    public Movie getMovie(Long movieId) {

        validateMovieId(movieId);

        TmdbMovieResponse response =
                tmdbClient.getMovie(movieId);

        if (response == null) {
            throw new IllegalStateException(
                    "O TMDB retornou uma resposta vazia para o filme."
            );
        }

        return movieMapper.toDomain(response);
    }

    public Movie getRandomTrendingMovie() {

        TmdbSearchResponse response =
                tmdbClient.getTrendingMovies();

        if (response == null) {
            throw new IllegalStateException(
                    "O TMDB retornou uma resposta vazia."
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

        return movieMapper.toDomain(
                response.results().get(randomIndex)
        );
    }

    public List<Movie> getTrendingMovies() {

        TmdbSearchResponse response =
                tmdbClient.getTrendingMovies();

        return mapMovies(response);
    }

    public MoviePageResult getTopRatedMovies(int page) {

        validatePage(page);

        TmdbSearchResponse response =
                tmdbClient.getTopRatedMovies(page);

        return mapMoviePage(response);
    }

    public MoviePageResult getPopularMovies(int page) {

        validatePage(page);

        TmdbSearchResponse response =
                tmdbClient.getPopularMovies(page);

        return mapMoviePage(response);
    }

    public MoviePageResult getNowPlayingMovies(int page) {

        validatePage(page);

        TmdbSearchResponse response =
                tmdbClient.getNowPlayingMovies(page);

        return mapMoviePage(response);
    }

    public MoviePageResult getUpcomingMovies(int page) {

        validatePage(page);

        TmdbSearchResponse response =
                tmdbClient.getUpcomingMovies(page);

        return mapMoviePage(response);
    }

    public MoviePageResult getSimilarMovies(
            Long movieId,
            int page
    ) {

        validateMovieId(movieId);
        validatePage(page);

        TmdbSearchResponse response =
                tmdbClient.getSimilarMovies(
                        movieId,
                        page
                );

        return mapMoviePage(response);
    }

    public MoviePageResult getMovieRecommendations(
            Long movieId,
            int page
    ) {

        validateMovieId(movieId);
        validatePage(page);

        TmdbSearchResponse response =
                tmdbClient.getMovieRecommendations(
                        movieId,
                        page
                );

        return mapMoviePage(response);
    }

    public MovieReviewsResponse getMovieReviews(
            Long movieId,
            int page
    ) {

        validateMovieId(movieId);
        validatePage(page);

        var response =
                tmdbClient.getMovieReviews(
                        movieId,
                        page
                );

        if (response == null) {

            return new MovieReviewsResponse(
                    page,
                    0,
                    0,
                    List.of()
            );
        }

        List<MovieReviewItemResponse> reviews =
                response.results() == null
                        ? List.of()
                        : response.results()
                        .stream()
                        .map(this::toReviewResponse)
                        .toList();

        return new MovieReviewsResponse(
                response.page(),
                response.totalPages(),
                response.totalResults(),
                reviews
        );
    }

    public MovieListResponse getMovieLists(
            Long movieId,
            int page
    ) {

        validateMovieId(movieId);
        validatePage(page);

        TmdbListsResponse response =
                tmdbClient.getMovieLists(
                        movieId,
                        page
                );

        if (response == null) {

            return new MovieListResponse(
                    page,
                    0,
                    0,
                    List.of()
            );
        }

        List<MovieListItemResponse> lists =
                response.results() == null
                        ? List.of()
                        : response.results()
                        .stream()
                        .map(list ->
                                new MovieListItemResponse(
                                        list.id(),
                                        list.name(),
                                        list.description(),
                                        list.itemCount(),
                                        list.posterPath(),
                                        list.backdropPath()
                                )
                        )
                        .toList();

        return new MovieListResponse(
                response.page(),
                response.totalPages(),
                response.totalResults(),
                lists
        );
    }

    public MovieTrailerResponse getMovieTrailer(
            Long movieId
    ) {

        validateMovieId(movieId);

        TmdbVideosResponse response =
                tmdbClient.getMovieVideos(movieId);

        if (response == null ||
                response.results() == null) {

            return null;
        }

        return response.results()
                .stream()
                .filter(video ->
                        "Trailer".equalsIgnoreCase(
                                video.type()
                        )
                )
                .filter(video ->
                        "YouTube".equalsIgnoreCase(
                                video.site()
                        )
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

    private List<Movie> mapMovies(
            TmdbSearchResponse response
    ) {

        if (response == null ||
                response.results() == null) {

            return List.of();
        }

        return response.results()
                .stream()
                .map(movieMapper::toDomain)
                .toList();
    }

    private MoviePageResult mapMoviePage(
            TmdbSearchResponse response
    ) {

        if (response == null) {
            return new MoviePageResult(
                    1,
                    0,
                    0,
                    List.of()
            );
        }

        List<Movie> movies =
                response.results() == null
                        ? List.of()
                        : response.results()
                        .stream()
                        .map(movieMapper::toDomain)
                        .toList();

        return new MoviePageResult(
                response.page(),
                response.totalPages(),
                response.totalResults(),
                movies
        );
    }

    private MovieReviewItemResponse toReviewResponse(
            TmdbReviewResponse review
    ) {

        String username = null;
        String avatarPath = null;
        Double rating = null;

        if (review.authorDetails() != null) {

            username =
                    review.authorDetails().username();

            avatarPath =
                    review.authorDetails().avatarPath();

            rating =
                    review.authorDetails().rating();
        }

        return new MovieReviewItemResponse(
                review.id(),
                review.author(),
                username,
                avatarPath,
                rating,
                review.content(),
                review.createdAt(),
                review.updatedAt(),
                review.url()
        );
    }

    private String normalizeQuery(String query) {

        if (query == null) {
            return null;
        }

        return query.trim();
    }

    private void validateMovieId(Long movieId) {

        if (movieId == null || movieId <= 0) {

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
