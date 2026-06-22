package io.mero.app.domain.trip.repository;

import io.mero.app.domain.trip.entity.TripMemo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripMemoRepository extends JpaRepository<TripMemo, Long> {
    List<TripMemo> findByTripIdOrderByCreatedAtDesc(Long tripId);

    // soft delete(@SQLRestriction)를 무시하고 삭제된 행까지 조회 (clientId 재사용/복구용)
    @Query(value = "SELECT * FROM trip_memos WHERE client_id = :clientId AND trip_id = :tripId LIMIT 1",
            nativeQuery = true)
    Optional<TripMemo> findByClientIdAndTripIdIncludingDeleted(@Param("clientId") String clientId,
                                                               @Param("tripId") Long tripId);

    // 여행 hard delete 시, cascade 대상이 아닌 메모를 직접 정리 (soft delete 포함)
    @Modifying
    @Query(value = "DELETE FROM trip_memos WHERE trip_id = :tripId", nativeQuery = true)
    void deleteAllByTripId(@Param("tripId") Long tripId);
}
