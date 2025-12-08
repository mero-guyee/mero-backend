package io.mero.app.domain.diary.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.diary.dto.DiaryCreateRequest;
import io.mero.app.domain.diary.dto.DiaryResponse;
import io.mero.app.domain.diary.dto.DiaryUpdateRequest;
import io.mero.app.domain.diary.service.DiaryService;
import io.mero.app.global.embedded.Location;
import io.mero.app.global.jwt.JwtAuthenticationFilter;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DiaryController.class,
        excludeFilters = {@ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class DiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DiaryService diaryService;

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
    @DisplayName("일기 생성 API 성공")
    void 일기_생성_API_성공() throws Exception {
        // given
        DiaryCreateRequest request = new DiaryCreateRequest(
                1L,
                "오늘은 크리스마스",
                LocalDate.of(2025, 12, 25),
                new Location(new BigDecimal("37.5665"), new BigDecimal("126.9780"), "서울특별시"),
                List.of("url1", "url2")
        );

        DiaryResponse response = new DiaryResponse(
                1L,
                1L,
                "오늘은 크리스마스",
                LocalDate.of(2025, 12, 25),
                new Location(new BigDecimal("37.5665"), new BigDecimal("126.9780"), "서울특별시"),
                null,
                List.of("url1", "url2")
        );

        given(diaryService.createDiary(anyLong(), any(DiaryCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/diaries")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value("오늘은 크리스마스"))
                .andExpect(jsonPath("$.location.locationName").value(response.getLocation().getLocationName()));

    }

    @Test
    @DisplayName("여행 일기 상세 조회 API 성공")
    void 여행_일기_상세_조회_API_성공() throws Exception {
        // given
        DiaryResponse response = new DiaryResponse(
                1L,
                1L,
                "첫째날",
                null,
                null,
                null,
                null
        );

        given(diaryService.getDiary(anyLong(), eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/diaries/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value("첫째날"));
    }
    
    @Test
    @DisplayName("여행 일기 수정 API 성공")
    void 여행_일기_수정_API_성공() throws Exception {
        // given
        DiaryUpdateRequest request = new DiaryUpdateRequest(
                1L,
                "첫째날 수정",
                LocalDate.of(2025, 12, 26),
                new Location(new BigDecimal("37.5665"), new BigDecimal("126.9780"), "서울특별시"),
                List.of("url3", "url4")
        );

        DiaryResponse response = new DiaryResponse(
                1L,
                1L,
                "첫째날 수정",
                null,
                null,
                null,
                null
        );

        given(diaryService.updateDiary(anyLong(), eq(1L), any(DiaryUpdateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(put("/api/diaries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("첫째날 수정"));
    }
    
    @Test
    @DisplayName("여행 일기 삭제 API 성공")
    void 여행_일기_삭제_API_성공() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/diaries/1"))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}