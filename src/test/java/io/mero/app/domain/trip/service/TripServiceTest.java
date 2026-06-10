package io.mero.app.domain.trip.service;

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
import io.mero.app.domain.trip.entity.TripDocument;
import io.mero.app.domain.trip.entity.TripMemo;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripCoverImageRepository tripCoverImageRepository;

    @Mock
    private TripDocumentRepository tripDocumentRepository;

    @Mock
    private TripMemoRepository tripMemoRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private MessageUtil messageUtil;

    @InjectMocks
    private TripService tripService;

    @Test
    @DisplayName("여행 등록 성공 - 이미지 없음")
    void 여행_등록_성공_이미지_없음() {
        // given
        Long userId = 1L;
        String clientId = "test-client-id-1";
        TripCreateRequest request = new TripCreateRequest(
                clientId,
                "남미 여행",
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 5, 15),
                List.of("브라질", "아르헨티나", "페루")
        );

        User user = createUser(userId);
        Trip trip = createTrip(1L, user, request.getTitle(), request.getStartDate(), request.getEndDate());

        given(tripRepository.findByClientIdAndUserId(clientId, userId)).willReturn(Optional.empty());
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tripRepository.save(any(Trip.class))).willReturn(trip);

        // when
        TripResponse response = tripService.createTrip(userId, request, null);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("남미 여행");
        assertThat(response.getImageUrl()).isNull();

        verify(tripRepository).findByClientIdAndUserId(clientId, userId);
        verify(userRepository).findById(userId);
        verify(tripRepository).save(any(Trip.class));
        verify(storageService, never()).uploadTripCoverImage(any(), any());
    }

    @Test
    @DisplayName("여행 등록 성공 - 이미지 포함")
    void 여행_등록_성공_이미지_포함() {
        // given
        Long userId = 1L;
        String clientId = "test-client-id-2";
        TripCreateRequest request = new TripCreateRequest(
                clientId,
                "남미 여행",
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 5, 15),
                List.of("브라질", "아르헨티나", "페루")
        );

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        StorageUploadResult uploadResult = new StorageUploadResult(
                "users/1/trips/cover/uuid_test.jpg",
                "test.jpg",
                18L,
                "image/jpeg"
        );

        User user = createUser(userId);
        Trip trip = createTrip(1L, user, request.getTitle(), request.getStartDate(), request.getEndDate());

        given(tripRepository.findByClientIdAndUserId(clientId, userId)).willReturn(Optional.empty());
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tripRepository.save(any(Trip.class))).willReturn(trip);
        given(storageService.uploadTripCoverImage(eq(userId), any(MultipartFile.class))).willReturn(uploadResult);
        given(tripCoverImageRepository.save(any(TripCoverImage.class))).willAnswer(invocation -> {
            TripCoverImage coverImage = invocation.getArgument(0);
            trip.setCoverImage(coverImage);
            return coverImage;
        });

        given(storageService.getImageSignedUrl("users/1/trips/cover/uuid_test.jpg"))
                .willReturn("https://signed.test/cover.jpg");

        // when
        TripResponse response = tripService.createTrip(userId, request, image);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("남미 여행");
        assertThat(response.getImageUrl()).isEqualTo("https://signed.test/cover.jpg");

        verify(storageService).uploadTripCoverImage(eq(userId), any(MultipartFile.class));
        verify(tripCoverImageRepository).save(any(TripCoverImage.class));
    }

    @Test
    @DisplayName("여행 목록 조회 성공")
    void 여행_목록_조회_성공() {
        // given
        Long userId = 1L;
        User user = createUser(userId);

        List<Trip> trips = List.of(
                createTrip(1L, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15)),
                createTrip(2L, user, "일본 여행", LocalDate.of(2026, 8, 11), LocalDate.of(2026, 10, 15))
        );

        given(tripRepository.findByUserIdOrderByStartDateDesc(userId)).willReturn(trips);

        // when
        List<TripResponse> responses = tripService.getTrips(userId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTitle()).isEqualTo("남미 여행");
        assertThat(responses.get(1).getTitle()).isEqualTo("일본 여행");

        verify(tripRepository).findByUserIdOrderByStartDateDesc(userId);
    }

    @Test
    @DisplayName("여행 상세 조회 성공")
    void 여행_상세_조회_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15));

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        TripDetailResponse response = tripService.getTrip(userId, tripId);

        // then
        assertThat(response.getId()).isEqualTo(tripId);
        assertThat(response.getTitle()).isEqualTo("남미 여행");

        verify(tripRepository).findById(tripId);
    }

    @Test
    @DisplayName("여행 상세 조회 실패 - 권한 없음")
    void 여행_상세_조회_실패_권한_없음() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long tripId = 1L;
        User otherUser = createUser(otherUserId);
        Trip trip = createTrip(tripId, otherUser, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15));

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(messageUtil.getMessage("error.forbidden")).willReturn("접근 권한이 없습니다");

        // when & then
        assertThatThrownBy(() -> tripService.getTrip(userId, tripId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한이 없습니다");

        verify(tripRepository).findById(tripId);
    }

    @Test
    @DisplayName("여행 수정 성공")
    void 여행_수정_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15));

        TripUpdateRequest request = new TripUpdateRequest(
                "남미 여행 수정",
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 5, 16),
                List.of("아르헨티나", "페루", "볼리비아")
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        TripResponse response = tripService.updateTrip(userId, tripId, request);

        // then
        assertThat(response.getTitle()).isEqualTo("남미 여행 수정");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 16));

        verify(tripRepository).findById(tripId);
    }

    @Test
    @DisplayName("여행 이미지 업로드 성공 - 기존 이미지 없음")
    void 여행_이미지_업로드_성공_기존_이미지_없음() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15));

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        StorageUploadResult uploadResult = new StorageUploadResult(
                "users/1/trips/cover/uuid_test.jpg",
                "test.jpg",
                18L,
                "image/jpeg"
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(storageService.uploadTripCoverImage(eq(userId), any(MultipartFile.class))).willReturn(uploadResult);
        given(tripCoverImageRepository.save(any(TripCoverImage.class))).willAnswer(invocation -> {
            TripCoverImage coverImage = invocation.getArgument(0);
            trip.setCoverImage(coverImage);
            return coverImage;
        });

        given(storageService.getImageSignedUrl("users/1/trips/cover/uuid_test.jpg"))
                .willReturn("https://signed.test/cover.jpg");

        // when
        TripResponse response = tripService.updateTripImage(userId, tripId, image);

        // then
        assertThat(response.getImageUrl()).isEqualTo("https://signed.test/cover.jpg");

        verify(storageService).uploadTripCoverImage(eq(userId), any(MultipartFile.class));
        verify(storageService, never()).deleteTripCoverImage(any());
        verify(tripCoverImageRepository).save(any(TripCoverImage.class));
    }

    @Test
    @DisplayName("여행 이미지 업로드 성공 - 기존 이미지 교체")
    void 여행_이미지_업로드_성공_기존_이미지_교체() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15));

        // 기존 커버 이미지 설정
        TripCoverImage existingCoverImage = TripCoverImage.builder()
                .trip(trip)
                .s3Key("users/1/trips/cover/old_image.jpg")
                .originalFilename("old_image.jpg")
                .fileSize(100L)
                .mimeType(ImageMimeType.JPEG)
                .build();
        trip.setCoverImage(existingCoverImage);

        MockMultipartFile newImage = new MockMultipartFile(
                "image",
                "new_test.jpg",
                "image/jpeg",
                "new test image content".getBytes()
        );

        StorageUploadResult uploadResult = new StorageUploadResult(
                "users/1/trips/cover/uuid_new_test.jpg",
                "new_test.jpg",
                22L,
                "image/jpeg"
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(storageService.uploadTripCoverImage(eq(userId), any(MultipartFile.class))).willReturn(uploadResult);
        given(tripCoverImageRepository.save(any(TripCoverImage.class))).willAnswer(invocation -> {
            TripCoverImage coverImage = invocation.getArgument(0);
            trip.setCoverImage(coverImage);
            return coverImage;
        });

        given(storageService.getImageSignedUrl("users/1/trips/cover/uuid_new_test.jpg"))
                .willReturn("https://signed.test/new_cover.jpg");

        // when
        TripResponse response = tripService.updateTripImage(userId, tripId, newImage);

        // then
        assertThat(response.getImageUrl()).isEqualTo("https://signed.test/new_cover.jpg");

        verify(storageService).deleteTripCoverImage("users/1/trips/cover/old_image.jpg");
        verify(tripCoverImageRepository).delete(existingCoverImage);
        verify(storageService).uploadTripCoverImage(eq(userId), any(MultipartFile.class));
        verify(tripCoverImageRepository).save(any(TripCoverImage.class));
    }

    @Test
    @DisplayName("여행 이미지 삭제 성공")
    void 여행_이미지_삭제_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15));

        // 커버 이미지 설정
        TripCoverImage coverImage = TripCoverImage.builder()
                .trip(trip)
                .s3Key("users/1/trips/cover/test.jpg")
                .originalFilename("test.jpg")
                .fileSize(100L)
                .mimeType(ImageMimeType.JPEG)
                .build();
        trip.setCoverImage(coverImage);

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        tripService.deleteTripImage(userId, tripId);

        // then
        verify(storageService).deleteTripCoverImage("users/1/trips/cover/test.jpg");
        verify(tripCoverImageRepository).delete(coverImage);
        assertThat(trip.getCoverImage()).isNull();
    }

    @Test
    @DisplayName("여행 이미지 삭제 - 이미지가 없는 경우")
    void 여행_이미지_삭제_이미지_없음() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15));

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        tripService.deleteTripImage(userId, tripId);

        // then
        verify(storageService, never()).deleteTripCoverImage(any());
        verify(tripCoverImageRepository, never()).delete(any());
    }

    @Test
    @DisplayName("여행 삭제 성공 - 커버 이미지 있음")
    void 여행_삭제_성공_이미지_있음() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15));

        // 커버 이미지 설정
        TripCoverImage coverImage = TripCoverImage.builder()
                .trip(trip)
                .s3Key("users/1/trips/cover/test.jpg")
                .originalFilename("test.jpg")
                .fileSize(100L)
                .mimeType(ImageMimeType.JPEG)
                .build();
        trip.setCoverImage(coverImage);

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        tripService.deleteTrip(userId, tripId);

        // then
        verify(tripRepository).findById(tripId);
        verify(storageService).deleteTripCoverImage("users/1/trips/cover/test.jpg");
        verify(tripRepository).delete(trip);
    }

    @Test
    @DisplayName("여행 삭제 성공 - 커버 이미지 없음")
    void 여행_삭제_성공_이미지_없음() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15));

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        tripService.deleteTrip(userId, tripId);

        // then
        verify(tripRepository).findById(tripId);
        verify(storageService, never()).deleteTripCoverImage(any());
        verify(tripRepository).delete(trip);
    }

    // ===== 문서 목록 조회 =====

    @Test
    @DisplayName("여행 문서 목록 조회 성공")
    void 여행_문서_목록_조회_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "도쿄 여행", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 7));

        TripDocument document = TripDocument.builder()
                .trip(trip)
                .originalFileName("ticket.pdf")
                .storedFileName("users/1/trips/1/documents/ticket.pdf")
                .fileSize(11L)
                .contentType(DocumentMimeType.PDF)
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(tripDocumentRepository.findByTripId(tripId)).willReturn(List.of(document));

        // when
        List<TripDocumentResponse> responses = tripService.getTripDocuments(userId, tripId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getFileName()).isEqualTo("ticket.pdf");

        verify(tripDocumentRepository).findByTripId(tripId);
    }

    // ===== 문서 업로드 =====

    @Test
    @DisplayName("여행 문서 업로드 성공")
    void 여행_문서_업로드_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "도쿄 여행", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 7));

        MockMultipartFile file = new MockMultipartFile(
                "file", "ticket.pdf", "application/pdf", "pdf content".getBytes()
        );

        StorageUploadResult uploadResult = new StorageUploadResult(
                "users/1/trips/1/documents/ticket.pdf",
                "ticket.pdf",
                11L,
                "application/pdf"
        );

        TripDocument document = TripDocument.builder()
                .trip(trip)
                .originalFileName("ticket.pdf")
                .storedFileName("users/1/trips/1/documents/ticket.pdf")
                .fileSize(11L)
                .contentType(DocumentMimeType.PDF)
                .build();

        String clientId = "doc-client-id-1";

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(tripDocumentRepository.findByClientIdAndTripId(clientId, tripId)).willReturn(Optional.empty());
        given(storageService.uploadTripDocument(eq(userId), eq(tripId), any(MultipartFile.class))).willReturn(uploadResult);
        given(tripDocumentRepository.save(any(TripDocument.class))).willReturn(document);
        given(storageService.getDocumentSignedUrl("users/1/trips/1/documents/ticket.pdf"))
                .willReturn("https://signed.example.com/ticket.pdf");

        // when
        TripDocumentResponse response = tripService.uploadTripDocument(userId, tripId, clientId, file);

        // then
        assertThat(response.getFileName()).isEqualTo("ticket.pdf");
        assertThat(response.getFileUrl()).isEqualTo("https://signed.example.com/ticket.pdf");

        verify(storageService).uploadTripDocument(eq(userId), eq(tripId), any(MultipartFile.class));
        verify(tripDocumentRepository).save(any(TripDocument.class));
    }

    @Test
    @DisplayName("여행 문서 삭제 성공")
    void 여행_문서_삭제_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long documentId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "도쿄 여행", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 7));

        TripDocument document = TripDocument.builder()
                .trip(trip)
                .originalFileName("ticket.pdf")
                .storedFileName("users/1/trips/1/documents/ticket.pdf")
                .fileSize(11L)
                .contentType(DocumentMimeType.PDF)
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(tripDocumentRepository.findById(documentId)).willReturn(Optional.of(document));

        // when
        tripService.deleteTripDocument(userId, tripId, documentId);

        // then
        verify(storageService).deleteTripDocument("users/1/trips/1/documents/ticket.pdf");
        verify(tripDocumentRepository).delete(document);
    }

    @Test
    @DisplayName("여행 문서 삭제 실패 - 문서 없음")
    void 여행_문서_삭제_실패_문서_없음() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long documentId = 999L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "도쿄 여행", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 7));

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(tripDocumentRepository.findById(documentId)).willReturn(Optional.empty());
        given(messageUtil.getMessage("error.document.notFound")).willReturn("문서를 찾을 수 없습니다");

        // when & then
        assertThatThrownBy(() -> tripService.deleteTripDocument(userId, tripId, documentId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("문서를 찾을 수 없습니다");
    }

    // ===== 메모 생성 =====

    @Test
    @DisplayName("여행 메모 생성 성공")
    void 여행_메모_생성_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "도쿄 여행", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 7));

        TripMemoCreateRequest request = new TripMemoCreateRequest("memo-client-id-1", "메모 제목", "메모 내용");

        TripMemo memo = TripMemo.builder()
                .trip(trip)
                .clientId("memo-client-id-1")
                .title("메모 제목")
                .content("메모 내용")
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(tripMemoRepository.findByClientIdAndTripId(request.getClientId(), tripId)).willReturn(Optional.empty());
        given(tripMemoRepository.save(any(TripMemo.class))).willReturn(memo);

        // when
        TripMemoResponse response = tripService.createTripMemo(userId, tripId, request);

        // then
        assertThat(response.getTitle()).isEqualTo("메모 제목");
        assertThat(response.getContent()).isEqualTo("메모 내용");

        verify(tripMemoRepository).save(any(TripMemo.class));
    }

    @Test
    @DisplayName("여행 메모 생성 성공 - 중복 clientId (멱등성)")
    void 여행_메모_생성_성공_중복_clientId() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "도쿄 여행", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 7));

        TripMemoCreateRequest request = new TripMemoCreateRequest("memo-client-id-1", "메모 제목", "메모 내용");

        TripMemo existing = TripMemo.builder()
                .trip(trip)
                .clientId("memo-client-id-1")
                .title("메모 제목")
                .content("메모 내용")
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(tripMemoRepository.findByClientIdAndTripId(request.getClientId(), tripId)).willReturn(Optional.of(existing));

        // when
        TripMemoResponse response = tripService.createTripMemo(userId, tripId, request);

        // then
        assertThat(response.getTitle()).isEqualTo("메모 제목");
        verify(tripMemoRepository, never()).save(any(TripMemo.class));
    }

    // ===== 메모 목록 조회 =====

    @Test
    @DisplayName("여행 메모 목록 조회 성공")
    void 여행_메모_목록_조회_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "도쿄 여행", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 7));

        List<TripMemo> memos = List.of(
                TripMemo.builder().trip(trip).clientId("c1").title("메모1").content("내용1").build(),
                TripMemo.builder().trip(trip).clientId("c2").title("메모2").content("내용2").build()
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(tripMemoRepository.findByTripIdOrderByCreatedAtDesc(tripId)).willReturn(memos);

        // when
        List<TripMemoResponse> responses = tripService.getTripMemos(userId, tripId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTitle()).isEqualTo("메모1");
    }

    // ===== 메모 상세 조회 =====

    @Test
    @DisplayName("여행 메모 상세 조회 성공")
    void 여행_메모_상세_조회_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long memoId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "도쿄 여행", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 7));

        TripMemo memo = TripMemo.builder()
                .trip(trip)
                .clientId("memo-client-id-1")
                .title("메모 제목")
                .content("메모 내용")
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(tripMemoRepository.findById(memoId)).willReturn(Optional.of(memo));

        // when
        TripMemoResponse response = tripService.getTripMemo(userId, tripId, memoId);

        // then
        assertThat(response.getTitle()).isEqualTo("메모 제목");
    }

    // ===== 메모 수정 =====

    @Test
    @DisplayName("여행 메모 수정 성공")
    void 여행_메모_수정_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long memoId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "도쿄 여행", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 7));

        TripMemo memo = TripMemo.builder()
                .trip(trip)
                .clientId("memo-client-id-1")
                .title("원본 제목")
                .content("원본 내용")
                .build();

        TripMemoUpdateRequest request = new TripMemoUpdateRequest("수정된 제목", "수정된 내용");

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(tripMemoRepository.findById(memoId)).willReturn(Optional.of(memo));

        // when
        TripMemoResponse response = tripService.updateTripMemo(userId, tripId, memoId, request);

        // then
        assertThat(response.getTitle()).isEqualTo("수정된 제목");
        assertThat(response.getContent()).isEqualTo("수정된 내용");
    }

    // ===== 메모 삭제 =====

    @Test
    @DisplayName("여행 메모 삭제 성공")
    void 여행_메모_삭제_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long memoId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "도쿄 여행", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 7));

        TripMemo memo = TripMemo.builder()
                .trip(trip)
                .clientId("memo-client-id-1")
                .title("메모 제목")
                .content("메모 내용")
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(tripMemoRepository.findById(memoId)).willReturn(Optional.of(memo));

        // when
        tripService.deleteTripMemo(userId, tripId, memoId);

        // then
        verify(tripMemoRepository).delete(memo);
    }

    @Test
    @DisplayName("여행 메모 삭제 실패 - 메모 없음")
    void 여행_메모_삭제_실패_메모_없음() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long memoId = 999L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "도쿄 여행", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 7));

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(tripMemoRepository.findById(memoId)).willReturn(Optional.empty());
        given(messageUtil.getMessage("error.memo.notFound")).willReturn("메모를 찾을 수 없습니다");

        // when & then
        assertThatThrownBy(() -> tripService.deleteTripMemo(userId, tripId, memoId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("메모를 찾을 수 없습니다");
    }

    private User createUser(Long userId) {
        return User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("password")
                .nickname("테스트유저")
                .build();
    }

    private Trip createTrip(Long tripId, User user, String title, LocalDate startDate, LocalDate endDate) {
        return Trip.builder()
                .id(tripId)
                .user(user)
                .title(title)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
