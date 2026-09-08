package br.com.watchusee.watchusee.movie.api.dto;

public record LanguageResponse(
        String code,
        String name,
        String englishName
) {
}