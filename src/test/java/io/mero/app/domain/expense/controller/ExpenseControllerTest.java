package io.mero.app.domain.expense.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.expense.dto.ExpenseCreateRequest;
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
    @DisplayName("지출 생성 API 성공 - Diary 없이")
    void createExpense_Success_WithoutDiary() throws Exception {
        // given
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                1L,
                null,  // diaryId
                new BigDecimal("100"),
                Currency.USD,
                "식비",
                "스타벅스",
                LocalDate.of(2024, 12, 8),
                "도쿄",
                null,
                null
        );

        ExpenseResponse response = new ExpenseResponse(
                1L, 1L, null,
                new BigDecimal("100"), Currency.USD,
                "식비", "스타벅스",
                LocalDate.of(2024, 12, 8), "도쿄",
                new BigDecimal("1472"), new BigDecimal("147200.00"),
                Currency.KRW, false, null, LocalDateTime.now()
        );

        given(expenseService.createExpense(anyLong(), any(ExpenseCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.diaryId").isEmpty())
                .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    @DisplayName("지출 생성 API 성공 - Diary 연결")
    void createExpense_Success_WithDiary() throws Exception {
        // given
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                1L,
                1L,  // diaryId
                new BigDecimal("100"),
                Currency.USD,
                "식비",
                "일기에 기록한 스타벅스",
                LocalDate.of(2024, 12, 8),
                "도쿄",
                null,
                null
        );

        ExpenseResponse response = new ExpenseResponse(
                1L, 1L, 1L,  // diaryId 포함
                new BigDecimal("100"), Currency.USD,
                "식비", "일기에 기록한 스타벅스",
                LocalDate.of(2024, 12, 8), "도쿄",
                new BigDecimal("1472"), new BigDecimal("147200.00"),
                Currency.KRW, false, null, LocalDateTime.now()
        );

        given(expenseService.createExpense(anyLong(), any(ExpenseCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.diaryId").value(1L));
    }

    @Test
    @DisplayName("지출 수정 API 성공 - Diary 연결")
    void updateExpense_Success_LinkDiary() throws Exception {
        // given
        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                1L,  // diaryId
                new BigDecimal("150"),
                Currency.USD,
                "식비",
                "수정된 설명",
                LocalDate.now(),
                "도쿄",
                null,
                null
        );

        ExpenseResponse response = new ExpenseResponse(
                1L, 1L, 1L,
                new BigDecimal("150"), Currency.USD,
                "식비", "수정된 설명", LocalDate.now(), "도쿄",
                new BigDecimal("1472"), new BigDecimal("220800.00"),
                Currency.KRW, false, null, LocalDateTime.now()
        );

        given(expenseService.updateExpense(anyLong(), eq(1L), any(ExpenseUpdateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(put("/api/expenses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diaryId").value(1L))
                .andExpect(jsonPath("$.amount").value(150));
    }

    @Test
    @DisplayName("지출 수정 API 성공 - Diary 연결 해제")
    void updateExpense_Success_UnlinkDiary() throws Exception {
        // given
        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                null,  // diaryId null (연결 해제)
                new BigDecimal("150"),
                Currency.USD,
                "식비",
                "수정된 설명",
                LocalDate.now(),
                "도쿄",
                null,
                null
        );

        ExpenseResponse response = new ExpenseResponse(
                1L, 1L, null,  // diaryId null
                new BigDecimal("150"), Currency.USD,
                "식비", "수정된 설명", LocalDate.now(), "도쿄",
                new BigDecimal("1472"), new BigDecimal("220800.00"),
                Currency.KRW, false, null, LocalDateTime.now()
        );

        given(expenseService.updateExpense(anyLong(), eq(1L), any(ExpenseUpdateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(put("/api/expenses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diaryId").isEmpty())
                .andExpect(jsonPath("$.amount").value(150));
    }

    @Test
    @DisplayName("지출 삭제 API 성공")
    void deleteExpense_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/expenses/1"))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

}