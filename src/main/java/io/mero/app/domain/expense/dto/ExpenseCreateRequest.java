package io.mero.app.domain.expense.dto;


import io.mero.app.global.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCreateRequest {

    @NotNull(message = "{expense.clientId.notNull}")
    @Size(max = 36, message = "{expense.clientId.size}")
    private String clientId;

    private Long footprintId;

    @NotNull(message = "{expense.amount.notNull}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{expense.amount.positive}")
    private BigDecimal amount;

    @NotNull(message = "{expense.currency.notNull}")
    private Currency currency;

    @NotNull(message = "{expense.categoryId.notNull}")
    private Long categoryId;

    @Size(max = 1000, message = "{expense.description.size}")
    private String description;

    @NotNull(message = "{expense.date.notNull}")
    private LocalDate date;

    @Size(max = 500, message = "{expense.location.size}")
    private String location;
}
