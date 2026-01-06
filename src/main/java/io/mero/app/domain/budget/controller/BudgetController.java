package io.mero.app.domain.budget.controller;

import io.mero.app.domain.budget.dto.BudgetCreateRequest;
import io.mero.app.domain.budget.dto.BudgetResponse;
import io.mero.app.domain.budget.dto.BudgetUpdateRequest;
import io.mero.app.domain.budget.service.BudgetService;
import io.mero.app.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Budget", description = "예산 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/trips/{tripId}/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @Operation(summary = "예산 생성",
            description = "여행에 새로운 통화 예산을 설정합니다 (같은 통화는 1개만 가능)")
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @PathVariable Long tripId,
            @Valid @RequestBody BudgetCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        BudgetResponse response = budgetService.createBudget(userId, tripId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "예산 목록 조회",
            description = "특정 여행의 모든 예산을 조회합니다")
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(@PathVariable Long tripId) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<BudgetResponse> response = budgetService.getBudgetsByTrip(userId, tripId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "예산 수정",
            description = "예산 정보를 수정합니다 (통화 변경 시 중복 확인)")
    @PutMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable Long tripId,
            @PathVariable Long budgetId,
            @Valid @RequestBody BudgetUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        BudgetResponse response = budgetService.updateBudget(userId, tripId, budgetId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "예산 삭제",
            description = "예산을 삭제합니다")
    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable Long tripId,
            @PathVariable Long budgetId) {
        Long userId = SecurityUtil.getCurrentUserId();
        budgetService.deleteBudget(userId, tripId, budgetId);
        return ResponseEntity.noContent().build();
    }
}
