package com.example.knowledgehub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JWT generation, parsing, and validation service.
 *
 * <p>Uses {@code jjwt} library (the de-facto Java JWT library).
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

    private String buildToken(Map<String, Object> extraClaims,
                              UserDetails userDetails,
                              long expirationMs) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .setIssuer("knowledge-hub")
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
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

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)        // throws if signature invalid
                .getBody();
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

    private Key getSigningKey() {
        // Decode Base64-encoded secret to byte array, build HMAC key
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
