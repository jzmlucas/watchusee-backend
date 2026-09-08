package br.com.watchusee.watchusee.movie.mapper;

import br.com.watchusee.watchusee.movie.api.dto.GenreResponse;
import br.com.watchusee.watchusee.movie.api.dto.LanguageResponse;
import br.com.watchusee.watchusee.movie.api.dto.MovieDetailsResponse;
import br.com.watchusee.watchusee.movie.api.dto.ProductionCompanyResponse;
import br.com.watchusee.watchusee.movie.api.dto.ProductionCountryResponse;
import br.com.watchusee.watchusee.movie.client.tmdb.dto.TmdbMovieResponse;
import org.springframework.stereotype.Component;

@Component
public class MovieDetailsMapper {

    public MovieDetailsResponse toResponse(TmdbMovieResponse movie) {

        if (movie == null) {
            throw new IllegalArgumentException(
                    "O filme não pode ser nulo."
            );
        }

        return new MovieDetailsResponse(
                movie.id(),
                movie.title(),
                movie.originalTitle(),
                movie.overview(),
                movie.tagline(),
                movie.releaseDate(),
                movie.runtime(),
                movie.releaseDate() != null
                        ? movie.releaseDate().getYear()
                        : null,
                movie.originalLanguage(),
                movie.spokenLanguages() == null
                        ? java.util.List.of()
                        : movie.spokenLanguages()
                        .stream()
                        .map(language -> new LanguageResponse(
                                language.iso6391(),
                                language.name(),
                                language.englishName()
                        ))
                        .toList(),
                movie.genres() == null
                        ? java.util.List.of()
                        : movie.genres()
                        .stream()
                        .map(genre -> new GenreResponse(
                                genre.id(),
                                genre.name()
                        ))
                        .toList(),
                movie.rating(),
                movie.voteCount(),
                movie.popularity(),
                movie.status(),
                movie.posterPath(),
                movie.backdropPath(),
                movie.homepage(),
                movie.adult(),
                movie.budget(),
                movie.revenue(),
                movie.productionCompanies() == null
                        ? java.util.List.of()
                        : movie.productionCompanies()
                        .stream()
                        .map(company -> new ProductionCompanyResponse(
                                company.id(),
                                company.name(),
                                company.logoPath(),
                                company.originCountry()
                        ))
                        .toList(),
                movie.productionCountries() == null
                        ? java.util.List.of()
                        : movie.productionCountries()
                        .stream()
                        .map(country -> new ProductionCountryResponse(
                                country.iso31661(),
                                country.name()
                        ))
                        .toList()
        );
    }
}