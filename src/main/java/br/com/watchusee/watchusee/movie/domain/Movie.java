package br.com.watchusee.watchusee.movie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @Column(nullable = false)
    private Long id;

    @Column(
            nullable = false,
            length = 255
    )
    private String title;

    @Column(
            columnDefinition = "TEXT"
    )
    private String overview;

    @Column(
            name = "release_date"
    )
    private LocalDate releaseDate;

    @Column(
            name = "poster_path",
            length = 500
    )
    private String posterPath;

    @Column
    private Double rating;

    protected Movie() {
    }

    public Movie(
            Long id,
            String title,
            String overview,
            LocalDate releaseDate,
            String posterPath,
            Double rating
    ) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "O ID do filme deve ser maior que zero."
            );
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "O título do filme não pode ser vazio."
            );
        }

        this.id = id;
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.posterPath = posterPath;
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getOverview() {
        return overview;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public Double getRating() {
        return rating;
    }
}