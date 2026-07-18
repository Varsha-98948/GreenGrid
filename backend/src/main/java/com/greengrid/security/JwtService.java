package com.greengrid.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Issues short-lived access tokens and longer-lived refresh tokens, and
 * validates them on every incoming request via {@link JwtAuthFilter}.
 * Access and refresh tokens are distinguished by a "type" claim so a
 * refresh token can never be replayed against a protected endpoint.
 */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email) {
        return buildToken(userId, email, TYPE_ACCESS, properties.accessTokenTtlMinutes(), ChronoUnit.MINUTES);
    }

    public String generateRefreshToken(UUID userId, String email) {
        return buildToken(userId, email, TYPE_REFRESH, properties.refreshTokenTtlDays(), ChronoUnit.DAYS);
    }

    private String buildToken(UUID userId, String email, String type, long amount, ChronoUnit unit) {
        Instant now = Instant.now();
        Instant expiry = now.plus(amount, unit);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public UUID validateAccessTokenAndGetUserId(String token) {
        Claims claims = parseClaims(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Token is not an access token");
        }
        return UUID.fromString(claims.getSubject());
    }

    public UUID validateRefreshTokenAndGetUserId(String token) {
        Claims claims = parseClaims(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Token is not a refresh token");
        }
        return UUID.fromString(claims.getSubject());
    }

    public String getEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    private static final String TYPE_OAUTH_STATE = "oauth_state";

    /**
     * Builds the short-lived, signed "state" parameter passed through the
     * GitHub OAuth redirect. Signing it prevents CSRF/state-tampering, and
     * embedding the acting user's id lets the callback know *which*
     * already-authenticated GreenGrid user is connecting GitHub — the
     * browser redirect carries no Authorization header, so this is the
     * only safe way to correlate the callback back to a session.
     * A null userId means "this is a sign-in-with-GitHub attempt", not a connect.
     */
    public String generateOAuthStateToken(UUID actingUserId) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .claim(CLAIM_TYPE, TYPE_OAUTH_STATE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(10, ChronoUnit.MINUTES)));
        if (actingUserId != null) {
            builder.subject(actingUserId.toString());
        }
        return builder.signWith(key).compact();
    }

    public java.util.Optional<UUID> validateOAuthStateAndGetUserId(String state) {
        Claims claims = parseClaims(state);
        if (!TYPE_OAUTH_STATE.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Invalid OAuth state token");
        }
        String subject = claims.getSubject();
        return subject == null ? java.util.Optional.empty() : java.util.Optional.of(UUID.fromString(subject));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
