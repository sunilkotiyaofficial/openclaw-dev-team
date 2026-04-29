package com.example.knowledgehub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that extracts and validates JWT from Authorization header on every request.
 *
 * <p>Runs ONCE per request (extends {@link OncePerRequestFilter}) BEFORE
 * Spring Security's standard authentication filters. If a valid JWT is found,
 * sets the {@code SecurityContextHolder} so downstream filters/controllers see
 * an authenticated user.</p>
 *
 * <p><b>Interview talking point — filter chain order:</b></p>
 * <blockquote>
 * "Spring Security uses a chain of filters. JwtAuthenticationFilter runs
 * before UsernamePasswordAuthenticationFilter — we authenticate from the
 * token, skipping form-based auth. If the JWT is missing or invalid, we
 * just pass through without setting auth — later filters (or
 * AuthenticationEntryPoint) handle the 401."
 * </blockquote>
 *
 * <p><b>Why OncePerRequestFilter:</b></p>
 * <blockquote>
 * "Servlet specs allow filters to run multiple times per request via
 * forwards/includes. OncePerRequestFilter ensures we only validate the
 * JWT once per HTTP request, regardless of internal dispatches."
 * </blockquote>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                    UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract Authorization header
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);  // no token — let downstream handle
            return;
        }

        // 2. Extract JWT (skip "Bearer " prefix)
        String jwt = authHeader.substring(BEARER_PREFIX.length());

        try {
            String username = jwtService.extractUsername(jwt);

            // 3. If we have a username AND no existing auth in context, validate
            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // 4. Build authentication object with user's authorities (roles)
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,                          // credentials — already validated via JWT
                                    userDetails.getAuthorities()); // roles for @PreAuthorize

                    authToken.setDetails(new WebAuthenticationDetailsSource()
                            .buildDetails(request));

                    // 5. Set the authentication in the security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Invalid token — log but don't fail the request here
            logger.warn("JWT authentication failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
