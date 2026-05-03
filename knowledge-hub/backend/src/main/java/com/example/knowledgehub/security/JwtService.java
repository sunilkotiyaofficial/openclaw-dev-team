package com.example.knowledgehub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JWT generation, parsing, and validation service.
 *
 * <p>Uses {@code jjwt} library 0.12.x (current API).
 * Algorithm: HS256 (HMAC-SHA256) — symmetric key. For inter-service
 * verification, prefer asymmetric (RS256) where issuer signs with private
 * key and consumers verify with public key.</p>
 *
 * <p><b>Interview talking point — JWT structure:</b></p>
 * <blockquote>
 * "JWT has three Base64-encoded parts separated by dots:
 * header.payload.signature.
 * <ul>
 *   <li>Header: algorithm + token type</li>
 *   <li>Payload (claims): subject, issuer, expiration, custom claims like roles</li>
 *   <li>Signature: HMAC or RSA over header+payload using the secret/private key</li>
 * </ul>
 * The signature is what makes JWTs tamper-proof. Anyone can read the
 * payload (it's just Base64 — never put secrets in it), but only someone
 * with the signing key can produce a valid signature."
 * </blockquote>
 *
 * <p><b>Interview talking point — access vs refresh tokens:</b></p>
 * <blockquote>
 * "Access tokens are short-lived (15 min - 1h), sent on every request.
 * Refresh tokens are long-lived (7-30 days), used only to get new access
 * tokens. The split limits blast radius — if access token leaks, it
 * expires soon. If refresh token leaks, you can revoke it server-side
 * via a denylist (or rotate refresh tokens on each use)."
 * </blockquote>
 */
@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.access-token.expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${security.jwt.refresh-token.expiration-ms}")
    private long refreshTokenExpirationMs;

    // ═══ Token generation ═══════════════════════════════════════════════

    public String generateAccessToken(UserDetails userDetails) {
        // Custom claim — embed roles for client-side display + RBAC short-circuit
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("roles", userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList()));
        return buildToken(extraClaims, userDetails, accessTokenExpirationMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshTokenExpirationMs);
    }

    /**
     * Build a JWT using the jjwt 0.12.x fluent API.
     *
     * <p>Notice the modern API:
     * <ul>
     *   <li>{@code .claims(map)} replaces {@code .setClaims(map)}</li>
     *   <li>{@code .subject(s)} replaces {@code .setSubject(s)}</li>
     *   <li>{@code .issuedAt()}, {@code .expiration()}, {@code .issuer()}</li>
     *   <li>{@code .signWith(key, Jwts.SIG.HS256)} replaces {@code SignatureAlgorithm.HS256}</li>
     * </ul>
     * </p>
     */
    private String buildToken(Map<String, Object> extraClaims,
                              UserDetails userDetails,
                              long expirationMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .issuer("knowledge-hub")
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // ═══ Token parsing & validation ═════════════════════════════════════

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse and validate a token using jjwt 0.12.x API.
     *
     * <p>The new flow:
     * <ul>
     *   <li>{@code Jwts.parser()} replaces {@code Jwts.parserBuilder()}</li>
     *   <li>{@code .verifyWith(key)} replaces {@code .setSigningKey(key)}</li>
     *   <li>{@code .parseSignedClaims(token)} replaces {@code .parseClaimsJws(token)}</li>
     *   <li>{@code .getPayload()} replaces {@code .getBody()}</li>
     * </ul>
     * Will throw if signature invalid, expired, or malformed.
     * </p>
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            // Any parsing error (bad signature, malformed, expired) → invalid
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Derive the HMAC signing key from the Base64-encoded secret.
     *
     * <p>Returns SecretKey (modern type) instead of generic Key —
     * matches jjwt 0.12.x API expectations.</p>
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
