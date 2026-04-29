package com.example.knowledgehub.controller;

import com.example.knowledgehub.domain.jpa.Role;
import com.example.knowledgehub.domain.jpa.User;
import com.example.knowledgehub.dto.AuthDto.*;
import com.example.knowledgehub.repository.jpa.UserRepository;
import com.example.knowledgehub.security.JwtService;
import com.example.knowledgehub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication endpoints — register, login, refresh.
 *
 * <p><b>Endpoint summary:</b></p>
 * <ul>
 *   <li>POST /api/auth/register — sign up new user</li>
 *   <li>POST /api/auth/login — exchange username/password for JWT</li>
 *   <li>POST /api/auth/refresh — exchange refresh token for new access token</li>
 *   <li>POST /api/auth/users/{id}/roles/{role} — admin-only: promote user</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService,
                          UserService userService,
                          UserRepository userRepository,
                          JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * Sign up — creates a new user, returns JWT.
     *
     * <p>Auto-login after registration: the response includes access+refresh
     * tokens so the client can immediately make authenticated requests.</p>
     */
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                900L,  // 15 min in seconds (matches application.yml)
                toUserInfo(user));
    }

    /**
     * Login — verify credentials, issue JWT tokens.
     *
     * <p>The {@code authenticationManager.authenticate(...)} call:
     * <ol>
     *   <li>Looks up user via UserDetailsService (DB query)</li>
     *   <li>Compares password using BCryptPasswordEncoder</li>
     *   <li>Throws AuthenticationException on bad credentials (returned as 401)</li>
     * </ol>
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        // Spring Security validates username + password against DB
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()));

        // If we got here, auth succeeded
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();

        // Update last-login timestamp
        userService.recordLogin(user.getUsername());

        // Issue tokens
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(accessToken, refreshToken, "Bearer", 900L, toUserInfo(user));
    }

    /**
     * Refresh access token using a valid refresh token.
     *
     * <p>Production extension: rotate refresh tokens on each use, store
     * in Redis with TTL, support revocation.</p>
     */
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String username = jwtService.extractUsername(request.refreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(request.refreshToken(), userDetails)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        User user = userRepository.findByUsername(username).orElseThrow();
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer", 900L, toUserInfo(user));
    }

    /**
     * Admin-only: assign a role to a user.
     *
     * <p>{@code @PreAuthorize} runs BEFORE the method — denied requests
     * never enter the method body. Note: SpEL expression evaluates against
     * the current authenticated user.</p>
     */
    @PostMapping("/users/{userId}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")  // method-level RBAC
    public UserInfo assignRole(@PathVariable Long userId, @PathVariable Role role) {
        User updated = userService.addRole(userId, role);
        return toUserInfo(updated);
    }

    private UserInfo toUserInfo(User user) {
        return new UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                Set.copyOf(user.getRoles()),
                user.getLastLoginAt());
    }
}
