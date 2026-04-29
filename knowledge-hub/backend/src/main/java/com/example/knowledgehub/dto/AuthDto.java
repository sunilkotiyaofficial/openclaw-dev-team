package com.example.knowledgehub.dto;

import com.example.knowledgehub.domain.jpa.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

/**
 * Auth request/response DTOs — Java records (immutable, concise).
 *
 * <p>Records are perfect for DTOs:</p>
 * <ul>
 *   <li>Immutable by default</li>
 *   <li>Auto-generated equals/hashCode/toString</li>
 *   <li>No setters (consistent with API boundaries)</li>
 *   <li>Pattern matching support (Java 21)</li>
 * </ul>
 */
public class AuthDto {

    /** Sign-up request. */
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100)
            @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).+$",
                message = "Password must contain upper, lower, and digit")
            String password,
            Set<Role> requestedRoles  // optional — defaults to USER
    ) {}

    /** Login request. */
    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    /** Auth response — returns access + refresh tokens. */
    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,           // always "Bearer"
            long expiresInSeconds,
            UserInfo user
    ) {}

    /** Public user info (no password!) — embedded in AuthResponse. */
    public record UserInfo(
            Long id,
            String username,
            String email,
            Set<Role> roles,
            Instant lastLoginAt
    ) {}

    /** Refresh token request. */
    public record RefreshTokenRequest(
            @NotBlank String refreshToken
    ) {}
}
