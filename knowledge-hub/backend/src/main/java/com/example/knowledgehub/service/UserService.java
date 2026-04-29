package com.example.knowledgehub.service;

import com.example.knowledgehub.domain.jpa.Role;
import com.example.knowledgehub.domain.jpa.User;
import com.example.knowledgehub.dto.AuthDto.RegisterRequest;
import com.example.knowledgehub.repository.jpa.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user. Validates uniqueness, hashes password, assigns roles.
     *
     * <p><b>Security note:</b> By default, new users get USER role only.
     * EDITOR and ADMIN roles must be assigned by an existing admin via
     * a separate endpoint (not via self-signup).</p>
     */
    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());

        // ALWAYS hash before persisting — never store plaintext
        user.setPassword(passwordEncoder.encode(request.password()));

        // Determine roles: trust user input only for USER role; ignore others on signup
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);   // default

        // SECURITY: only allow privileged roles if explicitly granted by admin elsewhere.
        // For this demo, accept requestedRoles BUT in production you'd authorize this.
        if (request.requestedRoles() != null) {
            request.requestedRoles().stream()
                    .filter(r -> r == Role.USER)  // demo: ignore EDITOR/ADMIN on signup
                    .forEach(roles::add);
        }
        user.setRoles(roles);

        User saved = userRepository.save(user);
        log.info("Registered user id={} username={} roles={}",
                saved.getId(), saved.getUsername(), saved.getRoles());
        return saved;
    }

    /** Update last-login timestamp — called from auth controller after successful auth. */
    @Transactional
    public void recordLogin(String username) {
        userRepository.findByUsername(username).ifPresent(u -> {
            u.setLastLoginAt(Instant.now());
            userRepository.save(u);
        });
    }

    /**
     * Admin-only — promote a user to a new role.
     * Authorization handled at controller layer via @PreAuthorize.
     */
    @Transactional
    public User addRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.addRole(role);
        return userRepository.save(user);
    }
}
