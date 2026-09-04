package br.com.watchusee.watchusee.movie.client.tmdb.exception;

public class TmdbException extends RuntimeException {

    public TmdbException(String message) {
        super(message);
    }

    public TmdbException(String message, Throwable cause) {
        super(message, cause);
    }
}