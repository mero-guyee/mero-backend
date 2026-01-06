package io.mero.app.domain.diary.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.diary.dto.DiaryCreateRequest;
import io.mero.app.domain.diary.dto.DiaryResponse;
import io.mero.app.domain.diary.dto.DiaryUpdateRequest;
import io.mero.app.domain.diary.service.DiaryService;
import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.global.embedded.Location;
import io.mero.app.global.enums.Currency;
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
import java.time.LocalDateTime;
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
                "title",
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
                List.of("url1", "url2"),
                null,
                null
        );

        given(diaryService.createDiary(anyLong(), anyLong(), any(DiaryCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/trips/1/diaries")
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
                null,
                null,
                null
        );

        given(diaryService.getDiary(anyLong(), anyLong(), eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/trips/1/diaries/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value("첫째날"));
    }

    @Test
    @DisplayName("일기 상세 조회 API 성공 - 경비 포함")
    void 일기_상세_조회_API_성공_경비_포함() throws Exception {
        // given
        List<ExpenseResponse> expenses = List.of(
                new ExpenseResponse(1L, 1L, 1L, new BigDecimal("100"), Currency.USD,
                        "식비", "스타벅스", LocalDate.now(), null,
                        LocalDateTime.now()),
                new ExpenseResponse(2L, 1L, 1L, new BigDecimal("5000"), Currency.JPY,
                        "교통", "택시", LocalDate.now(), "신주쿠",
                        LocalDateTime.now())
        );

        DiaryResponse response = new DiaryResponse(
                1L, 1L, "멋진 하루였다",
                LocalDate.of(2024, 12, 8), null, "맑음",
                List.of("photo1.jpg"), expenses, LocalDateTime.now()
        );

        given(diaryService.getDiary(anyLong(), anyLong(), eq(1L)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/trips/1/diaries/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.expenses").isArray())
                .andExpect(jsonPath("$.expenses.length()").value(2))
                .andExpect(jsonPath("$.expenses[0].diaryId").value(1L))
                .andExpect(jsonPath("$.expenses[1].diaryId").value(1L));
    }
    
    @Test
    @DisplayName("여행 일기 수정 API 성공")
    void 여행_일기_수정_API_성공() throws Exception {
        // given
        DiaryUpdateRequest request = new DiaryUpdateRequest(
                "첫째날 수정",
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
                null,
                null,
                null
        );

        given(diaryService.updateDiary(anyLong(), anyLong(), eq(1L), any(DiaryUpdateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(put("/api/trips/1/diaries/1")
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
        mockMvc.perform(delete("/api/trips/1/diaries/1"))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}