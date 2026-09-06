package br.com.watchusee.watchusee.movie.client.tmdb;

import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbListsResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbMovieResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbReviewsResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbSearchResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbVideosResponse;

public interface TmdbClient {

    TmdbSearchResponse searchMovies(String query);

    TmdbMovieResponse getMovie(Long movieId);

    TmdbSearchResponse getTrendingMovies();

    TmdbSearchResponse getTopRatedMovies(int page);

    TmdbSearchResponse getPopularMovies(int page);

    TmdbSearchResponse getNowPlayingMovies(int page);

    TmdbSearchResponse getUpcomingMovies(int page);

    TmdbSearchResponse getSimilarMovies(Long movieId, int page);

    TmdbSearchResponse getSimilarMovies(Long movieId);

    TmdbSearchResponse getMovieRecommendations(Long movieId, int page);

    TmdbReviewsResponse getMovieReviews(Long movieId, int page);

    TmdbListsResponse getMovieLists(Long movieId, int page);

    TmdbVideosResponse getMovieVideos(Long movieId);
}