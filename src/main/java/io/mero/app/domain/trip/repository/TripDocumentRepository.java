package io.mero.app.domain.trip.repository;

import io.mero.app.domain.trip.entity.TripDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripDocumentRepository extends JpaRepository<TripDocument, Long> {
    List<TripDocument> findByTripId(Long tripId);

    Optional<TripDocument> findByClientIdAndTripId(String clientId, Long tripId);
}
