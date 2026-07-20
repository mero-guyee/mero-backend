package io.mero.app.domain.expense.repository;

import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.entity.ExpenseCategory;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByTripOrderByDateDesc(Trip trip);

    List<Expense> findByFootprint(Footprint footprint);

    List<Expense> findByCategory(ExpenseCategory category);

    // soft delete(@SQLRestriction)를 무시하고 삭제된 행까지 조회 (clientId 재사용/복구용)
    @Query(value = "SELECT * FROM expenses WHERE client_id = :clientId AND trip_id = :tripId LIMIT 1",
            nativeQuery = true)
    Optional<Expense> findByClientIdAndTripIdIncludingDeleted(@Param("clientId") String clientId,
                                                              @Param("tripId") Long tripId);

}