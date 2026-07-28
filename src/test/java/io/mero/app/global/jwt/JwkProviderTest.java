package io.mero.app.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwkProviderTest {

    private static final String JWKS_URL = "https://example.com/keys";

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private JwkProvider jwkProvider;

    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }

    @Test
    @DisplayName("공개키 조회 성공 - JWKS의 kid와 일치하는 키를 반환한다")
    void 공개키_조회_성공() {
        // given
        given(restTemplate.getForObject(JWKS_URL, String.class)).willReturn(jwks("kid-1", keyPair));

        // when
        PublicKey key = jwkProvider.getPublicKeyFor("Test", JWKS_URL, token("kid-1", keyPair));

        // then
        assertThat(key).isEqualTo(keyPair.getPublic());
    }

    @Test
    @DisplayName("두 번째 조회는 캐시를 사용해 JWKS를 다시 받아오지 않는다")
    void 캐시_적중_시_재조회하지_않음() {
        // given
        given(restTemplate.getForObject(JWKS_URL, String.class)).willReturn(jwks("kid-1", keyPair));
        String token = token("kid-1", keyPair);

        // when
        jwkProvider.getPublicKeyFor("Test", JWKS_URL, token);
        jwkProvider.getPublicKeyFor("Test", JWKS_URL, token);
        jwkProvider.getPublicKeyFor("Test", JWKS_URL, token);

        // then
        verify(restTemplate, times(1)).getForObject(JWKS_URL, String.class);
    }

    @Test
    @DisplayName("캐시에 없는 kid를 만나면 키 로테이션으로 보고 JWKS를 다시 받아온다")
    void 모르는_kid는_JWKS를_갱신한다() throws Exception {
        // given
        KeyPair rotated = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        given(restTemplate.getForObject(JWKS_URL, String.class))
                .willReturn(jwks("kid-1", keyPair), jwks("kid-2", rotated));

        // when
        jwkProvider.getPublicKeyFor("Test", JWKS_URL, token("kid-1", keyPair));
        PublicKey key = jwkProvider.getPublicKeyFor("Test", JWKS_URL, token("kid-2", rotated));

        // then
        assertThat(key).isEqualTo(rotated.getPublic());
        verify(restTemplate, times(2)).getForObject(JWKS_URL, String.class);
    }

    @Test
    @DisplayName("갱신 후에도 kid가 없으면 BadRequestException")
    void 갱신_후에도_없는_kid는_예외() {
        // given
        given(restTemplate.getForObject(JWKS_URL, String.class)).willReturn(jwks("kid-1", keyPair));

        // when & then
        assertThatThrownBy(() -> jwkProvider.getPublicKeyFor("Test", JWKS_URL, token("unknown-kid", keyPair)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("공개키를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("JWKS 조회 실패는 클라이언트 오류가 아니라 ExternalServiceException")
    void JWKS_조회_실패는_외부서비스_예외() {
        // given
        given(restTemplate.getForObject(JWKS_URL, String.class))
                .willThrow(new ResourceAccessException("connection refused"));

        // when & then
        assertThatThrownBy(() -> jwkProvider.getPublicKeyFor("Test", JWKS_URL, token("kid-1", keyPair)))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("공개키를 가져오지 못했습니다");
    }

    @Test
    @DisplayName("kid 없는 토큰은 BadRequestException")
    void kid_없는_토큰은_예외() {
        // given
        String tokenWithoutKid = Jwts.builder()
                .subject("user")
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        // when & then
        assertThatThrownBy(() -> jwkProvider.getPublicKeyFor("Test", JWKS_URL, tokenWithoutKid))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("유효하지 않은 Test 토큰입니다");
    }

    private String token(String kid, KeyPair keyPair) {
        return Jwts.builder()
                .header().keyId(kid).and()
                .subject("user")
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private String jwks(String kid, KeyPair keyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return """
                {"keys":[{"kty":"RSA","kid":"%s","n":"%s","e":"%s"}]}
                """.formatted(
                kid,
                encoder.encodeToString(publicKey.getModulus().toByteArray()),
                encoder.encodeToString(publicKey.getPublicExponent().toByteArray()));
    }
}
