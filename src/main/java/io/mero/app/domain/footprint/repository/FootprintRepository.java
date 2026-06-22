package io.mero.app.domain.footprint.repository;

import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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

}
