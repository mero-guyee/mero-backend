package io.mero.app.domain.footprint.service;

import io.mero.app.domain.footprint.dto.FootprintCreateRequest;
import io.mero.app.domain.footprint.dto.FootprintResponse;
import io.mero.app.domain.footprint.dto.FootprintUpdateRequest;
import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.footprint.entity.FootprintLocation;
import io.mero.app.domain.footprint.entity.Photo;
import io.mero.app.domain.footprint.repository.FootprintRepository;
import io.mero.app.domain.footprint.repository.PhotoRepository;
import io.mero.app.domain.footprint.util.FootprintLocationMapper;
import io.mero.app.domain.footprint.util.PhotoMapper;
import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.domain.expense.repository.ExpenseRepository;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FootprintService {

    private final FootprintRepository footprintRepository;
    private final PhotoRepository photoRepository;
    private final TripRepository tripRepository;
    private final ExpenseRepository expenseRepository;
    private final MessageUtil messageUtil;

    @Transactional
    public FootprintResponse createFootprint(Long userId, Long tripId, FootprintCreateRequest request) {
        Trip trip = findTripById(tripId);
        validateOwner(trip, userId);

        Footprint footprint = Footprint.builder()
                .trip(trip)
                .title(request.getTitle())
                .content(request.getContent())
                .date(request.getDate())
                .photoUrls(request.getPhotoUrls())
                .build();

        Footprint savedFootprint = footprintRepository.save(footprint);

        List<Photo> photos = PhotoMapper.fromUrls(request.getPhotoUrls(), savedFootprint);
        savedFootprint.updatePhotos(photos);

        List<FootprintLocation> locations = FootprintLocationMapper.fromRequests(request.getLocations(), savedFootprint);
        savedFootprint.updateLocations(locations);

        return FootprintResponse.from(savedFootprint);
    }

    public List<FootprintResponse> getFootprints(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(trip, userId);

        List<Footprint> footprints = footprintRepository.findByTripIdOrderByDateDesc(tripId);
        return footprints.stream()
                .map(FootprintResponse::from)
                .toList();
    }

    public FootprintResponse getFootprint(Long userId, Long tripId, Long footprintId) {
        Footprint footprint = findFootprintById(footprintId);
        Trip trip = footprint.getTrip();

        validateTripMatch(trip, tripId);
        validateOwner(trip, userId);

        List<ExpenseResponse> expenses = expenseRepository.findByFootprint(footprint)
                .stream()
                .map(ExpenseResponse::from)
                .toList();

        return FootprintResponse.from(footprint, expenses);
    }

    @Transactional
    public FootprintResponse updateFootprint(Long userId, Long tripId, Long footprintId, FootprintUpdateRequest request) {
        Footprint footprint = findFootprintById(footprintId);
        Trip trip = footprint.getTrip();

        validateTripMatch(trip, tripId);
        validateOwner(trip, userId);

        footprint.update(
                request.getTitle(),
                request.getContent(),
                request.getDate()
        );

        List<Photo> newPhotos = PhotoMapper.fromUrls(request.getPhotoUrls(), footprint);
        footprint.updatePhotos(newPhotos);

        List<FootprintLocation> newLocations = FootprintLocationMapper.fromRequests(request.getLocations(), footprint);
        footprint.updateLocations(newLocations);

        return FootprintResponse.from(footprint);
    }

    @Transactional
    public void deleteFootprint(Long userId, Long tripId, Long footprintId) {
        Footprint footprint = findFootprintById(footprintId);
        Trip trip = footprint.getTrip();

        validateTripMatch(trip, tripId);
        validateOwner(trip, userId);

        footprintRepository.delete(footprint);
    }

    private Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.trip.notFound")));
    }

    private Footprint findFootprintById(Long footprintId) {
        return footprintRepository.findById(footprintId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.footprint.notFound")));
    }

    private void validateOwner(Trip trip, Long userId) {
        if (!trip.isOwner(userId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }

    private void validateTripMatch(Trip trip, Long tripId) {
        if (!trip.getId().equals(tripId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }
}
