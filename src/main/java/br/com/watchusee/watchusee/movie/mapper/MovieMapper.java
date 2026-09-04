package br.com.watchusee.watchusee.movie.mapper;

import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbMovieResponse;
import br.com.watchusee.watchusee.movie.domain.Movie;
import org.springframework.stereotype.Component;

@Component
public class MovieMapper {

    public Movie toDomain(TmdbMovieResponse response) {

        if (response == null) {
            throw new IllegalArgumentException(
                    "A resposta do filme não pode ser nula."
            );
        }

        return new Movie(
                response.id(),
                response.title(),
                response.overview(),
                response.releaseDate(),
                response.posterPath(),
                response.rating()
        );
    }
}