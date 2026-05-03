package com.example.knowledgehub.security;

import com.example.knowledgehub.repository.jpa.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Application-level beans for authentication.
 *
 * <p><b>Why separate from SecurityConfig?</b></p>
 * <blockquote>
 * "Spring's bean creation can hit circular dependencies when the same
 * config class defines beans that depend on filters that depend on those
 * beans. Splitting auth-component beans (UserDetailsService, PasswordEncoder,
 * AuthenticationProvider, AuthenticationManager) into their own
 * {@code ApplicationConfig} breaks the cycle. SecurityConfig is then free
 * to depend on JwtAuthenticationFilter without circular reference."
 * </blockquote>
 *
 * <p><b>Interview talking point — how to debug Spring circular dependencies:</b></p>
 * <blockquote>
 * "When Spring throws 'circular reference', three fixes in order:
 * <ol>
 *   <li>Refactor — split the bean responsibilities into separate config classes (preferred)</li>
 *   <li>Use {@code @Lazy} on one of the constructor params — defers proxy creation</li>
 *   <li>Setter injection instead of constructor injection (last resort — breaks immutability)</li>
 * </ol>
 * Constructor injection forces you to think about dependencies upfront —
 * the circular error is actually helpful, surfacing design issues early."
 * </blockquote>
 */
@Configuration
public class ApplicationConfig {

    private final UserRepository userRepository;

    public ApplicationConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
