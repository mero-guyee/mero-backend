package io.mero.app.domain.trip.service;

import io.mero.app.domain.trip.repository.TripDocumentRepository;
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
import io.mero.app.global.enums.Currency;
import io.mero.app.global.enums.Timezone;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.service.S3Service;
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
    private S3Service s3Service;

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
        verify(s3Service, never()).uploadTripCoverImage(any(), any());
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

        S3UploadResult uploadResult = new S3UploadResult(
                "users/1/trips/cover/uuid_test.jpg",
                "https://s3.amazonaws.com/bucket/users/1/trips/cover/uuid_test.jpg",
                "test.jpg",
                18L,
                "image/jpeg"
        );

        User user = createUser(userId);
        Trip trip = createTrip(1L, user, request.getTitle(), request.getStartDate(), request.getEndDate());

        given(tripRepository.findByClientIdAndUserId(clientId, userId)).willReturn(Optional.empty());
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tripRepository.save(any(Trip.class))).willReturn(trip);
        given(s3Service.uploadTripCoverImage(eq(userId), any(MultipartFile.class))).willReturn(uploadResult);
        given(tripCoverImageRepository.save(any(TripCoverImage.class))).willAnswer(invocation -> {
            TripCoverImage coverImage = invocation.getArgument(0);
            trip.setCoverImage(coverImage);
            return coverImage;
        });

        // when
        TripResponse response = tripService.createTrip(userId, request, image);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("남미 여행");
        assertThat(response.getImageUrl()).isEqualTo(uploadResult.getS3Url());

        verify(s3Service).uploadTripCoverImage(eq(userId), any(MultipartFile.class));
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
        given(tripDocumentRepository.findByTripId(tripId)).willReturn(Collections.emptyList());

        // when
        TripDetailResponse response = tripService.getTrip(userId, tripId);

        // then
        assertThat(response.getId()).isEqualTo(tripId);
        assertThat(response.getTitle()).isEqualTo("남미 여행");

        verify(tripRepository).findById(tripId);
        verify(tripDocumentRepository).findByTripId(tripId);
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

        S3UploadResult uploadResult = new S3UploadResult(
                "users/1/trips/cover/uuid_test.jpg",
                "https://s3.amazonaws.com/bucket/users/1/trips/cover/uuid_test.jpg",
                "test.jpg",
                18L,
                "image/jpeg"
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(s3Service.uploadTripCoverImage(eq(userId), any(MultipartFile.class))).willReturn(uploadResult);
        given(tripCoverImageRepository.save(any(TripCoverImage.class))).willAnswer(invocation -> {
            TripCoverImage coverImage = invocation.getArgument(0);
            trip.setCoverImage(coverImage);
            return coverImage;
        });

        // when
        TripResponse response = tripService.updateTripImage(userId, tripId, image);

        // then
        assertThat(response.getImageUrl()).isEqualTo(uploadResult.getS3Url());

        verify(s3Service).uploadTripCoverImage(eq(userId), any(MultipartFile.class));
        verify(s3Service, never()).deleteTripCoverImage(any());
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
                .s3Url("https://s3.amazonaws.com/bucket/users/1/trips/cover/old_image.jpg")
                .originalFilename("old_image.jpg")
                .fileSize(100L)
                .mimeType("image/jpeg")
                .build();
        trip.setCoverImage(existingCoverImage);

        MockMultipartFile newImage = new MockMultipartFile(
                "image",
                "new_test.jpg",
                "image/jpeg",
                "new test image content".getBytes()
        );

        S3UploadResult uploadResult = new S3UploadResult(
                "users/1/trips/cover/uuid_new_test.jpg",
                "https://s3.amazonaws.com/bucket/users/1/trips/cover/uuid_new_test.jpg",
                "new_test.jpg",
                22L,
                "image/jpeg"
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(s3Service.uploadTripCoverImage(eq(userId), any(MultipartFile.class))).willReturn(uploadResult);
        given(tripCoverImageRepository.save(any(TripCoverImage.class))).willAnswer(invocation -> {
            TripCoverImage coverImage = invocation.getArgument(0);
            trip.setCoverImage(coverImage);
            return coverImage;
        });

        // when
        TripResponse response = tripService.updateTripImage(userId, tripId, newImage);

        // then
        assertThat(response.getImageUrl()).isEqualTo(uploadResult.getS3Url());

        verify(s3Service).deleteTripCoverImage("users/1/trips/cover/old_image.jpg");
        verify(tripCoverImageRepository).delete(existingCoverImage);
        verify(s3Service).uploadTripCoverImage(eq(userId), any(MultipartFile.class));
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
                .s3Url("https://s3.amazonaws.com/bucket/users/1/trips/cover/test.jpg")
                .originalFilename("test.jpg")
                .fileSize(100L)
                .mimeType("image/jpeg")
                .build();
        trip.setCoverImage(coverImage);

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        tripService.deleteTripImage(userId, tripId);

        // then
        verify(s3Service).deleteTripCoverImage("users/1/trips/cover/test.jpg");
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
        verify(s3Service, never()).deleteTripCoverImage(any());
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
                .s3Url("https://s3.amazonaws.com/bucket/users/1/trips/cover/test.jpg")
                .originalFilename("test.jpg")
                .fileSize(100L)
                .mimeType("image/jpeg")
                .build();
        trip.setCoverImage(coverImage);

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        tripService.deleteTrip(userId, tripId);

        // then
        verify(tripRepository).findById(tripId);
        verify(s3Service).deleteTripCoverImage("users/1/trips/cover/test.jpg");
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
        verify(s3Service, never()).deleteTripCoverImage(any());
        verify(tripRepository).delete(trip);
    }

    private User createUser(Long userId) {
        return User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("password")
                .nickname("테스트유저")
                .defaultCurrency(Currency.KRW)
                .timezone(Timezone.ASIA_SEOUL)
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
