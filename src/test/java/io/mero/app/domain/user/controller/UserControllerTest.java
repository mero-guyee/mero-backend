package io.mero.app.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.user.dto.NicknameChangeRequest;
import io.mero.app.domain.user.dto.UserResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = {@ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

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
    @DisplayName("닉네임 변경 성공")
    void 닉네임_변경_성공() throws Exception {
        // given
        String body = "{\"nickname\":\"새닉네임\"}";

        // when & then
        mockMvc.perform(patch("/api/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(userService).changeNickname(eq(1L), any(NicknameChangeRequest.class));
    }

    @Test
    @DisplayName("닉네임 변경 실패 - 닉네임 길이 부족")
    void 닉네임_변경_실패_닉네임_길이_부족() throws Exception {
        // given
        String body = "{\"nickname\":\"a\"}";

        // when & then
        mockMvc.perform(patch("/api/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("닉네임 변경 실패 - 닉네임 없음")
    void 닉네임_변경_실패_닉네임_없음() throws Exception {
        // given
        String body = "{\"nickname\":\"\"}";

        // when & then
        mockMvc.perform(patch("/api/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("프로필 이미지 업로드 성공")
    void 프로필_이미지_업로드_성공() throws Exception {
        // given
        MockMultipartFile image = new MockMultipartFile(
                "image", "profile.jpg", MediaType.IMAGE_JPEG_VALUE, "image-content".getBytes());
        UserResponse response = new UserResponse(
                1L, "test@mero.io", "테스터", "https://storage.mero.io/signed-url", LocalDateTime.now());
        when(userService.updateProfileImage(eq(1L), any())).thenReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/users/me/profile-image").file(image))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImage").value("https://storage.mero.io/signed-url"));

        verify(userService).updateProfileImage(eq(1L), any());
    }

    @Test
    @DisplayName("프로필 이미지 업로드 실패 - 파일 없음")
    void 프로필_이미지_업로드_실패_파일_없음() throws Exception {
        // when & then
        mockMvc.perform(multipart("/api/users/me/profile-image"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("프로필 이미지 삭제 성공")
    void 프로필_이미지_삭제_성공() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/users/me/profile-image"))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(userService).deleteProfileImage(1L);
    }
}
