package io.mero.app.domain.expense.service;

import io.mero.app.domain.diary.entity.Diary;
import io.mero.app.domain.diary.repository.DiaryRepository;
import io.mero.app.domain.expense.dto.ExpenseCreateRequest;
import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.domain.expense.dto.ExpenseUpdateRequest;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.repository.ExpenseRepository;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.domain.user.entity.User;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.enums.Timezone;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.NotFoundException;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private MessageUtil messageUtil;

    @InjectMocks
    private ExpenseService expenseService;

    // ===== 기존 테스트 =====

    @Test
    @DisplayName("지출 생성 성공")
    void createExpense_Success_OfficialRate() {
        // given
        Long userId = 1L;
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                1L,
                null,  // diaryId
                new BigDecimal("100"),
                Currency.USD,
                "식비",
                "스타벅스",
                LocalDate.of(2024, 12, 8),
                "도쿄"
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
                .id(1L)
                .user(user)
                .title("일본 여행")
                .defaultCurrency(Currency.KRW)
                .build();

        Expense expense = Expense.builder()
                .id(1L)
                .trip(trip)
                .diary(null)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .category(request.getCategory())
                .description(request.getDescription())
                .date(request.getDate())
                .location(request.getLocation())
                .build();

        given(tripRepository.findById(1L)).willReturn(Optional.of(trip));
        given(expenseRepository.save(any(Expense.class))).willReturn(expense);

        // when
        ExpenseResponse response = expenseService.createExpense(userId, request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("100"));
        assertThat(response.getCurrency()).isEqualTo(Currency.USD);
        assertThat(response.getDiaryId()).isNull();
    }

    @Test
    @DisplayName("지출 생성 성공 - Diary 연결")
    void createExpense_Success_WithDiary() {
        // given
        Long userId = 1L;
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                1L,
                1L,  // diaryId
                new BigDecimal("100"),
                Currency.USD,
                "식비",
                "일기에 기록한 스타벅스",
                LocalDate.of(2024, 12, 8),
                "도쿄"
        );

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(1L)
                .user(user)
                .defaultCurrency(Currency.KRW)
                .build();

        Diary diary = Diary.builder()
                .id(1L)
                .trip(trip)
                .date(LocalDate.of(2024, 12, 8))
                .build();

        Expense expense = Expense.builder()
                .id(1L)
                .trip(trip)
                .diary(diary)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .category(request.getCategory())
                .description(request.getDescription())
                .date(request.getDate())
                .location(request.getLocation())
                .build();

        given(tripRepository.findById(1L)).willReturn(Optional.of(trip));
        given(diaryRepository.findById(1L)).willReturn(Optional.of(diary));
        given(expenseRepository.save(any(Expense.class))).willReturn(expense);

        // when
        ExpenseResponse response = expenseService.createExpense(userId, request);

        // then
        assertThat(response.getDiaryId()).isEqualTo(1L);
        verify(diaryRepository).findById(1L);
    }

    @Test
    @DisplayName("지출 생성 실패 - 다른 여행의 일기 연결 시도")
    void createExpense_Fail_DiaryFromDifferentTrip() {
        // given
        Long userId = 1L;
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                1L,
                2L,  // 다른 여행의 diaryId
                new BigDecimal("100"),
                Currency.USD,
                "식비",
                "스타벅스",
                LocalDate.of(2024, 12, 8),
                "도쿄"
        );

        User user = User.builder().id(userId).build();
        Trip trip1 = Trip.builder()
                .id(1L)
                .user(user)
                .build();

        Trip trip2 = Trip.builder()
                .id(2L)
                .user(user)
                .build();

        Diary diary = Diary.builder()
                .id(2L)
                .trip(trip2)  // 다른 여행!
                .build();

        given(tripRepository.findById(1L)).willReturn(Optional.of(trip1));
        given(diaryRepository.findById(2L)).willReturn(Optional.of(diary));
        given(messageUtil.getMessage("error.diary.tripMismatch")).willReturn("같은 여행의 일기만 연결할 수 있습니다");

        // when & then
        assertThatThrownBy(() -> expenseService.createExpense(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("같은 여행의 일기만 연결할 수 있습니다");
    }

    @Test
    @DisplayName("지출 생성 실패 - 권한 없음")
    void createExpense_Fail_NotOwner() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;

        ExpenseCreateRequest request = new ExpenseCreateRequest(
                1L, null, new BigDecimal("100"), Currency.USD,
                null, null, LocalDate.now(), null
        );

        User otherUser = User.builder().id(otherUserId).build();
        Trip trip = Trip.builder()
                .id(1L)
                .user(otherUser)
                .build();

        given(tripRepository.findById(1L)).willReturn(Optional.of(trip));
        given(messageUtil.getMessage("error.forbidden")).willReturn("접근 권한이 없습니다");

        // when & then
        assertThatThrownBy(() -> expenseService.createExpense(userId, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한이 없습니다");
    }

    @Test
    @DisplayName("여행의 지출 목록 조회 성공")
    void getExpensesByTrip_Success() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .defaultCurrency(Currency.KRW)
                .build();

        Diary diary = Diary.builder()
                .id(1L)
                .trip(trip)
                .build();

        List<Expense> expenses = List.of(
                Expense.builder()
                        .id(1L)
                        .trip(trip)
                        .diary(diary)
                        .amount(new BigDecimal("100"))
                        .currency(Currency.USD)
                        .date(LocalDate.now())
                        .build(),
                Expense.builder()
                        .id(2L)
                        .trip(trip)
                        .diary(null)
                        .amount(new BigDecimal("5000"))
                        .currency(Currency.JPY)
                        .date(LocalDate.now())
                        .build()
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(expenseRepository.findByTripOrderByDateDesc(trip)).willReturn(expenses);

        // when
        List<ExpenseResponse> responses = expenseService.getExpensesByTrip(userId, tripId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getDiaryId()).isEqualTo(1L);
        assertThat(responses.get(1).getDiaryId()).isNull();
    }

    @Test
    @DisplayName("지출 상세 조회 성공")
    void getExpense_Success() {
        // given
        Long userId = 1L;
        Long expenseId = 1L;

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(1L)
                .user(user)
                .defaultCurrency(Currency.KRW)
                .build();
        Diary diary = Diary.builder()
                .id(1L)
                .trip(trip)
                .build();
        Expense expense = Expense.builder()
                .id(expenseId)
                .trip(trip)
                .diary(diary)
                .amount(new BigDecimal("100"))
                .currency(Currency.USD)
                .date(LocalDate.now())
                .build();

        given(expenseRepository.findById(expenseId)).willReturn(Optional.of(expense));

        // when
        ExpenseResponse response = expenseService.getExpense(userId, expenseId);

        // then
        assertThat(response.getId()).isEqualTo(expenseId);
        assertThat(response.getDiaryId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("지출 수정 성공 - Diary 연결")
    void updateExpense_Success_LinkDiary() {
        // given
        Long userId = 1L;
        Long expenseId = 1L;

        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                1L,  // diaryId 연결
                new BigDecimal("150"),
                Currency.USD,
                "식비",
                "수정된 설명",
                LocalDate.now(),
                "도쿄"
        );

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(1L)
                .user(user)
                .defaultCurrency(Currency.KRW)
                .build();
        Diary diary = Diary.builder()
                .id(1L)
                .trip(trip)
                .build();
        Expense expense = Expense.builder()
                .id(expenseId)
                .trip(trip)
                .diary(null)  // 처음엔 없음
                .amount(new BigDecimal("100"))
                .currency(Currency.USD)
                .date(LocalDate.now())
                .build();

        given(expenseRepository.findById(expenseId)).willReturn(Optional.of(expense));
        given(diaryRepository.findById(1L)).willReturn(Optional.of(diary));

        // when
        ExpenseResponse response = expenseService.updateExpense(userId, expenseId, request);

        // then
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("150"));
        assertThat(response.getDescription()).isEqualTo("수정된 설명");
        verify(diaryRepository).findById(1L);
    }

    @Test
    @DisplayName("지출 수정 성공 - Diary 연결 해제")
    void updateExpense_Success_UnlinkDiary() {
        // given
        Long userId = 1L;
        Long expenseId = 1L;

        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                null,  // diaryId null (연결 해제)
                new BigDecimal("150"),
                Currency.USD,
                "식비",
                "수정된 설명",
                LocalDate.now(),
                "도쿄"
        );

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(1L)
                .user(user)
                .defaultCurrency(Currency.KRW)
                .build();
        Diary diary = Diary.builder()
                .id(1L)
                .trip(trip)
                .build();
        Expense expense = Expense.builder()
                .id(expenseId)
                .trip(trip)
                .diary(diary)  // 처음엔 있음
                .amount(new BigDecimal("100"))
                .currency(Currency.USD)
                .date(LocalDate.now())
                .build();

        given(expenseRepository.findById(expenseId)).willReturn(Optional.of(expense));

        // when
        ExpenseResponse response = expenseService.updateExpense(userId, expenseId, request);

        // then
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("150"));
        // diary 연결 해제 확인은 expense.getDiary()가 null인지 확인
    }

    @Test
    @DisplayName("지출 수정 실패 - 다른 여행의 일기 연결 시도")
    void updateExpense_Fail_DiaryFromDifferentTrip() {
        // given
        Long userId = 1L;
        Long expenseId = 1L;

        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                2L,  // 다른 여행의 diaryId
                new BigDecimal("150"),
                Currency.USD,
                "식비",
                "수정된 설명",
                LocalDate.now(),
                "도쿄"
        );

        User user = User.builder().id(userId).build();
        Trip trip1 = Trip.builder()
                .id(1L)
                .user(user)
                .build();
        Trip trip2 = Trip.builder()
                .id(2L)
                .user(user)
                .build();
        Diary diary = Diary.builder()
                .id(2L)
                .trip(trip2)  // 다른 여행!
                .build();
        Expense expense = Expense.builder()
                .id(expenseId)
                .trip(trip1)
                .amount(new BigDecimal("100"))
                .currency(Currency.USD)
                .date(LocalDate.now())
                .build();

        given(expenseRepository.findById(expenseId)).willReturn(Optional.of(expense));
        given(diaryRepository.findById(2L)).willReturn(Optional.of(diary));
        given(messageUtil.getMessage("error.diary.tripMismatch")).willReturn("같은 여행의 일기만 연결할 수 있습니다");

        // when & then
        assertThatThrownBy(() -> expenseService.updateExpense(userId, expenseId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("같은 여행의 일기만 연결할 수 있습니다");
    }

    @Test
    @DisplayName("지출 삭제 성공")
    void deleteExpense_Success() {
        // given
        Long userId = 1L;
        Long expenseId = 1L;

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(1L)
                .user(user)
                .build();
        Expense expense = Expense.builder()
                .id(expenseId)
                .trip(trip)
                .build();

        given(expenseRepository.findById(expenseId)).willReturn(Optional.of(expense));

        // when
        expenseService.deleteExpense(userId, expenseId);

        // then
        verify(expenseRepository).delete(expense);
    }

    @Test
    @DisplayName("지출 생성 실패 - 여행을 찾을 수 없음")
    void createExpense_Fail_TripNotFound() {
        // given
        Long userId = 1L;
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                999L, null, new BigDecimal("100"), Currency.USD,
                null, null, LocalDate.now(), null
        );

        given(tripRepository.findById(999L)).willReturn(Optional.empty());
        given(messageUtil.getMessage("error.trip.notFound"))
                .willReturn("여행을 찾을 수 없습니다");

        // when & then
        assertThatThrownBy(() -> expenseService.createExpense(userId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("여행을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("지출 생성 실패 - 일기를 찾을 수 없음")
    void createExpense_Fail_DiaryNotFound() {
        // given
        Long userId = 1L;
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                1L, 999L, new BigDecimal("100"), Currency.USD,
                null, null, LocalDate.now(), null
        );

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(1L)
                .user(user)
                .build();

        given(tripRepository.findById(1L)).willReturn(Optional.of(trip));
        given(diaryRepository.findById(999L)).willReturn(Optional.empty());
        given(messageUtil.getMessage("error.diary.notFound"))
                .willReturn("일기를 찾을 수 없습니다");

        // when & then
        assertThatThrownBy(() -> expenseService.createExpense(userId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("일기를 찾을 수 없습니다");
    }
}