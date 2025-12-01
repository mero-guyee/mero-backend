package io.mero.app.domain.trip.service;

import io.mero.app.domain.trip.dto.TripCreateRequest;
import io.mero.app.domain.trip.dto.TripResponse;
import io.mero.app.domain.trip.dto.TripUpdateRequest;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.UserRepository;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.enums.Timezone;
import io.mero.app.global.util.MessageUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private MessageUtil messageUtil;

    @InjectMocks
    private TripService tripService;

    @Test
    @DisplayName("여행 등록 성공")
    void 여행_등록_성공() {
        // given
        Long userId = 1L;
        TripCreateRequest request = new TripCreateRequest(
                "남미 여행",
                "남미 가자!",
                LocalDate.of(2026,03,11),
                LocalDate.of(2026,05,15),
                List.of("브라질", "아르헨티나", "페루"),
                new BigDecimal(10000000),
                Currency.ARS,
                Currency.KRW
        );

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("password")
                .nickname("테스트유저")
                .defaultCurrency(Currency.KRW)
                .timezone(Timezone.ASIA_SEOUL)
                .build();

        Trip trip = Trip.builder()
                .user(user)
                .id(1L)
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .countries(String.join(",", request.getCountries()))
                .totalBudget(request.getTotalBudget())
                .budgetCurrency(request.getBudgetCurrency())
                .defaultCurrency(request.getDefaultCurrency())
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tripRepository.save(any(Trip.class))).willReturn(trip);

        // when
        TripResponse response = tripService.createTrip(userId, request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("남미 여행");
        assertThat(response.getDescription()).isEqualTo("남미 가자!");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026,03,11));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026,05,15));

        verify(userRepository).findById(userId);
        verify(tripRepository).save(any(Trip.class));

    }
    
    @Test
    @DisplayName("여행 목록 조회 성공")
    void 여행_목록_조회_성공() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("password")
                .nickname("테스트유저")
                .build();

        List<Trip> trips = List.of(
                Trip.builder()
                        .id(1L).user(user).title("남미 여행").description("남미 가자!")
                        .startDate(LocalDate.of(2026, 3, 11))
                        .endDate(LocalDate.of(2026, 5, 15)).build(),
                Trip.builder()
                        .id(1L).user(user).title("일본 여행").description("일본 가자!")
                        .startDate(LocalDate.of(2026, 8, 11))
                        .endDate(LocalDate.of(2026, 10, 15)).build()
        );

        given(tripRepository.findByUserOrderByStartDateDesc(userId)).willReturn(trips);

        // when
        List<TripResponse> responses = tripService.getTrips(userId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTitle()).isEqualTo("남미 여행");
        assertThat(responses.get(1).getTitle()).isEqualTo("일본 여행");

        verify(tripRepository).findByUserOrderByStartDateDesc(userId);
    }
    
    @Test
    @DisplayName("여행 상세 조회 성공")
    void 여행_상세_조회_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("password")
                .nickname("테스트유저")
                .build();

        Trip trip = Trip.builder()
                .user(user)
                .id(tripId)
                .title("남미 여행")
                .description("남미 가자!")
                .startDate(LocalDate.of(2026, 03, 11))
                .endDate(LocalDate.of(2026, 05, 15))
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        TripResponse response = tripService.getTrip(userId, tripId);

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
        User otherUser = User.builder()
                .id(otherUserId)
                .email("test@example.com")
                .passwordHash("password")
                .nickname("테스트유저")
                .build();

        Trip trip = Trip.builder()
                .user(otherUser)
                .id(tripId)
                .title("남미 여행")
                .description("남미 가자!")
                .startDate(LocalDate.of(2026, 03, 11))
                .endDate(LocalDate.of(2026, 05, 15))
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(messageUtil.getMessage("error.forbidden")).willReturn("접근 권한이 없습니다");

        // when & then
        assertThatThrownBy(() -> tripService.getTrip(userId, tripId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("접근 권한이 없습니다");

        verify(tripRepository).findById(tripId);
    }
    
    @Test
    @DisplayName("여행 수정 성공")
    void 여행_수정_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("password")
                .nickname("테스트유저")
                .defaultCurrency(Currency.KRW)
                .timezone(Timezone.ASIA_SEOUL)
                .build();

        Trip trip = Trip.builder()
                .user(user)
                .id(tripId)
                .title("남미 여행")
                .description("남미 가자!")
                .startDate(LocalDate.of(2026, 03, 11))
                .endDate(LocalDate.of(2026, 05, 15))
                .build();

        TripUpdateRequest request = new TripUpdateRequest(
                "남미 여행 수정",
                "남미 진짜 가자!",
                LocalDate.of(2026,03,10),
                LocalDate.of(2026,05,16),
                List.of("아르헨티나", "페루", "볼리비아"),
                null,
                new BigDecimal(20000000),
                Currency.ARS,
                Currency.USD
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        TripResponse response = tripService.updateTrip(userId, tripId, request);

        // then
        assertThat(response.getTitle()).isEqualTo("남미 여행 수정");
        assertThat(response.getDescription()).isEqualTo("남미 진짜 가자!");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026,03,10));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026,05,16));
        assertThat(response.getDefaultCurrency()).isEqualTo(Currency.USD);

        verify(tripRepository).findById(tripId);
    }
    
    @Test
    @DisplayName("여행 삭제 성공")
    void 여행_삭제_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("password")
                .nickname("테스트유저")
                .defaultCurrency(Currency.KRW)
                .timezone(Timezone.ASIA_SEOUL)
                .build();

        Trip trip = Trip.builder()
                .user(user)
                .id(tripId)
                .title("남미 여행")
                .description("남미 가자!")
                .startDate(LocalDate.of(2026, 03, 11))
                .endDate(LocalDate.of(2026, 05, 15))
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        // when
        tripService.deleteTrip(userId, tripId);

        // then
        verify(tripRepository).findById(tripId);
        verify(tripRepository).delete(trip);
    }
}