package io.mero.app.domain.expense.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mero.app.domain.expense.dto.ExpenseCategoryCreateRequest;
import io.mero.app.domain.expense.dto.ExpenseCategoryResponse;
import io.mero.app.domain.expense.service.ExpenseCategoryService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ExpenseCategoryController.class,
        excludeFilters = {@ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ExpenseCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExpenseCategoryService expenseCategoryService;

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
    @DisplayName("카테고리 목록 조회 성공")
    void 카테고리_목록_조회_성공() throws Exception {
        // given
        List<ExpenseCategoryResponse> responses = List.of(
                ExpenseCategoryResponse.builder().id(1L).name("식비").icon("🍜").color("#FF5733").isDefault(true).build(),
                ExpenseCategoryResponse.builder().id(2L).name("교통").icon("🚌").color("#3498DB").isDefault(true).build(),
                ExpenseCategoryResponse.builder().id(3L).name("쇼핑").icon("🛍️").color("#9B59B6").isDefault(false).build()
        );

        given(expenseCategoryService.getCategories(anyLong())).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/expense-categories"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("식비"))
                .andExpect(jsonPath("$[0]['default']").value(true))
                .andExpect(jsonPath("$[2]['default']").value(false));
    }

    @Test
    @DisplayName("카테고리 생성 성공")
    void 카테고리_생성_성공() throws Exception {
        // given
        ExpenseCategoryCreateRequest request = new ExpenseCategoryCreateRequest(
                "category-client-id-1", 1L, "여가", "🎮", "#2ECC71"
        );

        // when & then
        mockMvc.perform(post("/api/expense-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(expenseCategoryService).createExpenseCategory(anyLong(), org.mockito.ArgumentMatchers.any(ExpenseCategoryCreateRequest.class));
    }

    @Test
    @DisplayName("카테고리 삭제 성공")
    void 카테고리_삭제_성공() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/expense-categories/1"))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(expenseCategoryService).deleteCategory(1L, 1L);
    }
}
