package br.com.watchusee.watchusee.movie.client.tmdb;

import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbMovieResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbSearchResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbVideosResponse;

public interface TmdbClient {

    TmdbSearchResponse searchMovies(String query);

    TmdbMovieResponse getMovie(Long movieId);

    TmdbSearchResponse getTrendingMovies();

    TmdbSearchResponse getTopRatedMovies(int page);

    TmdbSearchResponse getSimilarMovies(Long movieId, int page);

    TmdbSearchResponse getSimilarMovies(Long movieId);

    TmdbVideosResponse getMovieVideos(Long movieId);
}

