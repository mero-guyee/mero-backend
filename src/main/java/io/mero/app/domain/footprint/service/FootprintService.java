package io.mero.app.domain.footprint.service;

import io.mero.app.domain.footprint.dto.FootprintCreateRequest;
import io.mero.app.domain.footprint.dto.FootprintDetailResponse;
import io.mero.app.domain.footprint.dto.FootprintResponse;
import io.mero.app.domain.footprint.dto.FootprintUpdateRequest;
import io.mero.app.domain.footprint.dto.PhotoResponse;
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
import io.mero.app.global.dto.S3UploadResult;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.service.S3Service;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FootprintService {

    private final FootprintRepository footprintRepository;
    private final PhotoRepository photoRepository;
    private final TripRepository tripRepository;
    private final ExpenseRepository expenseRepository;
    private final S3Service s3Service;
    private final MessageUtil messageUtil;

    @Transactional
    public FootprintResponse createFootprint(Long userId, Long tripId, FootprintCreateRequest request) {
        Trip trip = findTripById(tripId);
        validateOwner(trip, userId);

        // 멱등성 체크: 동일한 clientId로 이미 생성된 Footprint가 있으면 해당 Footprint 반환
        return footprintRepository.findByClientIdAndTripId(request.getClientId(), tripId)
                .map(FootprintResponse::from)
                .orElseGet(() -> createNewFootprint(trip, request));
    }

    private FootprintResponse createNewFootprint(Trip trip, FootprintCreateRequest request) {
        Footprint footprint = Footprint.builder()
                .trip(trip)
                .clientId(request.getClientId())
                .title(request.getTitle())
                .content(request.getContent())
                .date(request.getDate())
                .build();

        Footprint savedFootprint = footprintRepository.save(footprint);

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

    public FootprintDetailResponse getFootprint(Long userId, Long tripId, Long footprintId) {
        Footprint footprint = findFootprintById(footprintId);
        Trip trip = footprint.getTrip();

        validateTripMatch(trip, tripId);
        validateOwner(trip, userId);

        List<ExpenseResponse> expenses = expenseRepository.findByFootprint(footprint)
                .stream()
                .map(ExpenseResponse::from)
                .toList();

        return FootprintDetailResponse.from(footprint, expenses);
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

    // === 사진 관리 ===

    @Transactional
    public List<PhotoResponse> uploadPhotos(Long userId, Long tripId, Long footprintId, List<MultipartFile> photos) {
        Footprint footprint = findFootprintById(footprintId);
        Trip trip = footprint.getTrip();

        validateTripMatch(trip, tripId);
        validateOwner(trip, userId);

        List<S3UploadResult> uploadResults = s3Service.uploadFootprintPhotos(userId, tripId, footprintId, photos);

        int startOrderIndex = footprint.getPhotos().size();
        List<Photo> newPhotos = PhotoMapper.fromUploadResults(uploadResults, footprint);
        for (int i = 0; i < newPhotos.size(); i++) {
            newPhotos.get(i).updateOrder(startOrderIndex + i);
        }

        List<Photo> savedPhotos = photoRepository.saveAll(newPhotos);
        footprint.getPhotos().addAll(savedPhotos);

        return savedPhotos.stream()
                .map(PhotoResponse::from)
                .toList();
    }

    @Transactional
    public void deletePhoto(Long userId, Long tripId, Long footprintId, Long photoId) {
        Footprint footprint = findFootprintById(footprintId);
        Trip trip = footprint.getTrip();

        validateTripMatch(trip, tripId);
        validateOwner(trip, userId);

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.photo.notFound")));

        if (!photo.getFootprint().getId().equals(footprintId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }

        footprint.getPhotos().remove(photo);
        photoRepository.delete(photo);
    }
}
