package io.mero.app.domain.budget.dto;

import io.mero.app.global.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BudgetUpdateRequest {

    @NotNull(message = "{budget.amount.notNull}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{budget.amount.positive}")
    private BigDecimal amount;

    @NotNull(message = "{budget.currency.notNull}")
    private Currency currency;

    @DecimalMin(value = "0.0", inclusive = false, message = "{budget.exchangeRate.positive}")
    private BigDecimal exchangeRate;
}
