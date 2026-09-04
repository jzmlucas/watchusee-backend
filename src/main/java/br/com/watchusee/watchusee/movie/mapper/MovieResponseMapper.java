package br.com.watchusee.watchusee.movie.mapper;

import br.com.watchusee.watchusee.movie.api.dto.MovieResponse;
import br.com.watchusee.watchusee.movie.domain.Movie;
import org.springframework.stereotype.Component;

@Component
public class MovieResponseMapper {

    public MovieResponse toResponse(Movie movie) {

        if (movie == null) {
            return null;
        }

        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getOverview(),
                movie.getReleaseDate(),
                movie.getPosterPath(),
                movie.getRating()
        );
    }
}