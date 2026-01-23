package io.mero.app.domain.budget.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.budget.dto.BudgetCreateRequest;
import io.mero.app.domain.budget.dto.BudgetResponse;
import io.mero.app.domain.budget.dto.BudgetUpdateRequest;
import io.mero.app.domain.budget.service.BudgetService;
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
        controllers = BudgetController.class,
        excludeFilters = {@ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BudgetService budgetService;

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
    @DisplayName("예산 생성 API 성공")
    void createBudget_Success() throws Exception {
        // given
        BudgetCreateRequest request = new BudgetCreateRequest(
                "test-client-id-1",
                new BigDecimal("4000"),
                Currency.USD
        );

        BudgetResponse response = new BudgetResponse(
                1L, "test-client-id-1", 1L,
                new BigDecimal("4000"), Currency.USD,
                LocalDateTime.now(), LocalDateTime.now()
        );

        given(budgetService.createBudget(anyLong(), eq(1L), any(BudgetCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/trips/1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.amount").value(4000));
    }

    @Test
    @DisplayName("예산 목록 조회 API 성공")
    void getBudgets_Success() throws Exception {
        // given
        List<BudgetResponse> responses = List.of(
                new BudgetResponse(1L, "client-id-1", 1L, new BigDecimal("4000"), Currency.USD,
                        LocalDateTime.now(), LocalDateTime.now()),
                new BudgetResponse(2L, "client-id-2", 1L, new BigDecimal("1000000"), Currency.KRW,
                        LocalDateTime.now(), LocalDateTime.now())
        );

        given(budgetService.getBudgetsByTrip(anyLong(), eq(1L)))
                .willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/trips/1/budgets"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[1].currency").value("KRW"));
    }

    @Test
    @DisplayName("예산 수정 API 성공")
    void updateBudget_Success() throws Exception {
        // given
        BudgetUpdateRequest request = new BudgetUpdateRequest(
                new BigDecimal("5000"),
                Currency.USD
        );

        BudgetResponse response = new BudgetResponse(
                1L, "client-id-1", 1L,
                new BigDecimal("5000"), Currency.USD,
                LocalDateTime.now(), LocalDateTime.now()
        );

        given(budgetService.updateBudget(anyLong(), eq(1L), eq(1L), any(BudgetUpdateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(put("/api/trips/1/budgets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(5000));
    }

    @Test
    @DisplayName("예산 삭제 API 성공")
    void deleteBudget_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/trips/1/budgets/1"))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}
