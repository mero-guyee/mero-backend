package io.mero.app.domain.expense.dto;

import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.global.embedded.Location;
import io.mero.app.global.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private Long tripId;
    private Long footprintId;
    private BigDecimal amount;
    private Currency currency;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private String description;
    private LocalDate date;
    private String location;
    private LocalDateTime createdAt;

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getTrip().getId(),
                expense.getFootprint() != null ? expense.getFootprint().getId() : null,
                expense.getAmount(),
                expense.getCurrency(),
                expense.getCategory().getId(),
                expense.getCategory().getName(),
                expense.getCategory().getIcon(),
                expense.getCategory().getColor(),
                expense.getDescription(),
                expense.getDate(),
                expense.getLocation(),
                expense.getCreatedAt()
        );
    }
}
