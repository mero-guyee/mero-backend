package io.mero.app.domain.diary.service;

import io.mero.app.domain.diary.dto.DiaryCreateRequest;
import io.mero.app.domain.diary.dto.DiaryResponse;
import io.mero.app.domain.diary.dto.DiaryUpdateRequest;
import io.mero.app.domain.diary.entity.Diary;
import io.mero.app.domain.diary.repository.DiaryRepository;
import io.mero.app.domain.exchange.service.ExchangeRateService;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.repository.ExpenseRepository;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.domain.user.entity.User;
import io.mero.app.global.embedded.Location;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.enums.Timezone;
import io.mero.app.global.exception.ForbiddenException;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {
    
    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    
    @Mock
    private MessageUtil messageUtil;
    
    @InjectMocks
    private DiaryService diaryService;
    
    @Test
    @DisplayName("일기 생성 성공")
    void 일기_생성_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        DiaryCreateRequest request = new DiaryCreateRequest(
                "12월 25일",
                "오늘은 크리스마스",
                LocalDate.of(2025, 12, 25),
                new Location(new BigDecimal("37.5665"), new BigDecimal("126.9780"), "서울특별시"),
                List.of("url1", "url2")
        );

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("password")
                .nickname("테스트")
                .defaultCurrency(Currency.KRW)
                .timezone(Timezone.ASIA_SEOUL)
                .build();

        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .title("서울 여행")
                .build();

        Diary diary = Diary.builder()
                .id(1L)
                .trip(trip)
                .content(request.getContent())
                .date(request.getDate())
                .location(request.getLocation())
                .photoUrls(request.getPhotoUrls())
                .build();

        given(tripRepository.findById(1L)).willReturn(Optional.of(trip));
        given(diaryRepository.save(any(Diary.class))).willReturn(diary);

        // when
        DiaryResponse response = diaryService.createDiary(userId, tripId, request);

        // then
        assertThat(response.getTripId()).isEqualTo(1L);
        assertThat(response.getContent()).isEqualTo("오늘은 크리스마스");
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2025, 12, 25));
        assertThat(response.getLocation().getLocationName()).isEqualTo("서울특별시");

        verify(tripRepository).findById(1L);
        verify(diaryRepository).save(any(Diary.class));

    }
    
    @Test
    @DisplayName("일기 생성 실패 - 여행 권한 없음")
    void 일기_생성_실패_여행_권한_없음() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long tripId = 1L;
        DiaryCreateRequest request = new DiaryCreateRequest(
                "12월 25일",
                "오늘은 크리스마스",
                LocalDate.of(2025, 12, 25),
                new Location(new BigDecimal("37.5665"), new BigDecimal("126.9780"), "서울특별시"),
                List.of("url1", "url2")
        );

        User otherUser = User.builder()
                .id(otherUserId)
                .email("test@example.com")
                .passwordHash("password")
                .nickname("테스트")
                .defaultCurrency(Currency.KRW)
                .timezone(Timezone.ASIA_SEOUL)
                .build();

        Trip trip = Trip.builder()
                .id(tripId)
                .user(otherUser)
                .title("서울 여행")
                .build();

        given(tripRepository.findById(1L)).willReturn(Optional.of(trip));
        given(messageUtil.getMessage("error.forbidden")).willReturn("접근 권한이 없습니다");

        // when & then
        assertThatThrownBy(() -> diaryService.createDiary(userId, tripId, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한이 없습니다");

    }
    
    @Test
    @DisplayName("일기 목록 조회 성공")
    void 일기_목록_조회_성공() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        User user = User.builder()
                .id(userId)
                .build();

        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .build();

        List<Diary> diaries = List.of(
                Diary.builder()
                        .id(1L)
                        .trip(trip)
                        .content("첫째 날 일기")
                        .build(),
                Diary.builder()
                        .id(2L)
                        .trip(trip)
                        .content("둘째 날 일기")
                        .build()
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(diaryRepository.findByTripIdOrderByDateDesc(tripId)).willReturn(diaries);

        // when
        List<DiaryResponse> responses = diaryService.getDiaries(userId, tripId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getContent()).isEqualTo("첫째 날 일기");
        assertThat(responses.get(1).getContent()).isEqualTo("둘째 날 일기");

    }
    
    @Test
    @DisplayName("일기 상세 조회 성공")
    void 일기_상세_조회_성공() {
        // given
        Long userId = 1L;
        Long tripId  = 1L;
        Long diaryId  = 1L;
        User user = User.builder()
                .id(userId)
                .build();

        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .build();

        Diary diary = Diary.builder()
                .id(diaryId)
                .trip(trip)
                .content("첫째 날 일기")
                .build();

        given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));

        // when
        DiaryResponse response = diaryService.getDiary(userId, tripId, diaryId);

        // then
        assertThat(response.getId()).isEqualTo(diaryId);
        assertThat(response.getContent()).isEqualTo("첫째 날 일기");

    }

    @Test
    @DisplayName("일기 상세 조회 성공 - 경비 포함")
    void 일기_상세_조회_성공_경비_포함() {
        // given
        Long userId = 1L;
        Long diaryId = 1L;
        Long tripId = 1L;

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .defaultCurrency(Currency.KRW)
                .build();
        Diary diary = Diary.builder()
                .id(diaryId)
                .trip(trip)
                .date(LocalDate.of(2024, 12, 8))
                .build();

        List<Expense> expenses = List.of(
                Expense.builder()
                        .id(1L)
                        .trip(trip)
                        .diary(diary)
                        .amount(new BigDecimal("100"))
                        .currency(Currency.USD)
                        .date(LocalDate.of(2024, 12, 8))
                        .build(),
                Expense.builder()
                        .id(2L)
                        .trip(trip)
                        .diary(diary)
                        .amount(new BigDecimal("5000"))
                        .currency(Currency.JPY)
                        .date(LocalDate.of(2024, 12, 8))
                        .build()
        );

        given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
        given(expenseRepository.findByDiary(diary)).willReturn(expenses);
        given(exchangeRateService.getRate(any(), any(), any()))
                .willReturn(new BigDecimal("1472"));

        // when
        DiaryResponse response = diaryService.getDiary(userId, tripId, diaryId);

        // then
        assertThat(response.getId()).isEqualTo(diaryId);
        assertThat(response.getExpenses()).hasSize(2);
        assertThat(response.getExpenses().get(0).getId()).isEqualTo(1L);
        assertThat(response.getExpenses().get(1).getId()).isEqualTo(2L);

        verify(expenseRepository).findByDiary(diary);
        verify(exchangeRateService, times(2)).getRate(any(), any(), any());
    }
    
    @Test
    @DisplayName("일기 수정 성공")
    void 일기_수정_성공() {
        // given
        Long userId = 1L;
        Long tripId  = 1L;
        Long diaryId  = 1L;
        User user = User.builder()
                .id(userId)
                .build();

        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .build();

        Diary diary = Diary.builder()
                .id(diaryId)
                .trip(trip)
                .content("첫째 날 일기")
                .build();

        DiaryUpdateRequest request = new DiaryUpdateRequest(
                "title",
                "첫째 날 일기 (수정)",
                LocalDate.of(2025, 12, 26),
                new Location(new BigDecimal("37.5665"), new BigDecimal("126.9780"), "서울특별시 - 수정본"),
                List.of("url3", "url4")
        );

        given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));

        // when
        DiaryResponse response = diaryService.updateDiary(userId, tripId, diaryId, request);

        // then
        assertThat(response.getContent()).isEqualTo("첫째 날 일기 (수정)");
    }
    
    @Test
    @DisplayName("일기 삭제 성공")
    void 일기_삭제_성공() {
        // given
        Long userId = 1L;
        Long tripId  = 1L;
        Long diaryId  = 1L;
        User user = User.builder()
                .id(userId)
                .build();

        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .build();

        Diary diary = Diary.builder()
                .id(diaryId)
                .trip(trip)
                .content("첫째 날 일기")
                .build();

        DiaryUpdateRequest request = new DiaryUpdateRequest(
                "title",
                "첫째 날 일기 (수정)",
                LocalDate.of(2025, 12, 26),
                new Location(new BigDecimal("37.5665"), new BigDecimal("126.9780"), "서울특별시 - 수정본"),
                List.of("url3", "url4")
        );

        given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));

        // when & then
        diaryService.deleteDiary(userId, tripId, diaryId);

        // then
        verify(diaryRepository).delete(diary);
    }

}