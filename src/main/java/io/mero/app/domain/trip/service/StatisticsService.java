package io.mero.app.domain.trip.service;

import io.mero.app.domain.exchange.service.ExchangeRateService;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.repository.ExpenseRepository;
import io.mero.app.domain.trip.dto.TripStatisticsResponse;
import io.mero.app.domain.trip.dto.TripStatisticsResponse.CategoryExpense;
import io.mero.app.domain.trip.dto.TripStatisticsResponse.CurrencyExpense;
import io.mero.app.domain.trip.dto.TripStatisticsResponse.DailyExpense;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final TripRepository tripRepository;
    private final ExpenseRepository expenseRepository;
    private final ExchangeRateService exchangeRateService;
    private final MessageUtil messageUtil;

    /**
     * 여행 통계 조회
     */
    public TripStatisticsResponse getTripStatistics(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(trip, userId);

        List<Expense> expenses = expenseRepository.findByTripOrderByDateDesc(trip);
        Currency baseCurrency = trip.getDefaultCurrency();

        // 총 지출 계산
        BigDecimal totalExpense = calculateTotalExpense(expenses, baseCurrency);

        // 예산 정보 계산
        BigDecimal budget = trip.getTotalBudget();
        BigDecimal budgetRemaining = budget != null ? budget.subtract(totalExpense) : null;
        Double budgetUsagePercentage = calculateBudgetUsage(totalExpense, budget);

        // 카테고리별 지출
        List<CategoryExpense> expensesByCategory = calculateExpensesByCategory(expenses, baseCurrency, totalExpense);

        // 일별 지출
        List<DailyExpense> expensesByDate = calculateExpensesByDate(expenses, baseCurrency);

        // 통화별 지출
        List<CurrencyExpense> expensesByCurrency = calculateExpensesByCurrency(expenses, baseCurrency);

        return new TripStatisticsResponse(
                trip.getId(),
                trip.getTitle(),
                baseCurrency,
                totalExpense,
                expenses.size(),
                budget,
                budgetRemaining,
                budgetUsagePercentage,
                expensesByCategory,
                expensesByDate,
                expensesByCurrency
        );
    }

    /**
     * 총 지출 계산
     */
    private BigDecimal calculateTotalExpense(List<Expense> expenses, Currency baseCurrency) {
        return expenses.stream()
                .map(expense -> {
                    BigDecimal rate = exchangeRateService.getRate(
                            expense.getCurrency(),
                            baseCurrency,
                            expense.getDate()
                    );
                    return expense.getAmount().multiply(rate);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 예산 사용률 계산
     */
    private Double calculateBudgetUsage(BigDecimal totalExpense, BigDecimal budget) {
        if (budget == null || budget.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return totalExpense.divide(budget, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    /**
     * 카테고리별 지출 계산
     */
    private List<CategoryExpense> calculateExpensesByCategory(List<Expense> expenses,
                                                              Currency baseCurrency,
                                                              BigDecimal totalExpense) {
        Map<String, List<Expense>> groupedByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getCategory() != null ? expense.getCategory() : "기타"
                ));

        return groupedByCategory.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<Expense> categoryExpenses = entry.getValue();

                    BigDecimal categoryTotal = categoryExpenses.stream()
                            .map(expense -> {
                                BigDecimal rate = exchangeRateService.getRate(
                                        expense.getCurrency(),
                                        baseCurrency,
                                        expense.getDate()
                                );
                                return expense.getAmount().multiply(rate);
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);

                    Double percentage = totalExpense.compareTo(BigDecimal.ZERO) > 0
                            ? categoryTotal.divide(totalExpense, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .doubleValue()
                            : 0.0;

                    return new CategoryExpense(
                            category,
                            categoryTotal,
                            percentage,
                            categoryExpenses.size()
                    );
                })
                .sorted(Comparator.comparing(CategoryExpense::getAmount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 일별 지출 계산
     */
    private List<DailyExpense> calculateExpensesByDate(List<Expense> expenses, Currency baseCurrency) {
        Map<LocalDate, List<Expense>> groupedByDate = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getDate));

        return groupedByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<Expense> dateExpenses = entry.getValue();

                    BigDecimal dailyTotal = dateExpenses.stream()
                            .map(expense -> {
                                BigDecimal rate = exchangeRateService.getRate(
                                        expense.getCurrency(),
                                        baseCurrency,
                                        expense.getDate()
                                );
                                return expense.getAmount().multiply(rate);
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);

                    return new DailyExpense(
                            date,
                            dailyTotal,
                            dateExpenses.size()
                    );
                })
                .sorted(Comparator.comparing(DailyExpense::getDate))
                .collect(Collectors.toList());
    }

    /**
     * 통화별 지출 계산
     */
    private List<CurrencyExpense> calculateExpensesByCurrency(List<Expense> expenses, Currency baseCurrency) {
        Map<Currency, List<Expense>> groupedByCurrency = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCurrency));

        return groupedByCurrency.entrySet().stream()
                .map(entry -> {
                    Currency currency = entry.getKey();
                    List<Expense> currencyExpenses = entry.getValue();

                    BigDecimal originalTotal = currencyExpenses.stream()
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);

                    BigDecimal convertedTotal = currencyExpenses.stream()
                            .map(expense -> {
                                BigDecimal rate = exchangeRateService.getRate(
                                        expense.getCurrency(),
                                        baseCurrency,
                                        expense.getDate()
                                );
                                return expense.getAmount().multiply(rate);
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);

                    return new CurrencyExpense(
                            currency,
                            originalTotal,
                            convertedTotal,
                            currencyExpenses.size()
                    );
                })
                .sorted(Comparator.comparing(CurrencyExpense::getConvertedAmount).reversed())
                .collect(Collectors.toList());
    }

    // ===== Private 메서드 =====

    private Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.trip.notFound")));
    }

    private void validateOwner(Trip trip, Long userId) {
        if (!trip.isOwner(userId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }
}
