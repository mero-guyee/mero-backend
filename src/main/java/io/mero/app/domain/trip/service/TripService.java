package io.mero.app.domain.trip.service;

import io.mero.app.domain.trip.entity.TripDocument;
import io.mero.app.domain.trip.entity.TripMemo;
import io.mero.app.domain.trip.repository.TripDocumentRepository;
import io.mero.app.domain.trip.repository.TripMemoRepository;
import io.mero.app.domain.trip.dto.TripCreateRequest;
import io.mero.app.domain.trip.dto.TripDetailResponse;
import io.mero.app.domain.trip.dto.TripDocumentResponse;
import io.mero.app.domain.trip.dto.TripMemoCreateRequest;
import io.mero.app.domain.trip.dto.TripMemoResponse;
import io.mero.app.domain.trip.dto.TripMemoUpdateRequest;
import io.mero.app.domain.trip.dto.TripResponse;
import io.mero.app.domain.trip.dto.TripUpdateRequest;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.entity.TripCoverImage;
import io.mero.app.domain.trip.repository.TripCoverImageRepository;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.UserRepository;
import io.mero.app.global.dto.StorageUploadResult;
import io.mero.app.global.enums.DocumentMimeType;
import io.mero.app.global.enums.ImageMimeType;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.service.StorageService;
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
    private final TripDocumentRepository tripDocumentRepository;
    private final TripMemoRepository tripMemoRepository;
    private final StorageService storageService;
    private final MessageUtil messageUtil;

    @Transactional
    public TripResponse createTrip(Long userId, TripCreateRequest request, MultipartFile image) {
        // 멱등성 체크: 동일한 clientId로 이미 생성된 Trip이 있으면 해당 Trip 반환
        return tripRepository.findByClientIdAndUserId(request.getClientId(), userId)
                .map(TripResponse::from)
                .orElseGet(() -> createNewTrip(userId, request, image));
    }

    private TripResponse createNewTrip(Long userId, TripCreateRequest request, MultipartFile image) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageUtil.getMessage("error.user.notFound")
                ));

        Trip trip = Trip.builder()
                .user(user)
                .clientId(request.getClientId())
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

        List<TripDocument> documents = tripDocumentRepository.findByTripId(tripId);

        return TripDetailResponse.from(trip, documents);
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
            storageService.deleteTripCoverImage(trip.getCoverImage().getS3Key());
            tripCoverImageRepository.delete(trip.getCoverImage());
            trip.removeCoverImage();
        }

        // 새 이미지 업로드
        uploadTripCoverImage(userId, image, trip);

        return TripResponse.from(trip);
    }

    private void uploadTripCoverImage(Long userId, MultipartFile image, Trip trip) {
        StorageUploadResult uploadResult = storageService.uploadTripCoverImage(userId, image);
        TripCoverImage coverImage = TripCoverImage.builder()
                .trip(trip)
                .s3Key(uploadResult.getStorageKey())
                .s3Url(uploadResult.getStorageUrl())
                .originalFilename(uploadResult.getOriginalFilename())
                .fileSize(uploadResult.getFileSize())
                .mimeType(ImageMimeType.fromMimeType(uploadResult.getMimeType()))
                .build();
        tripCoverImageRepository.save(coverImage);
        trip.setCoverImage(coverImage);
    }

    @Transactional
    public void deleteTripImage(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        if (trip.getCoverImage() != null) {
            storageService.deleteTripCoverImage(trip.getCoverImage().getS3Key());
            tripCoverImageRepository.delete(trip.getCoverImage());
            trip.removeCoverImage();
        }
    }

    @Transactional
    public TripDocumentResponse uploadTripDocument(Long userId, Long tripId, MultipartFile file) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        StorageUploadResult uploadResult = storageService.uploadTripDocument(userId, tripId, file);

        TripDocument document = TripDocument.builder()
                .trip(trip)
                .originalFileName(uploadResult.getOriginalFilename())
                .storedFileName(uploadResult.getStorageKey())
                .fileUrl(uploadResult.getStorageUrl())
                .fileSize(uploadResult.getFileSize())
                .contentType(DocumentMimeType.fromMimeType(uploadResult.getMimeType()))
                .build();

        TripDocument savedDocument = tripDocumentRepository.save(document);
        return TripDocumentResponse.from(savedDocument);
    }

    @Transactional
    public void deleteTripDocument(Long userId, Long tripId, Long documentId) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        TripDocument document = tripDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.document.notFound")));

        if (!document.getTrip().getId().equals(tripId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }

        storageService.deleteTripDocument(document.getStorageKey());
        tripDocumentRepository.delete(document);
    }

    @Transactional
    public void deleteTrip(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        if (trip.getCoverImage() != null) {
            storageService.deleteTripCoverImage(trip.getCoverImage().getS3Key());
        }

        List<TripDocument> documents = tripDocumentRepository.findByTripId(tripId);
        for (TripDocument document : documents) {
            storageService.deleteTripDocument(document.getStorageKey());
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

    @Transactional
    public TripMemoResponse createTripMemo(Long userId, Long tripId, TripMemoCreateRequest request) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        return tripMemoRepository.findByClientIdAndTripId(request.getClientId(), tripId)
                .map(TripMemoResponse::from)
                .orElseGet(() -> createNewTripMemo(trip, request));
    }

    private TripMemoResponse createNewTripMemo(Trip trip, TripMemoCreateRequest request) {
        TripMemo memo = TripMemo.builder()
                .trip(trip)
                .clientId(request.getClientId())
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        TripMemo savedMemo = tripMemoRepository.save(memo);
        return TripMemoResponse.from(savedMemo);
    }

    public List<TripMemoResponse> getTripMemos(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        return tripMemoRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                .map(TripMemoResponse::from)
                .collect(Collectors.toList());
    }

    public TripMemoResponse getTripMemo(Long userId, Long tripId, Long memoId) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        TripMemo memo = findMemoById(memoId);
        validateMemoOwnership(tripId, memo);

        return TripMemoResponse.from(memo);
    }

    @Transactional
    public TripMemoResponse updateTripMemo(Long userId, Long tripId, Long memoId, TripMemoUpdateRequest request) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        TripMemo memo = findMemoById(memoId);
        validateMemoOwnership(tripId, memo);

        memo.update(request.getTitle(), request.getContent());
        return TripMemoResponse.from(memo);
    }

    @Transactional
    public void deleteTripMemo(Long userId, Long tripId, Long memoId) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        TripMemo memo = findMemoById(memoId);
        validateMemoOwnership(tripId, memo);

        tripMemoRepository.delete(memo);
    }

    private TripMemo findMemoById(Long memoId) {
        return tripMemoRepository.findById(memoId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.memo.notFound")));
    }

    private void validateMemoOwnership(Long tripId, TripMemo memo) {
        if (!memo.getTrip().getId().equals(tripId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }
}
