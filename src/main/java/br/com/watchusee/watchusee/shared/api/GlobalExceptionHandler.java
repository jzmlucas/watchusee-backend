package br.com.watchusee.watchusee.shared.api;

import br.com.watchusee.watchusee.movie.client.tmdb.exception.TmdbException;
import br.com.watchusee.watchusee.movie.client.tmdb.exception.TmdbMovieNotFoundException;
import br.com.watchusee.watchusee.share.exception.ShareRecipientNotFoundException;
import br.com.watchusee.watchusee.shared.api.dto.ErrorResponse;
import br.com.watchusee.watchusee.user.exception.AccountLockedException;
import br.com.watchusee.watchusee.user.exception.InvalidCredentialsException;
import br.com.watchusee.watchusee.user.exception.UserAlreadyExistsException;
import br.com.watchusee.watchusee.user.exception.UserNotFoundException;
import br.com.watchusee.watchusee.watchlist.exception.WatchlistItemNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {

        String message = exception.getConstraintViolations()
                .stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Parâmetro inválido.");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Dados inválidos.");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(TmdbMovieNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTmdbMovieNotFound(
            TmdbMovieNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(TmdbException.class)
    public ResponseEntity<ErrorResponse> handleTmdbException(
            TmdbException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(
            AccountLockedException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.LOCKED,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(ShareRecipientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleShareRecipientNotFound(
            ShareRecipientNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(WatchlistItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWatchlistItemNotFound(
            WatchlistItemNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Acesso negado.",
                request
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(
            Exception exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {

        log.error(
                "Erro não tratado ao processar {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno. Tente novamente mais tarde.",
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(errorResponse);
    }
}
