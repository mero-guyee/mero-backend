package io.mero.app.domain.trip.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.trip.dto.TripCreateRequest;
import io.mero.app.domain.trip.dto.TripResponse;
import io.mero.app.domain.trip.dto.TripUpdateRequest;
import io.mero.app.domain.trip.service.TripService;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.jwt.JwtAuthenticationFilter;
import io.mero.app.global.util.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
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
        controllers = TripController.class,
        excludeFilters = {@ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = JwtAuthenticationFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockitoBean
    private TripService tripService;

    @Test
    @DisplayName("여행 생성 성공")
    void 여행_생성_성공() throws Exception {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            // given
            securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(1L);

            TripCreateRequest request = new TripCreateRequest(
                    "남미 여행",
                    "남미 가자!",
                    LocalDate.of(2026,03,11),
                    LocalDate.of(2026,05,15),
                    List.of("브라질", "아르헨티나", "페루"),
                    new BigDecimal(10000000),
                    Currency.ARS,
                    Currency.KRW
            );

            TripResponse response = new TripResponse(
                    1L,
                    "남미 여행",
                    "남미 가자!",
                    LocalDate.of(2026,03,11),
                    LocalDate.of(2026,05,15),
                    List.of("브라질", "아르헨티나", "페루"),
                    new BigDecimal(10000000),
                    Currency.ARS,
                    Currency.KRW,
                    LocalDateTime.now()
            );

            given(tripService.createTrip(anyLong(), any(TripCreateRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/trips")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(response.getId()))
                    .andExpect(jsonPath("$.title").value(response.getTitle()))
                    .andExpect(jsonPath("$.description").value(response.getDescription()))
                    .andExpect(jsonPath("$.startDate").value(response.getStartDate().toString()))
                    .andExpect(jsonPath("$.endDate").value(response.getEndDate().toString()))
                    .andExpect(jsonPath("$.countries").isArray())
                    .andExpect(jsonPath("$.countries[0]").value("브라질"))
                    .andExpect(jsonPath("$.countries[1]").value("아르헨티나"))
                    .andExpect(jsonPath("$.countries[2]").value("페루"))
                    .andExpect(jsonPath("$.totalBudget").value(response.getTotalBudget()))
                    .andExpect(jsonPath("$.budgetCurrency").value(response.getBudgetCurrency().toString()))
                    .andExpect(jsonPath("$.defaultCurrency").value(response.getDefaultCurrency().toString()));
        }
    }
    
    @Test
    @DisplayName("여행 목록 조회 성공")
    void 여행_목록_조회_성공() throws Exception {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            // given
            securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(1L);

            List<TripResponse> tripResponses = List.of(
                    new TripResponse(
                            1L, "남미 여행", "남미 가자!",
                            LocalDate.of(2026, 3, 11),
                            LocalDate.of(2026, 5, 15),
                            List.of("브라질", "아르헨티나", "페루"),
                            new BigDecimal(10000000),
                            Currency.ARS,
                            Currency.KRW,
                            LocalDateTime.now()
                    ),
                    new TripResponse(
                            2L, "일본 여행", "일본 가자!",
                            LocalDate.of(2026, 8, 10),
                            LocalDate.of(2026, 10, 16),
                            List.of("도쿄", "오사카", "교토"),
                            new BigDecimal(5000000),
                            Currency.JPY,
                            Currency.KRW,
                            LocalDateTime.now()
                    )
            );

            given(tripService.getTrips(anyLong())).willReturn(tripResponses);

            // when & then
            mockMvc.perform(get("/api/trips"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(tripResponses.size()))
                    .andExpect(jsonPath("$[0].title").value(tripResponses.get(0).getTitle()))
                    .andExpect(jsonPath("$[1].title").value(tripResponses.get(1).getTitle()));
        }
    }
    
    @Test
    @DisplayName("여행 상세 조회 성공")
    void 여행_상세_조회_성공() throws Exception {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            // given
            securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
            TripResponse response = new TripResponse(
                    1L,
                    "남미 여행",
                    "남미 가자!",
                    LocalDate.of(2026, 03, 11),
                    LocalDate.of(2026, 05, 15),
                    List.of("브라질", "아르헨티나", "페루"),
                    new BigDecimal(10000000),
                    Currency.ARS,
                    Currency.KRW,
                    LocalDateTime.now()
            );

            given(tripService.getTrip(anyLong(), eq(1L))).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/trips/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.title").value("남미 여행"));
        }
    }
    
    @Test
    @DisplayName("여행 수정 성공")
    void 여행_수정_성공() throws Exception {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            // given
            securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
            TripUpdateRequest request = new TripUpdateRequest(
                    "남미 여행 수정",
                    "남미 진짜 가자!",
                    LocalDate.of(2026, 3, 10),
                    LocalDate.of(2026, 5, 16),
                    List.of("아르헨티나", "페루", "볼리비아"),
                    null,
                    new BigDecimal(20000000),
                    Currency.ARS,
                    Currency.USD
            );

            TripResponse response = new TripResponse(
                    1L,
                    "남미 여행 수정",
                    "남미 진짜 가자!",
                    LocalDate.of(2026, 3, 10),
                    LocalDate.of(2026, 5, 16),
                    List.of("아르헨티나", "페루", "볼리비아"),
                    new BigDecimal(20000000),
                    Currency.ARS,
                    Currency.KRW,
                    LocalDateTime.now()
            );

            given(tripService.updateTrip(anyLong(), eq(1L), any(TripUpdateRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(put("/api/trips/1")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("남미 여행 수정"))
                    .andExpect(jsonPath("$.description").value("남미 진짜 가자!"));
        }
    }
    
    @Test
    @DisplayName("여행 삭제 성공")
    void 여행_삭제_성공() throws Exception {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            // given
            securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(1L);

            // when & then
            mockMvc.perform(delete("/api/trips/1"))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }
    }
}