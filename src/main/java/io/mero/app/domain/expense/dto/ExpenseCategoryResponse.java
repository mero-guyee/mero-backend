package io.mero.app.domain.expense.dto;

import io.mero.app.domain.expense.entity.ExpenseCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExpenseCategoryResponse {
    private Long id;
    private String name;
    private String icon;
    private String color;
    private boolean isDefault;

    public static ExpenseCategoryResponse from(ExpenseCategory category) {
        return ExpenseCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .color(category.getColor())
                .build();
    }
}
