package io.mero.app.domain.expense.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategoryCreateRequest {

    @NotNull(message = "{expenseCategory.clientId.notNull}")
    @Size(max = 36, message = "{expenseCategory.clientId.size}")
    private String clientId;

    @NotNull(message = "{expenseCategory.userId.notNull}")
    private Long userId;

    @NotNull(message = "{expenseCategory.name.notNull}")
    @Size(max = 50, message = "{expenseCategory.name.size}")
    private String name;

    private String icon;

    private String color;

}
