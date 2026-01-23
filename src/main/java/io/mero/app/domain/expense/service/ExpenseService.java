package io.mero.app.domain.expense.service;

import io.mero.app.domain.budget.entity.Budget;
import io.mero.app.domain.budget.repository.BudgetRepository;
import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.footprint.repository.FootprintRepository;
import io.mero.app.domain.expense.dto.CurrencyUsageDto;
import io.mero.app.domain.expense.dto.ExpenseCreateRequest;
import io.mero.app.domain.expense.dto.ExpenseListResponse;
import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.domain.expense.dto.ExpenseUpdateRequest;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.entity.ExpenseCategory;
import io.mero.app.domain.expense.repository.ExpenseCategoryRepository;
import io.mero.app.domain.expense.repository.ExpenseRepository;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final TripRepository tripRepository;
    private final FootprintRepository footprintRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final MessageUtil messageUtil;

    @Transactional
    public ExpenseResponse createExpense(Long userId, ExpenseCreateRequest request) {
        Trip trip = findTripById(request.getTripId());
        validateOwner(trip, userId);

        // 멱등성 체크: 동일한 clientId로 이미 생성된 Expense가 있으면 해당 Expense 반환
        return expenseRepository.findByClientIdAndTripId(request.getClientId(), request.getTripId())
                .map(ExpenseResponse::from)
                .orElseGet(() -> createNewExpense(userId, trip, request));
    }

    private ExpenseResponse createNewExpense(Long userId, Trip trip, ExpenseCreateRequest request) {
        Footprint footprint = null;
        if (request.getFootprintId() != null) {
            footprint = findFootprintById(request.getFootprintId());
            validateFootprintBelongsToTrip(footprint, trip);
        }

        ExpenseCategory category = findExpenseCategoryById(request.getCategoryId());
        validateCategoryOwner(category, userId);

        Expense expense = Expense.builder()
                .trip(trip)
                .clientId(request.getClientId())
                .footprint(footprint)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .category(category)
                .description(request.getDescription())
                .date(request.getDate())
                .location(request.getLocation())
                .build();

        Expense savedExpense = expenseRepository.save(expense);

        return ExpenseResponse.from(savedExpense);
    }

    public ExpenseListResponse getExpensesByTrip(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(trip, userId);

        List<Expense> expenses = expenseRepository.findByTripOrderByDateDesc(trip);
        List<ExpenseResponse> expenseResponses = expenses.stream()
                .map(ExpenseResponse::from)
                .collect(Collectors.toList());

        List<CurrencyUsageDto> currencyUsages = calculateCurrencyUsages(trip, expenses);

        return new ExpenseListResponse(expenseResponses, currencyUsages);
    }

    private List<CurrencyUsageDto> calculateCurrencyUsages(Trip trip, List<Expense> expenses) {
        List<Budget> budgets = budgetRepository.findByTripOrderByCreatedAtDesc(trip);

        Map<Currency, BigDecimal> expensesByCurrency = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCurrency,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Expense::getAmount,
                                BigDecimal::add
                        )
                ));

        return budgets.stream()
                .map(budget -> {
                    Currency currency = budget.getCurrency();
                    BigDecimal totalSpent = expensesByCurrency.getOrDefault(currency, BigDecimal.ZERO);
                    return CurrencyUsageDto.of(currency, totalSpent, budget.getAmount());
                })
                .collect(Collectors.toList());
    }

    public ExpenseResponse getExpense(Long userId, Long expenseId) {
        Expense expense = findExpenseById(expenseId);
        validateOwner(expense.getTrip(), userId);

        return ExpenseResponse.from(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long userId, Long expenseId, ExpenseUpdateRequest request) {
        Expense expense = findExpenseById(expenseId);
        validateOwner(expense.getTrip(), userId);

        // Footprint 연결/해제
        if (request.getFootprintId() != null) {
            Footprint footprint = findFootprintById(request.getFootprintId());
            validateFootprintBelongsToTrip(footprint, expense.getTrip());
            expense.linkToFootprint(footprint);
        } else {
            expense.unlinkFromFootprint();
        }

        ExpenseCategory category = findExpenseCategoryById(request.getCategoryId());
        validateCategoryOwner(category, userId);

        expense.update(
                request.getAmount(),
                request.getCurrency(),
                category,
                request.getDescription(),
                request.getDate(),
                request.getLocation()
        );

        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void deleteExpense(Long userId, Long expenseId) {
        Expense expense = findExpenseById(expenseId);
        validateOwner(expense.getTrip(), userId);

        expenseRepository.delete(expense);
    }

    private Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.trip.notFound")));
    }

    private Expense findExpenseById(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.expense.notFound")));
    }

    private Footprint findFootprintById(Long footprintId) {
        return footprintRepository.findById(footprintId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.footprint.notFound")));
    }

    private void validateOwner(Trip trip, Long userId) {
        if (!trip.isOwner(userId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }

    private void validateFootprintBelongsToTrip(Footprint footprint, Trip trip) {
        if (!footprint.getTrip().equals(trip)) {
            throw new BadRequestException(
                    messageUtil.getMessage("error.footprint.tripMismatch"));
        }
    }

    private ExpenseCategory findExpenseCategoryById(Long categoryId) {
        return expenseCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.expenseCategory.notFound")));
    }

    private void validateCategoryOwner(ExpenseCategory category, Long userId) {
        if (!category.getUser().getId().equals(userId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }
}