package io.mero.app.domain.budget.service;

import io.mero.app.domain.budget.dto.BudgetCreateRequest;
import io.mero.app.domain.budget.dto.BudgetResponse;
import io.mero.app.domain.budget.dto.BudgetUpdateRequest;
import io.mero.app.domain.budget.entity.Budget;
import io.mero.app.domain.budget.repository.BudgetRepository;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TripRepository tripRepository;
    private final MessageUtil messageUtil;

    @Transactional
    public BudgetResponse createBudget(Long userId, Long tripId, BudgetCreateRequest request) {
        Trip trip = findTripById(tripId);
        validateOwner(trip, userId);

        if (budgetRepository.findByTripAndCurrency(trip, request.getCurrency()).isPresent()) {
            throw new BadRequestException(
                    messageUtil.getMessage("error.budget.duplicateCurrency"));
        }

        Budget budget = Budget.builder()
                .trip(trip)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .build();

        Budget savedBudget = budgetRepository.save(budget);
        return BudgetResponse.from(savedBudget);
    }

    public List<BudgetResponse> getBudgetsByTrip(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(trip, userId);

        List<Budget> budgets = budgetRepository.findByTripOrderByCreatedAtDesc(trip);
        return budgets.stream()
                .map(BudgetResponse::from)
                .toList();
    }


    @Transactional
    public BudgetResponse updateBudget(Long userId, Long tripId, Long budgetId,
                                       BudgetUpdateRequest request) {
        Budget budget = findBudgetById(budgetId);
        Trip trip = budget.getTrip();

        validateTripMatch(trip, tripId);
        validateOwner(trip, userId);

        if (!budget.getCurrency().equals(request.getCurrency())) {
            if (budgetRepository.findByTripAndCurrency(trip, request.getCurrency()).isPresent()) {
                throw new BadRequestException(
                        messageUtil.getMessage("error.budget.duplicateCurrency"));
            }
        }

        budget.update(request.getAmount(), request.getCurrency());
        return BudgetResponse.from(budget);
    }

    @Transactional
    public void deleteBudget(Long userId, Long tripId, Long budgetId) {
        Budget budget = findBudgetById(budgetId);
        Trip trip = budget.getTrip();

        validateTripMatch(trip, tripId);
        validateOwner(trip, userId);

        budgetRepository.delete(budget);
    }

    private Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.trip.notFound")));
    }

    private Budget findBudgetById(Long budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.budget.notFound")));
    }

    private void validateOwner(Trip trip, Long userId) {
        if (!trip.isOwner(userId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }

    private void validateTripMatch(Trip trip, Long tripId) {
        if (!trip.getId().equals(tripId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }
}
