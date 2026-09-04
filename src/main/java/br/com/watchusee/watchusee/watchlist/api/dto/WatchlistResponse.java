package br.com.watchusee.watchusee.watchlist.api.dto;

import br.com.watchusee.watchusee.movie.domain.Movie;

import java.util.List;

public record WatchlistResponse(
        List<Movie> movies
) {
}