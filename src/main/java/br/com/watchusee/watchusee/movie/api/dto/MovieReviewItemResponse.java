package br.com.watchusee.watchusee.movie.api.dto;

public record MovieReviewItemResponse(

        String id,

        String author,

        String username,

        String avatarPath,

        Double rating,

        String content,

        String createdAt,

        String updatedAt,

        String url
) {
}