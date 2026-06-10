package io.mero.app.domain.trip.repository;

import io.mero.app.domain.trip.entity.TripMemo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripMemoRepository extends JpaRepository<TripMemo, Long> {
    List<TripMemo> findByTripIdOrderByCreatedAtDesc(Long tripId);

    Optional<TripMemo> findByClientIdAndTripId(String clientId, Long tripId);
}
