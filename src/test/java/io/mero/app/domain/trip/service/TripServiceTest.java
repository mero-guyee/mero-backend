package io.mero.app.domain.trip.service;

import io.mero.app.domain.file.repository.FileRepository;
import io.mero.app.domain.trip.dto.TripCreateRequest;
import io.mero.app.domain.trip.dto.TripDetailResponse;
import io.mero.app.domain.trip.dto.TripResponse;
import io.mero.app.domain.trip.dto.TripUpdateRequest;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.UserRepository;
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
    private FileRepository fileRepository;

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
        TripCreateRequest request = new TripCreateRequest(
                "남미 여행",
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 5, 15),
                List.of("브라질", "아르헨티나", "페루")
        );

        User user = createUser(userId);
        Trip trip = createTrip(1L, user, request.getTitle(), request.getStartDate(), request.getEndDate(), null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tripRepository.save(any(Trip.class))).willReturn(trip);

        // when
        TripResponse response = tripService.createTrip(userId, request, null);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("남미 여행");
        assertThat(response.getImageUrl()).isNull();

        verify(userRepository).findById(userId);
        verify(tripRepository).save(any(Trip.class));
        verify(s3Service, never()).uploadTripImage(any(), any());
    }

    @Test
    @DisplayName("여행 등록 성공 - 이미지 포함")
    void 여행_등록_성공_이미지_포함() {
        // given
        Long userId = 1L;
        TripCreateRequest request = new TripCreateRequest(
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

        String uploadedImageUrl = "https://s3.amazonaws.com/bucket/users/1/trips/images/uuid_test.jpg";

        User user = createUser(userId);
        Trip trip = createTrip(1L, user, request.getTitle(), request.getStartDate(), request.getEndDate(), uploadedImageUrl);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(s3Service.uploadTripImage(eq(userId), any(MultipartFile.class))).willReturn(uploadedImageUrl);
        given(tripRepository.save(any(Trip.class))).willReturn(trip);

        // when
        TripResponse response = tripService.createTrip(userId, request, image);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("남미 여행");
        assertThat(response.getImageUrl()).isEqualTo(uploadedImageUrl);

        verify(s3Service).uploadTripImage(eq(userId), any(MultipartFile.class));
    }

    @Test
    @DisplayName("여행 목록 조회 성공")
    void 여행_목록_조회_성공() {
        // given
        Long userId = 1L;
        User user = createUser(userId);

        List<Trip> trips = List.of(
                createTrip(1L, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15), null),
                createTrip(2L, user, "일본 여행", LocalDate.of(2026, 8, 11), LocalDate.of(2026, 10, 15), null)
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
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15), null);

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(fileRepository.findByTripId(tripId)).willReturn(Collections.emptyList());

        // when
        TripDetailResponse response = tripService.getTrip(userId, tripId);

        // then
        assertThat(response.getId()).isEqualTo(tripId);
        assertThat(response.getTitle()).isEqualTo("남미 여행");

        verify(tripRepository).findById(tripId);
        verify(fileRepository).findByTripId(tripId);
    }

    @Test
    @DisplayName("여행 상세 조회 실패 - 권한 없음")
    void 여행_상세_조회_실패_권한_없음() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long tripId = 1L;
        User otherUser = createUser(otherUserId);
        Trip trip = createTrip(tripId, otherUser, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15), null);

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
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15), null);

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
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15), null);

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        String uploadedImageUrl = "https://s3.amazonaws.com/bucket/users/1/trips/images/uuid_test.jpg";

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(s3Service.uploadTripImage(eq(userId), any(MultipartFile.class))).willReturn(uploadedImageUrl);

        // when
        TripResponse response = tripService.updateTripImage(userId, tripId, image);

        // then
        assertThat(response.getImageUrl()).isEqualTo(uploadedImageUrl);

        verify(s3Service).uploadTripImage(eq(userId), any(MultipartFile.class));
        verify(s3Service, never()).deleteTripImage(any());
    }

    @Test
    @DisplayName("여행 이미지 업로드 성공 - 기존 이미지 교체")
    void 여행_이미지_업로드_성공_기존_이미지_교체() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        String existingImageUrl = "https://s3.amazonaws.com/bucket/users/1/trips/images/old_image.jpg";

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15), existingImageUrl);

        MockMultipartFile newImage = new MockMultipartFile(
                "image",
                "new_test.jpg",
                "image/jpeg",
                "new test image content".getBytes()
        );

        String newImageUrl = "https://s3.amazonaws.com/bucket/users/1/trips/images/uuid_new_test.jpg";

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(s3Service.uploadTripImage(eq(userId), any(MultipartFile.class))).willReturn(newImageUrl);

        // when
        TripResponse response = tripService.updateTripImage(userId, tripId, newImage);

        // then
        assertThat(response.getImageUrl()).isEqualTo(newImageUrl);

        verify(s3Service).deleteTripImage(existingImageUrl);
        verify(s3Service).uploadTripImage(eq(userId), any(MultipartFile.class));
    }

    @Test
    @DisplayName("여행 이미지 삭제 성공")
    void 여행_이미지_삭제_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        String existingImageUrl = "https://s3.amazonaws.com/bucket/users/1/trips/images/test.jpg";

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15), existingImageUrl);

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        tripService.deleteTripImage(userId, tripId);

        // then
        verify(s3Service).deleteTripImage(existingImageUrl);
        assertThat(trip.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("여행 이미지 삭제 - 이미지가 없는 경우")
    void 여행_이미지_삭제_이미지_없음() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15), null);

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        tripService.deleteTripImage(userId, tripId);

        // then
        verify(s3Service, never()).deleteTripImage(any());
    }

    @Test
    @DisplayName("여행 삭제 성공")
    void 여행_삭제_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = createUser(userId);
        Trip trip = createTrip(tripId, user, "남미 여행", LocalDate.of(2026, 3, 11), LocalDate.of(2026, 5, 15), null);

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        tripService.deleteTrip(userId, tripId);

        // then
        verify(tripRepository).findById(tripId);
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

    private Trip createTrip(Long tripId, User user, String title, LocalDate startDate, LocalDate endDate, String imageUrl) {
        return Trip.builder()
                .id(tripId)
                .user(user)
                .title(title)
                .startDate(startDate)
                .endDate(endDate)
                .imageUrl(imageUrl)
                .build();
    }
}
