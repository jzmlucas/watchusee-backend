package br.com.watchusee.watchusee.watchlist.api;

import br.com.watchusee.watchusee.movie.api.dto.MovieResponse;
import br.com.watchusee.watchusee.movie.mapper.MovieResponseMapper;
import br.com.watchusee.watchusee.shared.security.AuthenticatedUser;
import br.com.watchusee.watchusee.watchlist.api.dto.PageResponse;
import br.com.watchusee.watchusee.watchlist.api.dto.WatchlistResponse;
import br.com.watchusee.watchusee.watchlist.domain.Watchlist;
import br.com.watchusee.watchusee.watchlist.domain.WatchlistStatus;
import br.com.watchusee.watchusee.watchlist.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}/watchlist")
@Validated
@Tag(
        name = "User Watchlist",
        description = "Visualização da watchlist de outros usuários."
)
public class UserWatchlistController {

    private final WatchlistService watchlistService;
    private final MovieResponseMapper movieResponseMapper;
    private final AuthenticatedUser authenticatedUser;

    public UserWatchlistController(
            WatchlistService watchlistService,
            MovieResponseMapper movieResponseMapper,
            AuthenticatedUser authenticatedUser
    ) {
        this.watchlistService = watchlistService;
        this.movieResponseMapper = movieResponseMapper;
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping
    @Operation(
            summary = "Visualizar a watchlist de um usuário"
    )
    public ResponseEntity<PageResponse<WatchlistResponse>> findUserWatchlist(

            @Parameter(
                    description = "ID do usuário cuja watchlist será visualizada.",
                    example = "25"
            )
            @PathVariable
            @Positive
            Long userId,

            @Parameter(
                    description = "Filtra pelo status da watchlist.",
                    example = "WATCHED"
            )
            @RequestParam(required = false)
            WatchlistStatus status,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size
    ) {

        Long authenticatedUserId =
                authenticatedUser.getId();

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        Page<WatchlistResponse> result =
                watchlistService
                        .findAll(
                                userId,
                                status,
                                pageable
                        )
                        .map(this::toResponse);

        PageResponse<WatchlistResponse> response =
                new PageResponse<>(
                        result.getContent(),
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.isFirst(),
                        result.isLast()
                );

        return ResponseEntity.ok(response);
    }

    private WatchlistResponse toResponse(
            Watchlist watchlist
    ) {

        MovieResponse movie =
                movieResponseMapper.toResponse(
                        watchlist.getMovie()
                );

        return new WatchlistResponse(
                movie,
                watchlist.getStatus(),
                watchlist.getCreatedAt()
        );
    }
}