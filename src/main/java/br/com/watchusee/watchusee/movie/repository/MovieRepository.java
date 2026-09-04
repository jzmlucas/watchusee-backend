package br.com.watchusee.watchusee.movie.repository;

import br.com.watchusee.watchusee.movie.domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}
