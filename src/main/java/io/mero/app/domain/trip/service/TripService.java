package io.mero.app.domain.trip.service;

import io.mero.app.domain.trip.entity.TripDocument;
import io.mero.app.domain.trip.entity.TripMemo;
import io.mero.app.domain.budget.repository.BudgetRepository;
import io.mero.app.domain.footprint.repository.FootprintRepository;
import io.mero.app.domain.footprint.repository.PhotoRepository;
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
import io.mero.app.global.service.StorageCleaner;
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
    private final BudgetRepository budgetRepository;
    private final PhotoRepository photoRepository;
    private final FootprintRepository footprintRepository;
    private final StorageService storageService;
    private final StorageCleaner storageCleaner;
    private final MessageUtil messageUtil;

    @Transactional
    public TripResponse createTrip(Long userId, TripCreateRequest request, MultipartFile image) {
        // 멱등성 체크: 동일한 clientId로 이미 생성된 Trip이 있으면 해당 Trip 반환
        return tripRepository.findByClientIdAndUserId(request.getClientId(), userId)
                .map(this::toTripResponse)
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

        return toTripResponse(savedTrip);
    }

    public List<TripResponse> getTrips(Long userId) {
        List<Trip> trips = tripRepository.findByUserIdOrderByStartDateDesc(userId);
        return trips.stream()
                .map(this::toTripResponse)
                .collect(Collectors.toList());
    }

    public TripDetailResponse getTrip(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);
        return TripDetailResponse.from(trip, coverSignedUrl(trip));
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

        return toTripResponse(trip);
    }

    @Transactional
    public TripResponse updateTripImage(Long userId, Long tripId, MultipartFile image) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        // 기존 이미지 삭제 (스토리지 파일은 커밋 후에 지운다)
        if (trip.getCoverImage() != null) {
            storageCleaner.deleteTripCoverImage(trip.getCoverImage().getStorageKey());
            tripCoverImageRepository.delete(trip.getCoverImage());
            trip.removeCoverImage();
            tripCoverImageRepository.flush();
        }

        // 새 이미지 업로드
        uploadTripCoverImage(userId, image, trip);

        return toTripResponse(trip);
    }

    private void uploadTripCoverImage(Long userId, MultipartFile image, Trip trip) {
        StorageUploadResult uploadResult = storageService.uploadTripCoverImage(userId, image);
        TripCoverImage coverImage = TripCoverImage.builder()
                .trip(trip)
                .storageKey(uploadResult.getStorageKey())
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
            storageCleaner.deleteTripCoverImage(trip.getCoverImage().getStorageKey());
            tripCoverImageRepository.delete(trip.getCoverImage());
            trip.removeCoverImage();
        }
    }

    @Transactional(readOnly = true)
    public List<TripDocumentResponse> getTripDocuments(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);
        return tripDocumentRepository.findByTripId(tripId).stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    @Transactional
    public TripDocumentResponse uploadTripDocument(Long userId, Long tripId, String clientId, MultipartFile file) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        // 멱등성 체크: soft delete된 문서는 파일을 유지하므로 재업로드 없이 복구.
        return tripDocumentRepository.findByClientIdAndTripIdIncludingDeleted(clientId, tripId)
                .map(this::restoreOrKeepTripDocument)
                .orElseGet(() -> uploadNewTripDocument(trip, userId, tripId, clientId, file));
    }

    private TripDocumentResponse restoreOrKeepTripDocument(TripDocument document) {
        if (document.isDeleted()) {
            document.restore();
        }
        return toDocumentResponse(document);
    }

    private TripDocumentResponse uploadNewTripDocument(Trip trip, Long userId, Long tripId, String clientId, MultipartFile file) {
        StorageUploadResult uploadResult = storageService.uploadTripDocument(userId, tripId, file);

        TripDocument document = TripDocument.builder()
                .trip(trip)
                .clientId(clientId)
                .originalFileName(uploadResult.getOriginalFilename())
                .storageKey(uploadResult.getStorageKey())
                .fileSize(uploadResult.getFileSize())
                .contentType(DocumentMimeType.fromMimeType(uploadResult.getMimeType()))
                .build();

        TripDocument savedDocument = tripDocumentRepository.save(document);
        return toDocumentResponse(savedDocument);
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

        // soft delete: 복구 가능하도록 스토리지 파일은 삭제하지 않고 유지 (여행 삭제 시 일괄 정리)
        document.delete();
    }

    @Transactional
    public void deleteTrip(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(userId, trip);

        if (trip.getCoverImage() != null) {
            storageCleaner.deleteTripCoverImage(trip.getCoverImage().getStorageKey());
        }

        // soft delete된 문서의 파일도 함께 정리 (개별 삭제 시점엔 파일을 유지했으므로)
        for (String storageKey : tripDocumentRepository.findStorageKeysByTripId(tripId)) {
            storageCleaner.deleteTripDocument(storageKey);
        }

        // soft delete된 자식은 @SQLRestriction에 가려져 cascade로 정리되지 않으므로 직접 제거.
        // 사진은 native bulk delete라 @PreRemove가 동작하지 않으므로 스토리지 파일을 먼저 정리한다.
        deletePhotoStorage(photoRepository.findSoftDeletedStorageKeysByTripId(tripId));
        photoRepository.deleteSoftDeletedByTripId(tripId);
        tripDocumentRepository.deleteSoftDeletedByTripId(tripId);
        budgetRepository.deleteSoftDeletedByTripId(tripId);
        tripMemoRepository.deleteAllByTripId(tripId);

        // soft delete된 발자취는 cascade 대상에서 빠지는데, DB의 ON DELETE CASCADE가 발자취 행을
        // 지울 때 자식(photo/footprint_location)엔 cascade가 없어 FK 위반이 난다.
        // 자식 → 발자취 순으로 직접 정리한다. (사진은 스토리지 파일도 함께 제거)
        deletePhotoStorage(photoRepository.findStorageKeysByDeletedFootprintTripId(tripId));
        photoRepository.deleteByDeletedFootprintTripId(tripId);
        footprintRepository.deleteLocationsByDeletedFootprintTripId(tripId);
        footprintRepository.deleteSoftDeletedByTripId(tripId);

        tripRepository.delete(trip);
    }

    // native bulk delete로 제거될 사진들의 스토리지 파일을 정리 (@PreRemove가 동작하지 않으므로 수동 처리)
    // 행이 지워지기 전에 키를 모아 두고, 실제 삭제는 커밋 후에 이뤄진다.
    private void deletePhotoStorage(List<String> storageKeys) {
        for (String storageKey : storageKeys) {
            storageCleaner.deleteFootprintPhoto(storageKey);
        }
    }

    private TripResponse toTripResponse(Trip trip) {
        return TripResponse.from(trip, coverSignedUrl(trip));
    }

    private String coverSignedUrl(Trip trip) {
        TripCoverImage cover = trip.getCoverImage();
        return cover != null ? storageService.getImageSignedUrl(cover.getStorageKey()) : null;
    }

    private TripDocumentResponse toDocumentResponse(TripDocument document) {
        return TripDocumentResponse.from(document, storageService.getDocumentSignedUrl(document.getStorageKey()));
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

        // 멱등성 체크: 동일한 clientId 행이 있으면 재사용. soft delete된 경우 복구 후 업데이트.
        return tripMemoRepository.findByClientIdAndTripIdIncludingDeleted(request.getClientId(), tripId)
                .map(memo -> restoreOrKeepTripMemo(memo, request))
                .orElseGet(() -> createNewTripMemo(trip, request));
    }

    private TripMemoResponse restoreOrKeepTripMemo(TripMemo memo, TripMemoCreateRequest request) {
        if (memo.isDeleted()) {
            memo.restore();
            memo.update(request.getTitle(), request.getContent());
        }
        return TripMemoResponse.from(memo);
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

        memo.delete();
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
