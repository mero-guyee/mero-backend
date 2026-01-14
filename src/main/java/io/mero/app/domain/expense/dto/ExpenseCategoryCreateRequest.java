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
public class ExpenseCategoryCreateRequest {

    @NotNull(message = "{expenseCategory.userId.notNull}")
    private Long userId;

    @NotNull(message = "{expenseCategory.name.notNull}")
    @Size(max = 50, message = "{expenseCategory.name.size}")
    private String name;

    private String icon;

    private String color;

}
