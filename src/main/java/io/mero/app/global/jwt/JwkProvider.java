package io.mero.app.global.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 소셜 로그인 제공자의 JWKS 엔드포인트에서 RSA 공개키를 가져온다.
 *
 * <p>공개키는 자주 바뀌지 않으므로 TTL 동안 메모리에 캐시한다. 캐시에 없는 kid를 만나면
 * 키 로테이션으로 보고 한 번만 강제로 다시 받아온다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwkProvider {

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, CachedJwkSet> cache = new ConcurrentHashMap<>();

    /**
     * ID 토큰 헤더의 kid에 해당하는 공개키를 반환한다.
     *
     * @param providerName 예외 메시지에 쓰이는 제공자 이름 (예: "Apple")
     * @param jwksUrl      제공자의 JWKS 엔드포인트
     * @param token        검증할 ID 토큰
     */
    public PublicKey getPublicKeyFor(String providerName, String jwksUrl, String token) {
        String kid = extractKid(providerName, token);

        CachedJwkSet cached = cache.get(jwksUrl);
        if (cached != null && !cached.isExpired()) {
            PublicKey key = cached.keys().get(kid);
            if (key != null) {
                return key;
            }
            log.info("{} JWKS 캐시에 없는 kid: {}. 키 로테이션으로 보고 갱신한다", providerName, kid);
        }

        PublicKey key = refresh(jwksUrl).get(kid);
        if (key == null) {
            throw new BadRequestException(providerName + " 공개키를 찾을 수 없습니다");
        }
        return key;
    }

    private String extractKid(String providerName, String token) {
        try {
            String[] parts = token.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            return objectMapper.readTree(headerJson).get("kid").asText();
        } catch (Exception e) {
            throw new BadRequestException("유효하지 않은 " + providerName + " 토큰입니다");
        }
    }

    private Map<String, PublicKey> refresh(String jwksUrl) {
        String keysJson;
        try {
            keysJson = restTemplate.getForObject(jwksUrl, String.class);
        } catch (RestClientException e) {
            throw new ExternalServiceException("인증 서버의 공개키를 가져오지 못했습니다", e);
        }

        Map<String, PublicKey> keys = parseKeys(keysJson);
        cache.put(jwksUrl, new CachedJwkSet(keys, Instant.now().plus(CACHE_TTL)));
        return keys;
    }

    private Map<String, PublicKey> parseKeys(String keysJson) {
        try {
            JsonNode keys = objectMapper.readTree(keysJson).get("keys");
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            Map<String, PublicKey> result = new HashMap<>();

            for (JsonNode key : keys) {
                if (!"RSA".equals(key.path("kty").asText())) {
                    continue;
                }
                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("n").asText()));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("e").asText()));
                result.put(key.get("kid").asText(),
                        keyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent)));
            }
            return result;
        } catch (Exception e) {
            throw new ExternalServiceException("인증 서버의 공개키를 해석하지 못했습니다", e);
        }
    }

    private record CachedJwkSet(Map<String, PublicKey> keys, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
