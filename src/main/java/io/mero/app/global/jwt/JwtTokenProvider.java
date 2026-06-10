package io.mero.app.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(Long userId) {
        return createToken(userId, TOKEN_TYPE_ACCESS, accessTokenValidity);
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(Long userId) {
        return createToken(userId, TOKEN_TYPE_REFRESH, refreshTokenValidity);
    }

    private String createToken(Long userId, String type, long validityMillis) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityMillis);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TOKEN_TYPE, type)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 userId 추출
     */
    public Long getUserIdFrom(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Access Token 유효성 검증 (서명·만료·타입)
     */
    public boolean validateAccessToken(String token) {
        return validate(token, TOKEN_TYPE_ACCESS);
    }

    /**
     * Refresh Token 유효성 검증 (서명·만료·타입)
     */
    public boolean validateRefreshToken(String token) {
        return validate(token, TOKEN_TYPE_REFRESH);
    }

    private boolean validate(String token, String expectedType) {
        try {
            Claims claims = parseClaims(token);
            String actualType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!expectedType.equals(actualType)) {
                log.debug("JWT token type mismatch: expected={}, actual={}", expectedType, actualType);
                return false;
            }
            return true;
        } catch (JwtException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
