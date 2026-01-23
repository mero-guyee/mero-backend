package io.mero.app.domain.expense.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.expense.dto.CurrencyUsageDto;
import io.mero.app.domain.expense.dto.ExpenseCreateRequest;
import io.mero.app.domain.expense.dto.ExpenseListResponse;
import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.domain.expense.dto.ExpenseUpdateRequest;
import io.mero.app.domain.expense.service.ExpenseService;
import io.mero.app.global.enums.Currency;
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
        controllers = ExpenseController.class,
        excludeFilters = {@ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExpenseService expenseService;

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
    @DisplayName("지출 생성 API 성공 - Footprint 없이")
    void createExpense_Success_WithoutFootprint() throws Exception {
        // given
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                "test-client-id-1",
                1L,
                null,  // footprintId
                new BigDecimal("100"),
                Currency.USD,
                1L,  // categoryId
                "스타벅스",
                LocalDate.of(2024, 12, 8),
                "도쿄"
        );

        ExpenseResponse response = new ExpenseResponse(
                1L, "test-client-id-1", 1L, null,
                new BigDecimal("100"), Currency.USD,
                1L, "식비", "🍔", "#FF5733",
                "스타벅스",
                LocalDate.of(2024, 12, 8), "도쿄",
                LocalDateTime.now()
        );

        given(expenseService.createExpense(anyLong(), any(ExpenseCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/trips/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.footprintId").isEmpty())
                .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    @DisplayName("지출 생성 API 성공 - Footprint 연결")
    void createExpense_Success_WithFootprint() throws Exception {
        // given
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                "test-client-id-2",
                1L,
                1L,  // footprintId
                new BigDecimal("100"),
                Currency.USD,
                1L,  // categoryId
                "일기에 기록한 스타벅스",
                LocalDate.of(2024, 12, 8),
                "도쿄"
        );

        ExpenseResponse response = new ExpenseResponse(
                1L, "test-client-id-2", 1L, 1L,  // footprintId 포함
                new BigDecimal("100"), Currency.USD,
                1L, "식비", "🍔", "#FF5733",
                "일기에 기록한 스타벅스",
                LocalDate.of(2024, 12, 8), "도쿄",
                LocalDateTime.now()
        );

        given(expenseService.createExpense(anyLong(), any(ExpenseCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/trips/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.footprintId").value(1L));
    }

    @Test
    @DisplayName("지출 수정 API 성공 - Footprint 연결")
    void updateExpense_Success_LinkFootprint() throws Exception {
        // given
        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                1L,  // footprintId
                new BigDecimal("150"),
                Currency.USD,
                1L,  // categoryId
                "수정된 설명",
                LocalDate.now(),
                "도쿄"
        );

        ExpenseResponse response = new ExpenseResponse(
                1L, "client-id-1", 1L, 1L,
                new BigDecimal("150"), Currency.USD,
                1L, "식비", "🍔", "#FF5733",
                "수정된 설명", LocalDate.now(), "도쿄",
                LocalDateTime.now()
        );

        given(expenseService.updateExpense(anyLong(), eq(1L), any(ExpenseUpdateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(put("/api/trips/1/expenses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.footprintId").value(1L))
                .andExpect(jsonPath("$.amount").value(150));
    }

    @Test
    @DisplayName("지출 수정 API 성공 - Footprint 연결 해제")
    void updateExpense_Success_UnlinkFootprint() throws Exception {
        // given
        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                null,  // footprintId null (연결 해제)
                new BigDecimal("150"),
                Currency.USD,
                1L,  // categoryId
                "수정된 설명",
                LocalDate.now(),
                "도쿄"
        );

        ExpenseResponse response = new ExpenseResponse(
                1L, "client-id-1", 1L, null,  // footprintId null
                new BigDecimal("150"), Currency.USD,
                1L, "식비", "🍔", "#FF5733",
                "수정된 설명", LocalDate.now(), "도쿄",
                LocalDateTime.now()
        );

        given(expenseService.updateExpense(anyLong(), eq(1L), any(ExpenseUpdateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(put("/api/trips/1/expenses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.footprintId").isEmpty())
                .andExpect(jsonPath("$.amount").value(150));
    }

    @Test
    @DisplayName("지출 삭제 API 성공")
    void deleteExpense_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/trips/1/expenses/1"))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("지출 목록 조회 API 성공")
    void getExpenses_Success() throws Exception {
        // given
        List<ExpenseResponse> expenses = List.of(
                new ExpenseResponse(
                        1L, "client-id-1", 1L, null,
                        new BigDecimal("50"), Currency.USD,
                        1L, "식비", "🍔", "#FF5733",
                        "스타벅스",
                        LocalDate.of(2024, 12, 8), "도쿄",
                        LocalDateTime.now()
                ),
                new ExpenseResponse(
                        2L, "client-id-2", 1L, null,
                        new BigDecimal("100000"), Currency.KRW,
                        2L, "쇼핑", "🛍️", "#33FF57",
                        "면세점",
                        LocalDate.of(2024, 12, 7), "인천",
                        LocalDateTime.now()
                )
        );

        List<CurrencyUsageDto> currencyUsages = List.of(
                CurrencyUsageDto.of(Currency.USD, new BigDecimal("80"), new BigDecimal("200")),
                CurrencyUsageDto.of(Currency.KRW, new BigDecimal("100000"), new BigDecimal("500000"))
        );

        ExpenseListResponse response = new ExpenseListResponse(expenses, currencyUsages);

        given(expenseService.getExpensesByTrip(anyLong(), eq(1L)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/trips/1/expenses"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenses").isArray())
                .andExpect(jsonPath("$.expenses.length()").value(2))
                .andExpect(jsonPath("$.expenses[0].id").value(1L))
                .andExpect(jsonPath("$.expenses[0].amount").value(50))
                .andExpect(jsonPath("$.expenses[0].currency").value("USD"))
                .andExpect(jsonPath("$.currencyUsages").isArray())
                .andExpect(jsonPath("$.currencyUsages.length()").value(2))
                .andExpect(jsonPath("$.currencyUsages[0].currency").value("USD"))
                .andExpect(jsonPath("$.currencyUsages[0].totalSpent").value(80))
                .andExpect(jsonPath("$.currencyUsages[0].budget").value(200))
                .andExpect(jsonPath("$.currencyUsages[0].usagePercent").value(40.00))
                .andExpect(jsonPath("$.currencyUsages[1].currency").value("KRW"))
                .andExpect(jsonPath("$.currencyUsages[1].totalSpent").value(100000))
                .andExpect(jsonPath("$.currencyUsages[1].budget").value(500000))
                .andExpect(jsonPath("$.currencyUsages[1].usagePercent").value(20.00));
    }

}