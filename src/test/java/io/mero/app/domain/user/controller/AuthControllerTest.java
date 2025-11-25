package io.mero.app.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.user.dto.LoginRequest;
import io.mero.app.domain.user.dto.LoginResponse;
import io.mero.app.domain.user.dto.SignUpRequest;
import io.mero.app.domain.user.dto.UserResponse;
import io.mero.app.domain.user.service.UserService;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.enums.Timezone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

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
                "temporary-access-token"
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
                .andExpect(jsonPath("$.accessToken").value("temporary-access-token"));

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
}