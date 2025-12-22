package io.mero.app.domain.expense.controller;

import io.mero.app.domain.expense.dto.ExpenseCreateRequest;
import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.domain.expense.dto.ExpenseUpdateRequest;
import io.mero.app.domain.expense.service.ExpenseService;
import io.mero.app.global.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        ExpenseResponse response = expenseService.createExpense(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> getExpense(@PathVariable Long expenseId) {
        Long userId = SecurityUtil.getCurrentUserId();
        ExpenseResponse response = expenseService.getExpense(userId, expenseId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long expenseId,
            @Valid @RequestBody ExpenseUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        ExpenseResponse response = expenseService.updateExpense(userId, expenseId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId) {
        Long userId = SecurityUtil.getCurrentUserId();
        expenseService.deleteExpense(userId, expenseId);
        return ResponseEntity.noContent().build();
    }
}
