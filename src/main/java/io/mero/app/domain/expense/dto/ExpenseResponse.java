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
    private Long diaryId;
    private BigDecimal amount;
    private Currency currency;
    private String category;
    private String description;
    private LocalDate date;
    private String location;

    // 환율 정보
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;
    private Currency targetCurrency;
    private boolean customRate;
    private String exchangeRateSource;

    private LocalDateTime createdAt;

    public static ExpenseResponse from(Expense expense,
                                       BigDecimal exchangeRate,
                                       Currency targetCurrency,
                                       boolean customRate,
                                       String exchangeRateSource) {
        BigDecimal convertedAmount = expense.getAmount()
                .multiply(exchangeRate)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        return new ExpenseResponse(
                expense.getId(),
                expense.getTrip().getId(),
                expense.getDiary() != null ? expense.getDiary().getId() : null,
                expense.getAmount(),
                expense.getCurrency(),
                expense.getCategory(),
                expense.getDescription(),
                expense.getDate(),
                expense.getLocation(),
                exchangeRate,
                convertedAmount,
                targetCurrency,
                customRate,
                exchangeRateSource,
                expense.getCreatedAt()
        );
    }
}
