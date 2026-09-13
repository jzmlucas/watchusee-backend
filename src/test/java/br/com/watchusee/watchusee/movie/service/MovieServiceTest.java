package br.com.watchusee.watchusee.movie.service;

import br.com.watchusee.watchusee.movie.client.tmdb.TmdbClient;
import br.com.watchusee.watchusee.movie.domain.Movie;
import br.com.watchusee.watchusee.movie.mapper.MovieDetailsMapper;
import br.com.watchusee.watchusee.movie.mapper.MovieMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private TmdbClient tmdbClient;

    @Mock
    private MovieMapper movieMapper;

    @Mock
    private MovieDetailsMapper movieDetailsMapper;

    private MovieService movieService;

    @BeforeEach
    void setUp() {
        movieService = new MovieService(
                tmdbClient,
                movieMapper,
                movieDetailsMapper
        );
    }

    @Test
    void shouldHandleNullSearchResponse() {
        when(tmdbClient.searchMovies("qualquer")).thenReturn(null);

        List<Movie> result = movieService.searchMovies("qualquer");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowOnZeroMovieId() {
        assertThatThrownBy(() -> movieService.getMovie(0L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> movieService.getMovie(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
