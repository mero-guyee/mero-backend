package io.mero.app.domain.expense.controller;

import io.mero.app.domain.expense.dto.ExpenseCategoryCreateRequest;
import io.mero.app.domain.expense.dto.ExpenseCategoryResponse;
import io.mero.app.domain.expense.service.ExpenseCategoryService;
import io.mero.app.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "ExpenseCategory", description = "지출 카테고리 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/expense-categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {

    private final ExpenseCategoryService expenseCategoryService;

    @Operation(summary = "카테고리 목록 조회", description = "지출 카테고리 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ExpenseCategoryResponse>> getCategories() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<ExpenseCategoryResponse> categories = expenseCategoryService.getCategories(userId);
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "카테고리 생성", description = "지출 카테고리를 생성합니다.")
    @PostMapping()
    public ResponseEntity<Void> createExpenseCategory(ExpenseCategoryCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        expenseCategoryService.createExpenseCategory(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "카테고리 삭제", description = "지출 카테고리를 삭제합니다. 해당 카테고리의 지출은 기본 카테고리로 변경됩니다.")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        Long userId = SecurityUtil.getCurrentUserId();
        expenseCategoryService.deleteCategory(userId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
