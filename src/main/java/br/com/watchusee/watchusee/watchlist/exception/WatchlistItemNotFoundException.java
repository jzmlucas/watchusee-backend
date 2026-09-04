package br.com.watchusee.watchusee.watchlist.exception;

public class WatchlistItemNotFoundException
        extends RuntimeException {

    public WatchlistItemNotFoundException(String message) {
        super(message);
    }
}