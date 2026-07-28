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
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final String GOOGLE_KEYS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );

    @Value("${google.client-ids}")
    private List<String> clientIds;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleClaims validate(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            JsonNode header = objectMapper.readTree(headerJson);
            String kid = header.get("kid").asText();

            String keysJson = restTemplate.getForObject(GOOGLE_KEYS_URL, String.class);
            JsonNode keys = objectMapper.readTree(keysJson).get("keys");

            JsonNode matchingKey = null;
            for (JsonNode key : keys) {
                if (kid.equals(key.get("kid").asText())) {
                    matchingKey = key;
                    break;
                }
            }
            if (matchingKey == null) {
                throw new BadRequestException("Google 공개키를 찾을 수 없습니다");
            }

            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(matchingKey.get("n").asText()));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(matchingKey.get("e").asText()));
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));

            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();

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

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Google 토큰 검증에 실패했습니다");
        }
    }

    public record GoogleClaims(String googleUserId, String email, String picture) {}
}
