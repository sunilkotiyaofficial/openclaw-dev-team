package com.example.knowledgehub.security;

import com.example.knowledgehub.repository.jpa.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 6 configuration — JWT-based stateless authentication
 * with Role-Based Authorization.
 *
 * <p><b>Key design decisions:</b></p>
 * <ul>
 *   <li>{@code SessionCreationPolicy.STATELESS} — no HTTP session; auth state in JWT</li>
 *   <li>CSRF disabled — only relevant for cookie-based sessions</li>
 *   <li>JWT filter inserted BEFORE UsernamePasswordAuthenticationFilter</li>
 *   <li>{@code @EnableMethodSecurity} — enables {@code @PreAuthorize} on methods</li>
 *   <li>BCrypt for password hashing (12 rounds — production-safe)</li>
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
    private final UserRepository userRepository;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          UserRepository userRepository) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userRepository = userRepository;
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

                // Wire our DAO authentication provider (uses UserDetailsService + PasswordEncoder)
                .authenticationProvider(authenticationProvider())

                // Insert JWT filter BEFORE Spring's standard form login filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Custom UserDetailsService — looks up users by username from JPA repository.
     * Spring Security calls this during authentication.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    /**
     * BCrypt password encoder — strength 12.
     *
     * <p>Strength = work factor (2^12 iterations). Higher = more secure but slower.
     * 12 is the production recommendation as of 2024 — takes ~250ms per hash on
     * modern hardware, infeasible for offline brute-force.</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * AuthenticationProvider — wires UserDetailsService + PasswordEncoder
     * for Spring Security to authenticate username/password against DB.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /** Exposes the AuthenticationManager bean — needed by AuthController. */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
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
