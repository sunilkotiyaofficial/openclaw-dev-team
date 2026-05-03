package com.example.knowledgehub.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the REST API.
 *
 * <p>Maps Java exceptions to consistent HTTP responses with structured
 * error bodies. All controllers in the application benefit automatically.</p>
 *
 * <p><b>Interview talking point — RFC 7807 Problem Details:</b></p>
 * <blockquote>
 * "Modern Spring (6+) supports RFC 7807 Problem Details for HTTP APIs —
 * standardized error response format with type, title, detail, and
 * instance fields. I prefer this over custom error JSON because clients
 * across the industry already understand it (it's the standard for
 * OAuth, JSON-API, OpenAPI). Spring auto-converts your exceptions to
 * ProblemDetail when you set spring.mvc.problemdetails.enabled=true."
 * </blockquote>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 404 — resource not found (custom exception) */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    /** 400 — Bean Validation failure on @RequestBody */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation failed",
                        fieldErrors));
    }

    /** 400 — Bean Validation failure on @PathVariable / @RequestParam */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    /** 400 — bad request (e.g., duplicate name) */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    /**
     * 401 — bad credentials, unknown user, or any AuthenticationException.
     *
     * <p><b>Why a SINGLE message ("Invalid username or password")?</b>
     * To avoid user enumeration attacks. Saying "user not found" lets an
     * attacker probe valid usernames; saying "wrong password" confirms
     * existence. Same opaque message regardless of cause.</p>
     */
    @ExceptionHandler({
            BadCredentialsException.class,
            UsernameNotFoundException.class,
            AuthenticationException.class
    })
    public ResponseEntity<ErrorResponse> handleAuthFailure(Exception ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED,
                        "Invalid username or password"));
    }

    /** 403 — authenticated but not authorized for this action. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN,
                        "You do not have permission to perform this action"));
    }

    /** 500 — catch-all (log full stack trace, hide internals from client) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal server error — see logs"));
    }

    /** Structured error response body. */
    public record ErrorResponse(
            Instant timestamp,
            int status,
            String message,
            Map<String, String> fieldErrors
    ) {
        public static ErrorResponse of(HttpStatus status, String message) {
            return new ErrorResponse(Instant.now(), status.value(), message, Map.of());
        }
    }
}
