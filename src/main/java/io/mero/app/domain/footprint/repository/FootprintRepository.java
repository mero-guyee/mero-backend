package io.mero.app.domain.footprint.repository;

import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FootprintRepository extends JpaRepository<Footprint, Long> {

    List<Footprint> findByTripIdOrderByDateDesc(Long tripId);

    Optional<Footprint> findByClientIdAndTripId(String clientId, Long tripId);

}
