package io.mero.app.domain.trip.service;

import io.mero.app.domain.file.entity.File;
import io.mero.app.domain.file.repository.FileRepository;
import io.mero.app.domain.trip.dto.TripCreateRequest;
import io.mero.app.domain.trip.dto.TripDetailResponse;
import io.mero.app.domain.trip.dto.TripResponse;
import io.mero.app.domain.trip.dto.TripUpdateRequest;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.entity.TripCoverImage;
import io.mero.app.domain.trip.repository.TripCoverImageRepository;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.UserRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripCoverImageRepository tripCoverImageRepository;
    private final FileRepository fileRepository;
    private final S3Service s3Service;
    private final MessageUtil messageUtil;

    @Transactional
    public TripResponse createTrip(Long userId, TripCreateRequest request, MultipartFile image) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageUtil.getMessage("error.user.notFound")
                ));

        Trip trip = Trip.builder()
                .user(user)
                .title(request.getTitle())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .countries(request.getCountries())
                .build();

        Trip savedTrip = tripRepository.save(trip);

        if (image != null && !image.isEmpty()) {
            uploadTripCoverImage(userId, image, savedTrip);
        }

        return TripResponse.from(savedTrip);
    }

    public List<TripResponse> getTrips(Long userId) {
        List<Trip> trips = tripRepository.findByUserIdOrderByStartDateDesc(userId);
        return trips.stream()
                .map(TripResponse::from)
                .collect(Collectors.toList());
    }

    public TripDetailResponse getTrip(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        List<File> files = fileRepository.findByTripId(tripId);

        return TripDetailResponse.from(trip, files);
    }

    @Transactional
    public TripResponse updateTrip(Long userId, Long tripId, TripUpdateRequest request) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        trip.update(
                request.getTitle(),
                request.getStartDate(),
                request.getEndDate(),
                request.getCountries()
        );

        return TripResponse.from(trip);
    }

    @Transactional
    public TripResponse updateTripImage(Long userId, Long tripId, MultipartFile image) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        // 기존 이미지 삭제
        if (trip.getCoverImage() != null) {
            s3Service.deleteTripCoverImage(trip.getCoverImage().getS3Key());
            tripCoverImageRepository.delete(trip.getCoverImage());
            trip.removeCoverImage();
        }

        // 새 이미지 업로드
        uploadTripCoverImage(userId, image, trip);

        return TripResponse.from(trip);
    }

    private void uploadTripCoverImage(Long userId, MultipartFile image, Trip trip) {
        S3UploadResult uploadResult = s3Service.uploadTripCoverImage(userId, image);
        TripCoverImage coverImage = TripCoverImage.builder()
                .trip(trip)
                .s3Key(uploadResult.getS3Key())
                .s3Url(uploadResult.getS3Url())
                .originalFilename(uploadResult.getOriginalFilename())
                .fileSize(uploadResult.getFileSize())
                .mimeType(uploadResult.getMimeType())
                .build();
        tripCoverImageRepository.save(coverImage);
        trip.setCoverImage(coverImage);
    }

    @Transactional
    public void deleteTripImage(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        if (trip.getCoverImage() != null) {
            s3Service.deleteTripCoverImage(trip.getCoverImage().getS3Key());
            tripCoverImageRepository.delete(trip.getCoverImage());
            trip.removeCoverImage();
        }
    }

    @Transactional
    public void deleteTrip(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        if (trip.getCoverImage() != null) {
            s3Service.deleteTripCoverImage(trip.getCoverImage().getS3Key());
        }

        tripRepository.delete(trip);
    }

    private Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.trip.notFound")));
    }

    private void validateOwner(Long userId, Trip trip) {
        if (!trip.isOwner(userId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }
}
