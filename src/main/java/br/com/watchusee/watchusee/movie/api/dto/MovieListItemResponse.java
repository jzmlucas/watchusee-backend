package br.com.watchusee.watchusee.movie.api.dto;

public record MovieListItemResponse(

        int id,

        String name,

        String description,

        int itemCount,

        String posterPath,

        String backdropPath
) {
}