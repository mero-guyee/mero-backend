package io.mero.app.domain.expense.dto;

import io.mero.app.global.enums.Currency;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseUpdateRequest {

    private Long diaryId;

    @NotNull(message = "{expense.amount.notNull}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{expense.amount.positive}")
    private BigDecimal amount;

    @NotNull(message = "{expense.currency.notNull}")
    private Currency currency;

    @Size(max = 200, message = "{expense.category.size}")
    private String category;

    @Size(max = 1000, message = "{expense.description.size}")
    private String description;

    @NotNull(message = "{expense.date.notNull}")
    private LocalDate date;

    @Size(max = 500, message = "{expense.location.size}")
    private String location;

    @DecimalMin(value = "0.0", inclusive = false, message = "{exchangeRate.rate.positive}")
    private BigDecimal customExchangeRate;

    private String exchangeRateSource;
}