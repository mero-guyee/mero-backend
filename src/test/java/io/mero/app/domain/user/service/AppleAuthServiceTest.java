package io.mero.app.domain.user.service;

import io.jsonwebtoken.Jwts;
import io.mero.app.domain.user.service.AppleAuthService.AppleClaims;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AppleAuthServiceTest {

    private static final String BUNDLE_ID = "io.mero.app";
    private static final String ISSUER = "https://appleid.apple.com";

    @Mock
    private JwkProvider jwkProvider;

    @InjectMocks
    private AppleAuthService appleAuthService;

    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        ReflectionTestUtils.setField(appleAuthService, "bundleId", BUNDLE_ID);
        given(jwkProvider.getPublicKeyFor(eq("Apple"), any(), any())).willReturn(keyPair.getPublic());
    }

    @Test
    @DisplayName("email_verified가 문자열 \"true\"여도 인증된 것으로 본다")
    void 문자열_true도_인증으로_처리() {
        // given - Apple은 email_verified를 문자열로 내려주는 경우가 있다
        String token = token(builder -> builder
                .claim("email", "apple@example.com")
                .claim("email_verified", "true"));

        // when
        AppleClaims claims = appleAuthService.validate(token);

        // then
        assertThat(claims.email()).isEqualTo("apple@example.com");
        assertThat(claims.appleUserId()).isEqualTo("apple-user-id");
    }

    @Test
    @DisplayName("email_verified가 boolean true여도 인증된 것으로 본다")
    void boolean_true도_인증으로_처리() {
        // given
        String token = token(builder -> builder
                .claim("email", "apple@example.com")
                .claim("email_verified", true));

        // when
        AppleClaims claims = appleAuthService.validate(token);

        // then
        assertThat(claims.email()).isEqualTo("apple@example.com");
    }

    @Test
    @DisplayName("email이 있는데 email_verified가 false면 거부한다")
    void 미인증_이메일은_거부() {
        // given
        String token = token(builder -> builder
                .claim("email", "attacker@example.com")
                .claim("email_verified", "false"));

        // when & then
        assertThatThrownBy(() -> appleAuthService.validate(token))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("이메일이 인증되지 않은");
    }

    @Test
    @DisplayName("email이 있는데 email_verified 클레임이 아예 없으면 거부한다")
    void 클레임_누락은_거부() {
        // given
        String token = token(builder -> builder.claim("email", "apple@example.com"));

        // when & then
        assertThatThrownBy(() -> appleAuthService.validate(token))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("이메일이 인증되지 않은");
    }

    @Test
    @DisplayName("email이 없는 재로그인 토큰은 정상 처리한다")
    void 이메일_없는_토큰은_통과() {
        // given - Apple은 최초 로그인에만 email을 내려준다
        String token = token(builder -> builder);

        // when
        AppleClaims claims = appleAuthService.validate(token);

        // then
        assertThat(claims.appleUserId()).isEqualTo("apple-user-id");
        assertThat(claims.email()).isNull();
    }

    @Test
    @DisplayName("issuer가 다르면 거부한다")
    void 잘못된_issuer는_거부() {
        // given
        String token = Jwts.builder()
                .header().keyId("kid-1").and()
                .subject("apple-user-id")
                .issuer("https://evil.example.com")
                .audience().add(BUNDLE_ID).and()
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        // when & then
        assertThatThrownBy(() -> appleAuthService.validate(token))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("유효하지 않은 Apple 토큰");
    }

    @Test
    @DisplayName("audience가 우리 번들 ID가 아니면 거부한다")
    void 잘못된_audience는_거부() {
        // given
        String token = Jwts.builder()
                .header().keyId("kid-1").and()
                .subject("apple-user-id")
                .issuer(ISSUER)
                .audience().add("com.other.app").and()
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        // when & then
        assertThatThrownBy(() -> appleAuthService.validate(token))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("유효하지 않은 Apple 토큰");
    }

    @Test
    @DisplayName("만료된 토큰은 거부한다")
    void 만료된_토큰은_거부() {
        // given
        String token = Jwts.builder()
                .header().keyId("kid-1").and()
                .subject("apple-user-id")
                .issuer(ISSUER)
                .audience().add(BUNDLE_ID).and()
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        // when & then
        assertThatThrownBy(() -> appleAuthService.validate(token))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Apple 토큰 검증에 실패했습니다");
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 거부한다")
    void 위조_서명은_거부() throws Exception {
        // given
        KeyPair attackerKey = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String token = Jwts.builder()
                .header().keyId("kid-1").and()
                .subject("apple-user-id")
                .issuer(ISSUER)
                .audience().add(BUNDLE_ID).and()
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(attackerKey.getPrivate(), Jwts.SIG.RS256)
                .compact();

        // when & then
        assertThatThrownBy(() -> appleAuthService.validate(token))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Apple 토큰 검증에 실패했습니다");
    }

    private String token(java.util.function.UnaryOperator<io.jsonwebtoken.JwtBuilder> customizer) {
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .header().keyId("kid-1").and()
                .subject("apple-user-id")
                .issuer(ISSUER)
                .audience().add(BUNDLE_ID).and()
                .expiration(new Date(System.currentTimeMillis() + 60_000));

        return customizer.apply(builder)
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }
}
