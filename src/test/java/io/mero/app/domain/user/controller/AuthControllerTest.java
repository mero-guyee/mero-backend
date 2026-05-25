package io.mero.app.domain.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.user.dto.AppleLoginRequest;
import io.mero.app.domain.user.dto.GoogleLoginRequest;
import io.mero.app.domain.user.dto.LoginRequest;
import io.mero.app.domain.user.dto.LoginResponse;
import io.mero.app.domain.user.dto.PasswordResetConfirmRequest;
import io.mero.app.domain.user.dto.SignUpRequest;
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
    @DisplayName("회원가입 API 성공")
    void 회원가입_API_성공() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest(
                "test@example.com",
                "password123",
                "테스트유저",
                null,
                null
        );

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isAccepted());

        verify(userService).signUp(any(SignUpRequest.class));
    }

    @Test
    @DisplayName("회원가입 API 실패 - 이메일 형식 오류")
    void 회원가입_API_실패_이메일_형식_오류() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest(
                "invalid-email",  // 잘못된 형식
                "password123",
                "테스트유저",
                null,
                null
        );

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 API 실패 - 비밀번호 길이 부족")
    void 회원가입_API_실패_비밀번호_길이_부족() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest(
                "test@example.com",
                "short",  // 8자 미만
                "테스트유저",
                null,
                null
        );

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 API 실패 - 필수값 누락")
    void 회원가입_API_실패_필수값_누락() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest(
                null,
                "password123",
                "테스트유저",
                null,
                null
        );

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 API 성공")
    void 로그인_API_성공() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@email.com", "password123");

        LoginResponse response = new LoginResponse(
                1L,
                "test@email.com",
                "테스트유저",
                "temporary-access-token",
                "temporary-refresh-token"
        );

        given(userService.login(any(LoginRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.nickname").value("테스트유저"))
                .andExpect(jsonPath("$.accessToken").value("temporary-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("temporary-refresh-token"));

    }

    @Test
    @DisplayName("로그인 API 실패 - 이메일 형식 오류")
    void 로그인_API_실패_이메일_형식_오류() throws Exception {
        // given
        LoginRequest request = new LoginRequest("invalid-email.com", "password123");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
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
                2L, "apple@example.com", "user1a2b3c", "access-token", "refresh-token"
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
                3L, "google@example.com", "user-google", "access-token", "refresh-token"
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
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

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

    @Test
    @DisplayName("이메일 인증 성공")
    void 이메일_인증_성공() throws Exception {
        // when & then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/auth/email/verify")
                        .param("token", "valid-token"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userService).verifyEmail("valid-token");
    }

    @Test
    @DisplayName("인증 메일 재발송 성공")
    void 인증_메일_재발송_성공() throws Exception {
        // given
        String body = "{\"email\":\"test@example.com\"}";

        // when & then
        mockMvc.perform(post("/api/auth/email/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(userService).resendVerificationEmail("test@example.com");
    }

    @Test
    @DisplayName("인증 메일 재발송 실패 - 이메일 형식 오류")
    void 인증_메일_재발송_실패_이메일_형식_오류() throws Exception {
        // given
        String body = "{\"email\":\"invalid-email\"}";

        // when & then
        mockMvc.perform(post("/api/auth/email/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 성공")
    void 비밀번호_재설정_요청_성공() throws Exception {
        // given
        String body = "{\"email\":\"test@example.com\"}";

        // when & then
        mockMvc.perform(post("/api/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("비밀번호 재설정 성공")
    void 비밀번호_재설정_성공() throws Exception {
        // given
        String body = "{\"token\":\"reset-token\",\"newPassword\":\"newPassword1\"}";

        // when & then
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(userService).resetPassword(any(PasswordResetConfirmRequest.class));
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 새 비밀번호 길이 부족")
    void 비밀번호_재설정_실패_새_비밀번호_길이_부족() throws Exception {
        // given
        String body = "{\"token\":\"reset-token\",\"newPassword\":\"short\"}";

        // when & then
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}