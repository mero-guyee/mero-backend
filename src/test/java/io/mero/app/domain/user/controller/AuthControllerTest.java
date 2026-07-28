package io.mero.app.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.user.dto.AppleLoginRequest;
import io.mero.app.domain.user.dto.GoogleLoginRequest;
import io.mero.app.domain.user.dto.LoginResponse;
import io.mero.app.domain.user.dto.TokenRefreshRequest;
import io.mero.app.domain.user.dto.TokenRefreshResponse;
import io.mero.app.domain.user.service.UserService;
import io.mero.app.global.jwt.JwtAuthenticationFilter;
import io.mero.app.global.jwt.JwtTokenProvider;
import io.mero.app.global.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class}  // ← Security Filter 제외
        ))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private MockedStatic<SecurityUtil> securityUtil;

    @BeforeEach
    void setUp() {
        securityUtil = mockStatic(SecurityUtil.class);
        securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtil.close();
    }

    @Test
    @DisplayName("토큰 재발급 API 성공")
    void 토큰_재발급_API_성공() throws Exception {
        // given
        TokenRefreshRequest request = new TokenRefreshRequest("old-refresh-token");
        TokenRefreshResponse response = new TokenRefreshResponse("new-access-token", "new-refresh-token");

        given(userService.refreshToken(any(TokenRefreshRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("토큰 재발급 API 실패 - refresh token 없음")
    void 토큰_재발급_API_실패_refresh_token_없음() throws Exception {
        // given
        TokenRefreshRequest request = new TokenRefreshRequest("");

        // when & then
        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그아웃 API 성공")
    void 로그아웃_API_성공() throws Exception {
        // when & then
        mockMvc.perform(post("/api/auth/logout"))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(userService).logout(1L);
    }

    @Test
    @DisplayName("Apple 로그인 API 성공")
    void Apple_로그인_API_성공() throws Exception {
        // given
        String body = "{\"identityToken\":\"valid.apple.token\"}";

        LoginResponse response = new LoginResponse(
                2L, "apple@example.com", "user1a2b3c", null, "access-token", "refresh-token", false
        );

        given(userService.appleLogin(any(AppleLoginRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2L))
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("Apple 로그인 API 실패 - identityToken 없음")
    void Apple_로그인_API_실패_identityToken_없음() throws Exception {
        // given
        String body = "{\"identityToken\":\"\"}";

        // when & then
        mockMvc.perform(post("/api/auth/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Google 로그인 API 성공")
    void Google_로그인_API_성공() throws Exception {
        // given
        String body = "{\"idToken\":\"valid.google.token\"}";

        LoginResponse response = new LoginResponse(
                3L, "google@example.com", "user-google", "https://lh3.googleusercontent.com/a/pic",
                "access-token", "refresh-token", true
        );

        given(userService.googleLogin(any(GoogleLoginRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(3L))
                .andExpect(jsonPath("$.email").value("google@example.com"))
                .andExpect(jsonPath("$.profileImage").value("https://lh3.googleusercontent.com/a/pic"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.isNewUser").value(true));

        verify(userService).googleLogin(any(GoogleLoginRequest.class));
    }

    @Test
    @DisplayName("Google 로그인 API 실패 - idToken 없음")
    void Google_로그인_API_실패_idToken_없음() throws Exception {
        // given
        String body = "{\"idToken\":\"\"}";

        // when & then
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
