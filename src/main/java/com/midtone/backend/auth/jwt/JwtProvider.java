package com.midtone.backend.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String CLAIM_TOKEN_TYPE = "type";

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.access-token-expiration}") Duration accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration}") Duration refreshTokenExpiration
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = issuer;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String createAccessToken(long userId) {
        return createToken(userId, TokenType.ACCESS, accessTokenExpiration);
    }

    public String createRefreshToken(long userId) {
        return createToken(userId, TokenType.REFRESH, refreshTokenExpiration);
    }

    public Duration getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public Duration getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public Instant getIssuedAt(String token) {
        return parseClaims(token).getIssuedAt().toInstant();
    }

    public TokenType getTokenType(String token) {
        String type = parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class);
        return TokenType.valueOf(type);
    }

    public boolean isAccessToken(String token) {
        return getTokenType(token) == TokenType.ACCESS;
    }

    public boolean isRefreshToken(String token) {
        return getTokenType(token) == TokenType.REFRESH;
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String createToken(long userId, TokenType tokenType, Duration expiration) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration.toMillis());

        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim(CLAIM_TOKEN_TYPE, tokenType.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
