package io.mero.app.domain.budget.dto;

import io.mero.app.domain.budget.entity.Budget;
import io.mero.app.global.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BudgetResponse {

    private Long id;
    private String clientId;
    private Long tripId;
    private BigDecimal amount;
    private Currency currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BudgetResponse from(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getClientId(),
                budget.getTrip().getId(),
                budget.getAmount(),
                budget.getCurrency(),
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }
}
