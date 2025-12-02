package io.mero.app.domain.trip.repository;

import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByUserIdOrderByStartDateDesc(Long userId);

}
