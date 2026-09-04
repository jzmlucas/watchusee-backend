package br.com.watchusee.watchusee.watchlist.api;

import br.com.watchusee.watchusee.movie.api.dto.MovieResponse;
import br.com.watchusee.watchusee.movie.mapper.MovieResponseMapper;
import br.com.watchusee.watchusee.shared.security.AuthenticatedUser;
import br.com.watchusee.watchusee.watchlist.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watchlist")
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

    @PostMapping("/to-watch/{movieId}")
    @Operation(
            summary = "Adicionar filme à lista Assistir"
    )
    public ResponseEntity<Void> addToWatch(
            @PathVariable @Positive Long movieId
    ) {

        Long userId =
                authenticatedUser.getId();

        watchlistService.addToWatch(
                userId,
                movieId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping("/to-watch/{movieId}")
    @Operation(
            summary = "Remover filme da lista Assistir"
    )
    public ResponseEntity<Void> removeFromWatch(
            @PathVariable @Positive Long movieId
    ) {

        Long userId =
                authenticatedUser.getId();

        watchlistService.removeFromWatch(
                userId,
                movieId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping("/watched/{movieId}")
    @Operation(
            summary = "Marcar filme como Assistido"
    )
    public ResponseEntity<Void> markAsWatched(
            @PathVariable @Positive Long movieId
    ) {

        Long userId =
                authenticatedUser.getId();

        watchlistService.markAsWatched(
                userId,
                movieId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping("/watched/{movieId}")
    @Operation(
            summary = "Remover filme da lista Assistidos"
    )
    public ResponseEntity<Void> removeFromWatched(
            @PathVariable @Positive Long movieId
    ) {

        Long userId =
                authenticatedUser.getId();

        watchlistService.removeFromWatched(
                userId,
                movieId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @GetMapping("/to-watch")
    @Operation(
            summary = "Lista de filmes para assistir"
    )
    public ResponseEntity<List<MovieResponse>> getToWatch() {

        Long userId =
                authenticatedUser.getId();

        List<MovieResponse> response =
                watchlistService
                        .getToWatch(userId)
                        .stream()
                        .map(movieResponseMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/watched")
    @Operation(
            summary = "Lista de filmes assistidos"
    )
    public ResponseEntity<List<MovieResponse>> getWatched() {

        Long userId =
                authenticatedUser.getId();

        List<MovieResponse> response =
                watchlistService
                        .getWatched(userId)
                        .stream()
                        .map(movieResponseMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{movieId}/status")
    @Operation(
            summary = "Consulta o status do filme na watchlist"
    )
    public ResponseEntity<WatchlistStatusResponse> getStatus(
            @PathVariable @Positive Long movieId
    ) {

        Long userId =
                authenticatedUser.getId();

        return ResponseEntity.ok(
                new WatchlistStatusResponse(
                        movieId,
                        watchlistService.isToWatch(
                                userId,
                                movieId
                        ),
                        watchlistService.isWatched(
                                userId,
                                movieId
                        )
                )
        );
    }

    public record WatchlistStatusResponse(
            Long movieId,
            boolean toWatch,
            boolean watched
    ) {
    }
}