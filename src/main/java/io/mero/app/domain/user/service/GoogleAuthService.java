package io.mero.app.domain.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.jwt.JwkProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final String GOOGLE_KEYS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String PROVIDER_NAME = "Google";
    private static final Set<String> GOOGLE_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );

    @Value("${google.client-ids}")
    private List<String> clientIds;

    private final JwkProvider jwkProvider;

    public GoogleClaims validate(String idToken) {
        PublicKey publicKey = jwkProvider.getPublicKeyFor(PROVIDER_NAME, GOOGLE_KEYS_URL, idToken);

        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadRequestException("Google 토큰 검증에 실패했습니다");
        }

        if (!GOOGLE_ISSUERS.contains(claims.getIssuer())) {
            throw new BadRequestException("유효하지 않은 Google 토큰입니다");
        }
        if (claims.getAudience().stream().noneMatch(clientIds::contains)) {
            throw new BadRequestException("유효하지 않은 Google 토큰입니다");
        }

        String email = claims.get("email", String.class);
        Boolean emailVerified = claims.get("email_verified", Boolean.class);
        if (email == null || !Boolean.TRUE.equals(emailVerified)) {
            throw new BadRequestException("이메일이 인증되지 않은 Google 계정입니다");
        }

        return new GoogleClaims(claims.getSubject(), email, claims.get("picture", String.class));
    }

    public record GoogleClaims(String googleUserId, String email, String picture) {}
}
