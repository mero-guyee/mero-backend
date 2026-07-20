package io.mero.app.domain.footprint.repository;

import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FootprintRepository extends JpaRepository<Footprint, Long> {

    List<Footprint> findByTripIdOrderByDateDesc(Long tripId);

    // soft delete(@SQLRestriction)를 무시하고 삭제된 행까지 조회 (clientId 재사용/복구용)
    @Query(value = "SELECT * FROM footprint WHERE client_id = :clientId AND trip_id = :tripId LIMIT 1",
            nativeQuery = true)
    Optional<Footprint> findByClientIdAndTripIdIncludingDeleted(@Param("clientId") String clientId,
                                                                @Param("tripId") Long tripId);

    // soft delete된 발자취의 위치 정보 제거 (발자취 행 삭제 시 FK 위반 방지, footprint_location엔 ON DELETE CASCADE 없음)
    @Modifying
    @Query(value = "DELETE FROM footprint_location WHERE footprint_id IN " +
            "(SELECT id FROM footprint WHERE trip_id = :tripId AND deleted_at IS NOT NULL)", nativeQuery = true)
    void deleteLocationsByDeletedFootprintTripId(@Param("tripId") Long tripId);

    // @SQLRestriction에 가려져 cascade로 정리되지 않는 soft delete된 발자취를 직접 제거
    @Modifying
    @Query(value = "DELETE FROM footprint WHERE trip_id = :tripId AND deleted_at IS NOT NULL", nativeQuery = true)
    void deleteSoftDeletedByTripId(@Param("tripId") Long tripId);

}
