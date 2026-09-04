package br.com.watchusee.watchusee.movie.client.tmdb.exception;

public class TmdbMovieNotFoundException extends RuntimeException {

    public TmdbMovieNotFoundException(String message) {
        super(message);
    }
}