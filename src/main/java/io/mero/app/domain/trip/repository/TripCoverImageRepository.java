package io.mero.app.domain.trip.repository;

import io.mero.app.domain.trip.entity.TripCoverImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripCoverImageRepository extends JpaRepository<TripCoverImage, Long> {
    Optional<TripCoverImage> findByTripId(Long tripId);
    void deleteByTripId(Long tripId);
}
