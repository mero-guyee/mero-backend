package io.mero.app.domain.trip.service;

import io.mero.app.domain.trip.dto.TripCreateRequest;
import io.mero.app.domain.trip.dto.TripResponse;
import io.mero.app.domain.trip.dto.TripUpdateRequest;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.UserRepository;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final MessageUtil messageUtil;

    @Transactional
    public TripResponse createTrip(Long userId, TripCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageUtil.getMessage("error.user.notFound")
                ));

        Trip trip = Trip.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .countries(request.getCountries())
                .totalBudget(request.getTotalBudget())
                .budgetCurrency(request.getBudgetCurrency())
                .defaultCurrency(request.getDefaultCurrency())
                .build();


        Trip savedTrip = tripRepository.save(trip);
        return TripResponse.from(savedTrip);
    }

    public List<TripResponse> getTrips(Long userId) {
        List<Trip> trips = tripRepository.findByUserIdOrderByStartDateDesc(userId);
        return trips.stream()
                .map(TripResponse::from)
                .collect(Collectors.toList());
    }

    public TripResponse getTrip(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);

        validateOwner(userId, trip);

        return TripResponse.from(trip);

    }

    @Transactional
    public TripResponse updateTrip(Long userId, Long tripId, TripUpdateRequest request) {
        Trip trip = findTripById(tripId);

        validateOwner(userId, trip);

        trip.update(
                request.getTitle(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate(),
                request.getCountries(),
                request.getTotalBudget(),
                request.getBudgetCurrency(),
                request.getDefaultCurrency()
        );

        return TripResponse.from(trip);
    }

    @Transactional
    public void deleteTrip(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);

        validateOwner(userId, trip);

        tripRepository.delete(trip);
    }

    private Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageUtil.getMessage("error.trip.notFound")
                ));
    }

    private void validateOwner(Long userId, Trip trip) {
        if (!trip.isOwner(userId)) {
            throw new IllegalArgumentException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }
}
