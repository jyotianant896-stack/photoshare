package com.trizen.photoshare.security;

import com.trizen.photoshare.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues two kinds of token:
 *  - access tokens for logged in staff (subject = email, type = access)
 *  - gallery tokens handed to a customer after a correct PIN (subject = slug, type = gallery)
 * Keeping them apart means a gallery token can never be replayed against the staff API.
 */
@Service
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_GALLERY = "gallery";

    private static final String CLAIM_TYPE = "typ";

    private final SecretKey key;
    private final AppProperties properties;

    public JwtService(AppProperties properties) {
        this.properties = properties;
        byte[] secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 characters; set it through the environment");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public String issueAccessToken(AppUserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(properties.getJwt().getAccessTokenMinutes()));
        return Jwts.builder()
                .subject(principal.getEmail())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim("uid", principal.getId())
                .claim("role", principal.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public String issueGalleryToken(String slug) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(properties.getJwt().getGalleryTokenMinutes()));
        return Jwts.builder()
                .subject(slug)
                .claim(CLAIM_TYPE, TYPE_GALLERY)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public Instant accessTokenExpiry() {
        return Instant.now().plus(Duration.ofMinutes(properties.getJwt().getAccessTokenMinutes()));
    }

    public long galleryTokenSeconds() {
        return Duration.ofMinutes(properties.getJwt().getGalleryTokenMinutes()).toSeconds();
    }

    /** Returns the claims when the token is valid and of the expected type, otherwise null. */
    public Claims parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                return null;
            }
            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}
