package io.mero.app.global.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * <p>키는 kid로 캐시하고, 모르는 kid를 만나면 키 로테이션으로 보고 다시 받아온다.
 * 만료 시간을 따로 두지 않는 이유는, 로테이션이야말로 키가 바뀌는 유일한 경우라 이 경로가
 * 이미 그것을 처리하기 때문이다. 다만 존재하지 않는 kid를 반복 전송해 JWKS 조회를 계속
 * 유발할 수 있으므로, 같은 URL에 대한 재조회에는 최소 간격을 둔다.
 */
@Slf4j
@Component
public class JwkProvider {

    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofMinutes(1);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Duration minRefreshInterval;
    private final Map<String, JwkSet> cache = new ConcurrentHashMap<>();

    @Autowired
    public JwkProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this(restTemplate, objectMapper, MIN_REFRESH_INTERVAL);
    }

    JwkProvider(RestTemplate restTemplate, ObjectMapper objectMapper, Duration minRefreshInterval) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.minRefreshInterval = minRefreshInterval;
    }

    /**
     * ID 토큰 헤더의 kid에 해당하는 공개키를 반환한다.
     *
     * @param jwksUrl 제공자의 JWKS 엔드포인트
     * @param token   검증할 ID 토큰
     */
    public PublicKey getPublicKeyFor(String jwksUrl, String token) {
        String kid = extractKid(token);

        JwkSet cached = cache.get(jwksUrl);
        if (cached != null) {
            PublicKey key = cached.keys().get(kid);
            if (key != null) {
                return key;
            }
            if (!cached.isRefreshable(minRefreshInterval)) {
                throw new BadRequestException("인증 서버의 공개키를 찾을 수 없습니다");
            }
            log.info("JWKS 캐시에 없는 kid: {}. 키 로테이션으로 보고 갱신한다", kid);
        }

        PublicKey key = refresh(jwksUrl).get(kid);
        if (key == null) {
            throw new BadRequestException("인증 서버의 공개키를 찾을 수 없습니다");
        }
        return key;
    }

    private String extractKid(String token) {
        try {
            String[] parts = token.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            return objectMapper.readTree(headerJson).get("kid").asText();
        } catch (Exception e) {
            throw new BadRequestException("유효하지 않은 토큰입니다");
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
        cache.put(jwksUrl, new JwkSet(keys, Instant.now()));
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

    private record JwkSet(Map<String, PublicKey> keys, Instant fetchedAt) {
        boolean isRefreshable(Duration minInterval) {
            return Duration.between(fetchedAt, Instant.now()).compareTo(minInterval) >= 0;
        }
    }
}
