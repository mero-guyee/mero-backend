package io.mero.app.domain.expense.dto;

import io.mero.app.global.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@AllArgsConstructor
public class CurrencyUsageDto {
    private Currency currency;
    private BigDecimal totalSpent;
    private BigDecimal budget;
    private BigDecimal usagePercent;

    public static CurrencyUsageDto of(Currency currency, BigDecimal totalSpent, BigDecimal budget) {
        BigDecimal usagePercent = BigDecimal.ZERO;
        if (budget.compareTo(BigDecimal.ZERO) > 0) {
            usagePercent = totalSpent
                    .multiply(BigDecimal.valueOf(100))
                    .divide(budget, 2, RoundingMode.HALF_UP);
        }

        return new CurrencyUsageDto(currency, totalSpent, budget, usagePercent);
    }
}
