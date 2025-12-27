package io.mero.app.domain.trip.dto;

import io.mero.app.global.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class TripStatisticsResponse {

    // 기본 정보
    private Long tripId;
    private String tripTitle;
    private Currency currency;

    // 총 지출
    private BigDecimal totalExpense;
    private Integer expenseCount;

    // 예산 정보
    private BigDecimal budget;
    private BigDecimal budgetRemaining;
    private Double budgetUsagePercentage;

    // 카테고리별 지출
    private List<CategoryExpense> expensesByCategory;

    // 일별 지출
    private List<DailyExpense> expensesByDate;

    // 통화별 지출
    private List<CurrencyExpense> expensesByCurrency;

    @Getter
    @AllArgsConstructor
    public static class CategoryExpense {
        private String category;
        private BigDecimal amount;
        private Double percentage;
        private Integer count;
    }

    @Getter
    @AllArgsConstructor
    public static class DailyExpense {
        private LocalDate date;
        private BigDecimal amount;
        private Integer count;
    }

    @Getter
    @AllArgsConstructor
    public static class CurrencyExpense {
        private Currency currency;
        private BigDecimal originalAmount;
        private BigDecimal convertedAmount;
        private Integer count;
    }
}

