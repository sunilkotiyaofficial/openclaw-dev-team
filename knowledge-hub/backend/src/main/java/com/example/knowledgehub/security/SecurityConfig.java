package com.example.knowledgehub.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 6 filter chain configuration.
 *
 * <p><b>Stripped down responsibility</b>: This class only configures the
 * security filter chain + CORS. Authentication beans (UserDetailsService,
 * PasswordEncoder, AuthenticationProvider, AuthenticationManager) live in
 * {@link ApplicationConfig} to avoid circular dependencies.</p>
 *
 * <p><b>Key design decisions:</b></p>
 * <ul>
 *   <li>{@code SessionCreationPolicy.STATELESS} — no HTTP session; auth state in JWT</li>
 *   <li>CSRF disabled — only relevant for cookie-based sessions</li>
 *   <li>JWT filter inserted BEFORE UsernamePasswordAuthenticationFilter</li>
 *   <li>{@code @EnableMethodSecurity} — enables {@code @PreAuthorize} on methods</li>
 * </ul>
 *
 * <p><b>Interview talking point — stateless vs stateful:</b></p>
 * <blockquote>
 * "Stateless JWT = horizontal scalability without sticky sessions.
 * No session store needed; any node can validate any token. Trade-off:
 * can't invalidate a JWT before expiry without a denylist (Redis).
 * For most APIs, short-lived access tokens (15 min) make this acceptable."
 * </blockquote>
 */
@Configuration
@EnableMethodSecurity  // enables @PreAuthorize, @PostAuthorize, @Secured
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          AuthenticationProvider authenticationProvider) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — not needed for JWT (no cookies)
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS with our config bean
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ─── Authorization rules ────────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no auth required
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/prometheus").permitAll()  // for metrics scraping

                        // Read-only — any authenticated user (USER, EDITOR, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/topics/**", "/api/notes/**", "/api/resources/**")
                            .hasAnyRole("USER", "EDITOR", "ADMIN")

                        // Write — EDITOR or ADMIN only
                        .requestMatchers(HttpMethod.POST, "/api/topics/**", "/api/notes/**", "/api/resources/**")
                            .hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/topics/**", "/api/notes/**", "/api/resources/**")
                            .hasAnyRole("EDITOR", "ADMIN")

                        // Delete — ADMIN only
                        .requestMatchers(HttpMethod.DELETE, "/api/**")
                            .hasRole("ADMIN")

                        // Anything else — must be authenticated
                        .anyRequest().authenticated()
                )

                // Stateless — every request must carry credentials (JWT)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Wire our DAO authentication provider (from ApplicationConfig)
                .authenticationProvider(authenticationProvider)

                // Insert JWT filter BEFORE Spring's standard form login filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** CORS — allows React frontend (localhost:3000) to call this API. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
