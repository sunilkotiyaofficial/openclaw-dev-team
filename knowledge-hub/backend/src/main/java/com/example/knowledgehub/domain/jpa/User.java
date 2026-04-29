package com.example.knowledgehub.domain.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User entity — implements Spring Security's {@link UserDetails}.
 *
 * <p>This single class serves three roles:</p>
 * <ol>
 *   <li>JPA entity (persisted to {@code users} table)</li>
 *   <li>Spring Security principal (returned by {@code UserDetailsService})</li>
 *   <li>Application domain object (used by controllers/services)</li>
 * </ol>
 *
 * <p><b>Interview talking point — why implement UserDetails directly?</b></p>
 * <blockquote>
 * "Implementing UserDetails on the entity removes the need for a separate
 * adapter class. Trade-off: it couples your domain model to Spring Security.
 * For pure domain purity, use a {@code CustomUserDetails} adapter that
 * wraps the User. For most apps, direct implementation is pragmatic
 * and clear."
 * </blockquote>
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_user_username", columnNames = "username")
})
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @NotBlank
    @Email
    @Column(nullable = false, length = 255, unique = true)
    private String email;

    /** BCrypt-hashed password — never store plaintext. */
    @NotBlank
    @Column(nullable = false, length = 60)  // BCrypt hash is exactly 60 chars
    private String password;

    /**
     * Many-to-many — a user can have multiple roles.
     * EAGER fetch is acceptable here because every auth check needs roles
     * (and the count per user is small).
     */
    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", length = 30)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean accountNonLocked = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (roles.isEmpty()) {
            roles.add(Role.USER);  // default role if none specified
        }
    }

    // ─── UserDetails implementation ───────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Convert Role enum set to Spring Security authorities
        // Each role becomes "ROLE_USER", "ROLE_ADMIN", etc.
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority(r.authority()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return accountNonLocked; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }

    // ─── Standard getters/setters ─────────────────────────────────────

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    public void addRole(Role role) { this.roles.add(role); }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setAccountNonLocked(boolean v) { this.accountNonLocked = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant t) { this.lastLoginAt = t; }
}
