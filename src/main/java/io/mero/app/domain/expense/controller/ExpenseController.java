package io.mero.app.domain.expense.controller;

import io.mero.app.domain.expense.dto.ExpenseCreateRequest;
import io.mero.app.domain.expense.dto.ExpenseListResponse;
import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.domain.expense.dto.ExpenseUpdateRequest;
import io.mero.app.domain.expense.service.ExpenseService;
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


@Tag(name = "Expense", description = "경비 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/trips/{tripId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @Operation(summary = "경비 생성", description = "새로운 경비를 등록합니다 (공식 환율 또는 커스텀 환율)")
    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        ExpenseResponse response = expenseService.createExpense(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "경비 목록 조회", description = "여행의 경비 목록과 화폐별 사용량을 조회합니다")
    @GetMapping
    public ResponseEntity<ExpenseListResponse> getExpenses(
        @PathVariable Long tripId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        ExpenseListResponse response = expenseService.getExpensesByTrip(userId, tripId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "경비 수정", description = "경비 정보를 수정합니다")
    @PutMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long expenseId,
            @Valid @RequestBody ExpenseUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        ExpenseResponse response = expenseService.updateExpense(userId, expenseId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "경비 삭제", description = "경비를 삭제합니다")
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId) {
        Long userId = SecurityUtil.getCurrentUserId();
        expenseService.deleteExpense(userId, expenseId);
        return ResponseEntity.noContent().build();
    }
}
