package io.mero.app.domain.budget.repository;

import io.mero.app.domain.budget.entity.Budget;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.global.enums.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByTripOrderByCreatedAtDesc(Trip trip);

    Optional<Budget> findByTripAndCurrency(Trip trip, Currency currency);

    Optional<Budget> findByClientIdAndTripId(String clientId, Long tripId);
}
