package io.mero.app.domain.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.jwt.JwkProvider;
import io.mero.app.global.util.ClaimUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.PublicKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleAuthService {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    @Value("${apple.bundle-id}")
    private String bundleId;

    private final JwkProvider jwkProvider;

    public AppleClaims validate(String identityToken) {
        PublicKey publicKey = jwkProvider.getPublicKeyFor(APPLE_KEYS_URL, identityToken);

        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadRequestException("Apple 토큰 검증에 실패했습니다");
        }

        if (!APPLE_ISSUER.equals(claims.getIssuer())) {
            throw new BadRequestException("유효하지 않은 Apple 토큰입니다");
        }
        if (!claims.getAudience().contains(bundleId)) {
            throw new BadRequestException("유효하지 않은 Apple 토큰입니다");
        }

        // Apple은 최초 로그인에만 email을 내려주므로, 없는 것은 정상이다.
        // 다만 email이 있다면 그 값으로 기존 계정에 연결될 수 있으므로 인증 여부를 반드시 확인한다.
        String email = claims.get("email", String.class);
        if (email != null && !ClaimUtils.readBoolean(claims, "email_verified")) {
            log.warn("이메일이 인증되지 않은 Apple 계정의 로그인 시도: sub={}", claims.getSubject());
            throw new BadRequestException("이메일이 인증되지 않은 Apple 계정입니다");
        }

        return new AppleClaims(claims.getSubject(), email);
    }

    public record AppleClaims(String appleUserId, String email) {}
}
