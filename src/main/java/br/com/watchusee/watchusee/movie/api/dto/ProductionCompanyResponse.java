package br.com.watchusee.watchusee.movie.api.dto;

public record ProductionCompanyResponse(
        Long id,
        String name,
        String logoPath,
        String originCountry
) {
}