package br.com.watchusee.watchusee.watchlist.api;

import br.com.watchusee.watchusee.movie.api.dto.MovieResponse;
import br.com.watchusee.watchusee.movie.mapper.MovieResponseMapper;
import br.com.watchusee.watchusee.shared.security.AuthenticatedUser;
import br.com.watchusee.watchusee.watchlist.api.dto.PageResponse;
import br.com.watchusee.watchusee.watchlist.api.dto.UpdateWatchlistRequest;
import br.com.watchusee.watchusee.watchlist.api.dto.WatchlistResponse;
import br.com.watchusee.watchusee.watchlist.domain.Watchlist;
import br.com.watchusee.watchusee.watchlist.domain.WatchlistStatus;
import br.com.watchusee.watchusee.watchlist.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/watchlist")
@Validated
@Tag(
        name = "Watchlist",
        description = "Gerenciamento dos filmes do usuário autenticado."
)
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final MovieResponseMapper movieResponseMapper;
    private final AuthenticatedUser authenticatedUser;

    public WatchlistController(
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
            summary = "Listar filmes da watchlist"
    )
    public ResponseEntity<PageResponse<WatchlistResponse>> findAll(

            @Parameter(
                    description = "Filtra pelo status da watchlist.",
                    example = "TO_WATCH"
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

        Long userId =
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

    @GetMapping("/{movieId}")
    @Operation(
            summary = "Consultar filme na watchlist"
    )
    public ResponseEntity<WatchlistResponse> get(
            @PathVariable @Positive Long movieId
    ) {

        Long userId =
                authenticatedUser.getId();

        Watchlist watchlist =
                watchlistService.get(
                        userId,
                        movieId
                );

        return ResponseEntity.ok(
                toResponse(watchlist)
        );
    }

    @PutMapping("/{movieId}")
    @Operation(
            summary = "Adicionar ou atualizar o status de um filme"
    )
    public ResponseEntity<WatchlistResponse> updateStatus(

            @PathVariable @Positive Long movieId,

            @RequestBody @Valid
            UpdateWatchlistRequest request

    ) {

        Long userId =
                authenticatedUser.getId();

        Watchlist watchlist =
                watchlistService.updateStatus(
                        userId,
                        movieId,
                        request.status()
                );

        return ResponseEntity.ok(
                toResponse(watchlist)
        );
    }

    @DeleteMapping("/{movieId}")
    @Operation(
            summary = "Remover filme da watchlist"
    )
    public ResponseEntity<Void> remove(
            @PathVariable @Positive Long movieId
    ) {

        Long userId =
                authenticatedUser.getId();

        watchlistService.remove(
                userId,
                movieId
        );

        return ResponseEntity
                .noContent()
                .build();
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