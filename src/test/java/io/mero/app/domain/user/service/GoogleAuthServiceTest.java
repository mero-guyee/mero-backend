package io.mero.app.domain.user.service;

import io.jsonwebtoken.Jwts;
import io.mero.app.domain.user.service.GoogleAuthService.GoogleClaims;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.jwt.JwkProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    private static final String CLIENT_ID = "123.apps.googleusercontent.com";
    private static final String ISSUER = "https://accounts.google.com";

    @Mock
    private JwkProvider jwkProvider;

    @InjectMocks
    private GoogleAuthService googleAuthService;

    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        ReflectionTestUtils.setField(googleAuthService, "clientIds", List.of(CLIENT_ID));
        given(jwkProvider.getPublicKeyFor(any(), any())).willReturn(keyPair.getPublic());
    }

    @Test
    @DisplayName("email_verified가 boolean true면 인증된 것으로 본다")
    void boolean_true는_인증으로_처리() {
        // given
        String token = token(builder -> builder
                .claim("email", "google@example.com")
                .claim("email_verified", true)
                .claim("picture", "https://lh3.googleusercontent.com/a/pic"));

        // when
        GoogleClaims claims = googleAuthService.validate(token);

        // then
        assertThat(claims.email()).isEqualTo("google@example.com");
        assertThat(claims.picture()).isEqualTo("https://lh3.googleusercontent.com/a/pic");
    }

    @Test
    @DisplayName("email_verified가 문자열 \"true\"여도 인증된 것으로 본다")
    void 문자열_true도_인증으로_처리() {
        // given
        String token = token(builder -> builder
                .claim("email", "google@example.com")
                .claim("email_verified", "true"));

        // when
        GoogleClaims claims = googleAuthService.validate(token);

        // then
        assertThat(claims.email()).isEqualTo("google@example.com");
    }

    @Test
    @DisplayName("email_verified가 false면 거부한다")
    void 미인증_이메일은_거부() {
        // given
        String token = token(builder -> builder
                .claim("email", "attacker@example.com")
                .claim("email_verified", false));

        // when & then
        assertThatThrownBy(() -> googleAuthService.validate(token))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("이메일이 인증되지 않은");
    }

    @Test
    @DisplayName("email이 없으면 거부한다")
    void 이메일_없으면_거부() {
        // given
        String token = token(builder -> builder.claim("email_verified", true));

        // when & then
        assertThatThrownBy(() -> googleAuthService.validate(token))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("이메일이 인증되지 않은");
    }

    @Test
    @DisplayName("issuer가 다르면 거부한다")
    void 잘못된_issuer는_거부() {
        // given
        String token = Jwts.builder()
                .header().keyId("kid-1").and()
                .subject("google-user-id")
                .issuer("https://evil.example.com")
                .audience().add(CLIENT_ID).and()
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        // when & then
        assertThatThrownBy(() -> googleAuthService.validate(token))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("유효하지 않은 Google 토큰");
    }

    @Test
    @DisplayName("audience가 우리 클라이언트 ID가 아니면 거부한다")
    void 잘못된_audience는_거부() {
        // given
        String token = Jwts.builder()
                .header().keyId("kid-1").and()
                .subject("google-user-id")
                .issuer(ISSUER)
                .audience().add("999.apps.googleusercontent.com").and()
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        // when & then
        assertThatThrownBy(() -> googleAuthService.validate(token))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("유효하지 않은 Google 토큰");
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 거부한다")
    void 위조_서명은_거부() throws Exception {
        // given
        KeyPair attackerKey = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String token = Jwts.builder()
                .header().keyId("kid-1").and()
                .subject("google-user-id")
                .issuer(ISSUER)
                .audience().add(CLIENT_ID).and()
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(attackerKey.getPrivate(), Jwts.SIG.RS256)
                .compact();

        // when & then
        assertThatThrownBy(() -> googleAuthService.validate(token))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Google 토큰 검증에 실패했습니다");
    }

    private String token(java.util.function.UnaryOperator<io.jsonwebtoken.JwtBuilder> customizer) {
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .header().keyId("kid-1").and()
                .subject("google-user-id")
                .issuer(ISSUER)
                .audience().add(CLIENT_ID).and()
                .expiration(new Date(System.currentTimeMillis() + 60_000));

        return customizer.apply(builder)
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }
}
