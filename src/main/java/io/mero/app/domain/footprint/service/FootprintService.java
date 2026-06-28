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
import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.repository.ExpenseRepository;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.global.dto.StorageUploadResult;
import io.mero.app.global.enums.ImageMimeType;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.service.StorageService;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FootprintService {

    private final FootprintRepository footprintRepository;
    private final PhotoRepository photoRepository;
    private final TripRepository tripRepository;
    private final ExpenseRepository expenseRepository;
    private final StorageService storageService;
    private final MessageUtil messageUtil;

    @Transactional
    public FootprintResponse createFootprint(Long userId, Long tripId, FootprintCreateRequest request) {
        Trip trip = findTripById(tripId);
        validateOwner(trip, userId);

        // 멱등성 체크: 동일한 clientId 행이 있으면 재사용. soft delete된 경우 복구 후 업데이트.
        return footprintRepository.findByClientIdAndTripIdIncludingDeleted(request.getClientId(), tripId)
                .map(footprint -> restoreOrKeepFootprint(footprint, request))
                .orElseGet(() -> createNewFootprint(trip, request));
    }

    private FootprintResponse restoreOrKeepFootprint(Footprint footprint, FootprintCreateRequest request) {
        if (footprint.isDeleted()) {
            footprint.restore();
            footprint.update(request.getTitle(), request.getContent(),
                    request.getDate(), request.getWeatherInfo());
            List<FootprintLocation> locations =
                    FootprintLocationMapper.fromRequests(request.getLocations(), footprint);
            footprint.updateLocations(locations);
        }
        return toFootprintResponse(footprint);
    }

    private FootprintResponse createNewFootprint(Trip trip, FootprintCreateRequest request) {
        Footprint footprint = Footprint.builder()
                .trip(trip)
                .clientId(request.getClientId())
                .title(request.getTitle())
                .content(request.getContent())
                .date(request.getDate())
                .weatherInfo(request.getWeatherInfo())
                .build();

        Footprint savedFootprint = footprintRepository.save(footprint);

        List<FootprintLocation> locations = FootprintLocationMapper.fromRequests(request.getLocations(), savedFootprint);
        savedFootprint.updateLocations(locations);

        return toFootprintResponse(savedFootprint);
    }

    public List<FootprintResponse> getFootprints(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(trip, userId);

        List<Footprint> footprints = footprintRepository.findByTripIdOrderByDateDesc(tripId);
        return footprints.stream()
                .map(this::toFootprintResponse)
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

        return FootprintDetailResponse.from(footprint, photoResponses(footprint), expenses);
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
                request.getDate(),
                request.getWeatherInfo()
        );

        List<FootprintLocation> newLocations = FootprintLocationMapper.fromRequests(request.getLocations(), footprint);
        footprint.updateLocations(newLocations);

        return toFootprintResponse(footprint);
    }

    @Transactional
    public void deleteFootprint(Long userId, Long tripId, Long footprintId) {
        Footprint footprint = findFootprintById(footprintId);
        Trip trip = footprint.getTrip();

        validateTripMatch(trip, tripId);
        validateOwner(trip, userId);

        expenseRepository.findByFootprint(footprint)
                .forEach(Expense::unlinkFromFootprint);

        footprint.delete();
    }

    private FootprintResponse toFootprintResponse(Footprint footprint) {
        List<PhotoResponse> photos = photoResponses(footprint);
        String thumbnailUrl = photos.isEmpty() ? null : photos.get(0).getS3Url();
        return FootprintResponse.from(footprint, thumbnailUrl, photos);
    }

    private List<PhotoResponse> photoResponses(Footprint footprint) {
        return footprint.getPhotos().stream()
                .sorted(Comparator.comparing(Photo::getOrderIndex))
                .map(this::toPhotoResponse)
                .toList();
    }

    private PhotoResponse toPhotoResponse(Photo photo) {
        return PhotoResponse.from(photo, storageService.getImageSignedUrl(photo.getS3Key()));
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
    public PhotoResponse uploadPhoto(Long userId, Long tripId, Long footprintId,
                                     String clientId, MultipartFile photo) {
        Footprint footprint = findFootprintById(footprintId);
        Trip trip = footprint.getTrip();

        validateTripMatch(trip, tripId);
        validateOwner(trip, userId);

        // 멱등성 체크: soft delete된 사진은 파일을 유지하므로 재업로드 없이 복구.
        Optional<Photo> existing = photoRepository.findByClientIdIncludingDeleted(clientId);
        if (existing.isPresent()) {
            Photo existingPhoto = existing.get();
            if (existingPhoto.isDeleted()) {
                existingPhoto.restore();
            }
            return toPhotoResponse(existingPhoto);
        }

        StorageUploadResult uploadResult = storageService
                .uploadFootprintPhotos(userId, tripId, footprintId, List.of(photo))
                .get(0);

        Photo newPhoto = Photo.builder()
                .footprint(footprint)
                .clientId(clientId)
                .s3Key(uploadResult.getStorageKey())
                .originalFilename(uploadResult.getOriginalFilename())
                .fileSize(uploadResult.getFileSize())
                .mimeType(ImageMimeType.fromMimeType(uploadResult.getMimeType()))
                .orderIndex(footprint.getPhotos().size())
                .build();

        Photo saved = photoRepository.save(newPhoto);
        footprint.getPhotos().add(saved);

        return toPhotoResponse(saved);
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

        photo.delete();
    }
}
