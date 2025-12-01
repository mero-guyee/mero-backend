package io.mero.app.domain.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.user.dto.*;
import io.mero.app.domain.user.service.UserService;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.enums.Timezone;
import io.mero.app.global.jwt.JwtAuthenticationFilter;
import io.mero.app.global.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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

        UserResponse response = new UserResponse(
                1L,
                "test@example.com",
                "테스트유저",
                null,
                Currency.KRW,
                Timezone.ASIA_SEOUL,
                LocalDateTime.now()
        );

        given(userService.signUp(any(SignUpRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.nickname").value("테스트유저"))
                .andExpect(jsonPath("$.defaultCurrency").value("KRW"))
                .andExpect(jsonPath("$.timezone").value("ASIA_SEOUL"));
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
        // given
        LogoutRequest request = new LogoutRequest("valid-refresh-token");

        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(userService).logout(any(LogoutRequest.class));
    }
    
    @Test
    @DisplayName("로그아웃 API 실패 - 토큰 없음")
    void 로그아웃_API_실패_토큰_없음() throws Exception {
        // given
        LogoutRequest request = new LogoutRequest("");

        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}