package io.mero.app.global.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUP() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey",
                "your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm-please-change-this-in-production");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidity", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenValidity", 604800000L);
        jwtTokenProvider.init();
    }
    
    @Test
    @DisplayName("Access Token 생성")
    void Access_Token_생성() {
        // given
        long userId = 1L;
    
        // when
        String accessToken = jwtTokenProvider.createAccessToken(userId);

        // then
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
    }
    
    @Test
    @DisplayName("토큰에서 userId 추출")
    void 토큰에서_userId_추출() {
        // given
        long userId = 1L;
        String accessToken = jwtTokenProvider.createAccessToken(userId);

        // when
        long extractedUserId = jwtTokenProvider.getUserIdFrom(accessToken);

        // then
        assertThat(extractedUserId).isEqualTo(userId);
    }
    
    @Test
    @DisplayName("토큰 유효성 검증 - 성공")
    void 토큰_유효성_검증_성공() {
        // given
        long userId = 1L;
        String accessToken = jwtTokenProvider.createAccessToken(userId);
    
        // when
        boolean isValid = jwtTokenProvider.validateToken(accessToken);

        // then
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("토큰 유효성 검증 - 잘못된 토큰")
    void 토큰_유효성_검증_잘못된_토큰() {
        // given
        String invalidToken = "invalid.token";

        // when
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Refresh Token 생성")
    void Refresh_Token_생성() {
        // given
        long userId = 1L;

        // when
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
    }

}