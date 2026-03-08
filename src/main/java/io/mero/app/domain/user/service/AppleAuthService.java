package io.mero.app.domain.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.mero.app.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AppleAuthService {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    @Value("${apple.bundle-id}")
    private String bundleId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AppleClaims validate(String identityToken) {
        try {
            // 1. JWT 헤더에서 kid 추출
            String[] parts = identityToken.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            JsonNode header = objectMapper.readTree(headerJson);
            String kid = header.get("kid").asText();

            // 2. Apple 공개키 목록 조회
            String keysJson = restTemplate.getForObject(APPLE_KEYS_URL, String.class);
            JsonNode keys = objectMapper.readTree(keysJson).get("keys");

            // 3. kid 일치하는 키 탐색
            JsonNode matchingKey = null;
            for (JsonNode key : keys) {
                if (kid.equals(key.get("kid").asText())) {
                    matchingKey = key;
                    break;
                }
            }
            if (matchingKey == null) {
                throw new BadRequestException("Apple 공개키를 찾을 수 없습니다");
            }

            // 4. RSA 공개키 생성
            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(matchingKey.get("n").asText()));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(matchingKey.get("e").asText()));
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));

            // 5. JWT 검증 및 클레임 추출
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();

            // 6. issuer, audience 검증
            if (!APPLE_ISSUER.equals(claims.getIssuer())) {
                throw new BadRequestException("유효하지 않은 Apple 토큰입니다");
            }
            if (!claims.getAudience().contains(bundleId)) {
                throw new BadRequestException("유효하지 않은 Apple 토큰입니다");
            }

            return new AppleClaims(claims.getSubject(), claims.get("email", String.class));

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Apple 토큰 검증에 실패했습니다");
        }
    }

    public record AppleClaims(String appleUserId, String email) {}
}
