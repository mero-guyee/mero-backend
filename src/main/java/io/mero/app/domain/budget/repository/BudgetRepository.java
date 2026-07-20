package io.mero.app.domain.budget.repository;

import io.mero.app.domain.budget.entity.Budget;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.global.enums.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByTripOrderByCreatedAtDesc(Trip trip);

    Optional<Budget> findByTripAndCurrency(Trip trip, Currency currency);

    // soft delete(@SQLRestriction)를 무시하고 삭제된 행까지 조회 (clientId 재사용/복구용)
    @Query(value = "SELECT * FROM budgets WHERE client_id = :clientId AND trip_id = :tripId LIMIT 1",
            nativeQuery = true)
    Optional<Budget> findByClientIdAndTripIdIncludingDeleted(@Param("clientId") String clientId,
                                                             @Param("tripId") Long tripId);

    // 여행 hard delete 시, cascade로 정리되지 않는 soft delete된 예산을 직접 제거
    @Modifying
    @Query(value = "DELETE FROM budgets WHERE trip_id = :tripId AND deleted_at IS NOT NULL", nativeQuery = true)
    void deleteSoftDeletedByTripId(@Param("tripId") Long tripId);
}
