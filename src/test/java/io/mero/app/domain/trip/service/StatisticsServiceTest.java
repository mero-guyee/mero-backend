package io.mero.app.domain.trip.service;

import io.mero.app.domain.exchange.service.ExchangeRateService;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.repository.ExpenseRepository;
import io.mero.app.domain.trip.dto.TripStatisticsResponse;
import io.mero.app.domain.trip.dto.TripStatisticsResponse.CategoryExpense;
import io.mero.app.domain.trip.dto.TripStatisticsResponse.CurrencyExpense;
import io.mero.app.domain.trip.dto.TripStatisticsResponse.DailyExpense;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.domain.user.entity.User;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.enums.Timezone;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private MessageUtil messageUtil;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    @DisplayName("여행 통계 조회 성공 - 기본 통계")
    void 여행_통계_조회_성공_기본_통계() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

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
                .title("일본 여행")
                .startDate(LocalDate.of(2024, 12, 1))
                .endDate(LocalDate.of(2024, 12, 7))
                .defaultCurrency(Currency.KRW)
                .build();

        List<Expense> expenses = List.of(
                Expense.builder()
                        .id(1L)
                        .trip(trip)
                        .amount(new BigDecimal("100"))
                        .currency(Currency.USD)
                        .category("식비")
                        .description("스타벅스")
                        .date(LocalDate.of(2024, 12, 1))
                        .location("도쿄")
                        .build(),
                Expense.builder()
                        .id(2L)
                        .trip(trip)
                        .amount(new BigDecimal("150"))
                        .currency(Currency.USD)
                        .category("숙박")
                        .description("호텔")
                        .date(LocalDate.of(2024, 12, 1))
                        .location("도쿄")
                        .build(),
                Expense.builder()
                        .id(3L)
                        .trip(trip)
                        .amount(new BigDecimal("5000"))
                        .currency(Currency.JPY)
                        .category("식비")
                        .description("라멘")
                        .date(LocalDate.of(2024, 12, 2))
                        .location("도쿄")
                        .build()
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(expenseRepository.findByTripOrderByDateDesc(trip)).willReturn(expenses);

        // USD → KRW: 1472
        given(exchangeRateService.getRate(eq(Currency.USD), eq(Currency.KRW), any()))
                .willReturn(new BigDecimal("1472"));

        // JPY → KRW: 9.49
        given(exchangeRateService.getRate(eq(Currency.JPY), eq(Currency.KRW), any()))
                .willReturn(new BigDecimal("9.49"));

        // when
        TripStatisticsResponse response = statisticsService.getTripStatistics(userId, tripId);

        // then
        assertThat(response.getTripId()).isEqualTo(tripId);
        assertThat(response.getTripTitle()).isEqualTo("일본 여행");
        assertThat(response.getCurrency()).isEqualTo(Currency.KRW);
        assertThat(response.getExpenseCount()).isEqualTo(3);

        // 총 지출: (100 + 150) * 1472 + 5000 * 9.49 = 368000 + 47450 = 415450
        assertThat(response.getTotalExpense()).isEqualByComparingTo(new BigDecimal("415450.00"));

        // 예산 정보
//        assertThat(response.getBudget()).isEqualByComparingTo(new BigDecimal("2000000"));
//        assertThat(response.getBudgetRemaining()).isEqualByComparingTo(new BigDecimal("1584550.00"));
//        assertThat(response.getBudgetUsagePercentage()).isEqualTo(20.77);
    }

    @Test
    @DisplayName("여행 통계 조회 성공 - 카테고리별 분석")
    void 여행_통계_조회_성공_카테고리별_분석() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .title("일본 여행")
                .defaultCurrency(Currency.KRW)
                .build();

        List<Expense> expenses = List.of(
                Expense.builder()
                        .trip(trip)
                        .amount(new BigDecimal("100"))
                        .currency(Currency.USD)
                        .category("식비")
                        .date(LocalDate.of(2024, 12, 1))
                        .build(),
                Expense.builder()
                        .trip(trip)
                        .amount(new BigDecimal("50"))
                        .currency(Currency.USD)
                        .category("식비")
                        .date(LocalDate.of(2024, 12, 2))
                        .build(),
                Expense.builder()
                        .trip(trip)
                        .amount(new BigDecimal("200"))
                        .currency(Currency.USD)
                        .category("숙박")
                        .date(LocalDate.of(2024, 12, 1))
                        .build()
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(expenseRepository.findByTripOrderByDateDesc(trip)).willReturn(expenses);
        given(exchangeRateService.getRate(any(), any(), any()))
                .willReturn(new BigDecimal("1472"));

        // when
        TripStatisticsResponse response = statisticsService.getTripStatistics(userId, tripId);

        // then
        List<CategoryExpense> categories = response.getExpensesByCategory();

        assertThat(categories).hasSize(2);

        // 숙박이 가장 많음 (200 * 1472 = 294400)
        assertThat(categories.get(0).getCategory()).isEqualTo("숙박");
        assertThat(categories.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("294400.00"));
        assertThat(categories.get(0).getCount()).isEqualTo(1);

        // 식비가 두 번째 (150 * 1472 = 220800)
        assertThat(categories.get(1).getCategory()).isEqualTo("식비");
        assertThat(categories.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("220800.00"));
        assertThat(categories.get(1).getCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("여행 통계 조회 성공 - 일별 분석")
    void 여행_통계_조회_성공_일별_분석() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .title("일본 여행")
                .defaultCurrency(Currency.KRW)
                .build();

        List<Expense> expenses = List.of(
                Expense.builder()
                        .trip(trip)
                        .amount(new BigDecimal("100"))
                        .currency(Currency.USD)
                        .date(LocalDate.of(2024, 12, 1))
                        .build(),
                Expense.builder()
                        .trip(trip)
                        .amount(new BigDecimal("50"))
                        .currency(Currency.USD)
                        .date(LocalDate.of(2024, 12, 1))
                        .build(),
                Expense.builder()
                        .trip(trip)
                        .amount(new BigDecimal("200"))
                        .currency(Currency.USD)
                        .date(LocalDate.of(2024, 12, 2))
                        .build()
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(expenseRepository.findByTripOrderByDateDesc(trip)).willReturn(expenses);
        given(exchangeRateService.getRate(any(), any(), any()))
                .willReturn(new BigDecimal("1472"));

        // when
        TripStatisticsResponse response = statisticsService.getTripStatistics(userId, tripId);

        // then
        List<DailyExpense> dailyExpenses = response.getExpensesByDate();

        assertThat(dailyExpenses).hasSize(2);

        // 날짜 순 정렬 확인
        assertThat(dailyExpenses.get(0).getDate()).isEqualTo(LocalDate.of(2024, 12, 1));
        assertThat(dailyExpenses.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("220800.00"));
        assertThat(dailyExpenses.get(0).getCount()).isEqualTo(2);

        assertThat(dailyExpenses.get(1).getDate()).isEqualTo(LocalDate.of(2024, 12, 2));
        assertThat(dailyExpenses.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("294400.00"));
        assertThat(dailyExpenses.get(1).getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("여행 통계 조회 성공 - 통화별 분석")
    void 여행_통계_조회_성공_통화별_분석() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .title("일본 여행")
                .defaultCurrency(Currency.KRW)
                .build();

        List<Expense> expenses = List.of(
                Expense.builder()
                        .trip(trip)
                        .amount(new BigDecimal("100"))
                        .currency(Currency.USD)
                        .date(LocalDate.of(2024, 12, 1))
                        .build(),
                Expense.builder()
                        .trip(trip)
                        .amount(new BigDecimal("50"))
                        .currency(Currency.USD)
                        .date(LocalDate.of(2024, 12, 1))
                        .build(),
                Expense.builder()
                        .trip(trip)
                        .amount(new BigDecimal("10000"))
                        .currency(Currency.JPY)
                        .date(LocalDate.of(2024, 12, 2))
                        .build()
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(expenseRepository.findByTripOrderByDateDesc(trip)).willReturn(expenses);

        given(exchangeRateService.getRate(eq(Currency.USD), eq(Currency.KRW), any()))
                .willReturn(new BigDecimal("1472"));

        given(exchangeRateService.getRate(eq(Currency.JPY), eq(Currency.KRW), any()))
                .willReturn(new BigDecimal("9.49"));

        // when
        TripStatisticsResponse response = statisticsService.getTripStatistics(userId, tripId);

        // then
        List<CurrencyExpense> currencyExpenses = response.getExpensesByCurrency();

        assertThat(currencyExpenses).hasSize(2);

        // USD가 가장 많음 (변환 금액 기준)
        assertThat(currencyExpenses.get(0).getCurrency()).isEqualTo(Currency.USD);
        assertThat(currencyExpenses.get(0).getOriginalAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(currencyExpenses.get(0).getConvertedAmount()).isEqualByComparingTo(new BigDecimal("220800.00"));
        assertThat(currencyExpenses.get(0).getCount()).isEqualTo(2);

        // JPY가 두 번째
        assertThat(currencyExpenses.get(1).getCurrency()).isEqualTo(Currency.JPY);
        assertThat(currencyExpenses.get(1).getOriginalAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(currencyExpenses.get(1).getConvertedAmount()).isEqualByComparingTo(new BigDecimal("94900.00"));
        assertThat(currencyExpenses.get(1).getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("여행 통계 조회 성공 - 경비 없는 경우")
    void 여행_통계_조회_성공_경비_없는_경우() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .title("계획 중인 여행")
                .defaultCurrency(Currency.KRW)
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(expenseRepository.findByTripOrderByDateDesc(trip)).willReturn(List.of());

        // when
        TripStatisticsResponse response = statisticsService.getTripStatistics(userId, tripId);

        // then
        assertThat(response.getTotalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getExpenseCount()).isEqualTo(0);
//        assertThat(response.getBudgetRemaining()).isEqualByComparingTo(new BigDecimal("2000000"));
//        assertThat(response.getBudgetUsagePercentage()).isEqualTo(0.0);
        assertThat(response.getExpensesByCategory()).isEmpty();
        assertThat(response.getExpensesByDate()).isEmpty();
        assertThat(response.getExpensesByCurrency()).isEmpty();
    }

    @Test
    @DisplayName("여행 통계 조회 성공 - 카테고리 없는 경비는 '기타'로 분류")
    void 여행_통계_조회_성공_카테고리_없는_경비는_기타로_분류() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .title("일본 여행")
                .defaultCurrency(Currency.KRW)
                .build();

        List<Expense> expenses = List.of(
                Expense.builder()
                        .trip(trip)
                        .amount(new BigDecimal("100"))
                        .currency(Currency.USD)
                        .category(null)  // 카테고리 없음
                        .date(LocalDate.of(2024, 12, 1))
                        .build(),
                Expense.builder()
                        .trip(trip)
                        .amount(new BigDecimal("50"))
                        .currency(Currency.USD)
                        .category("식비")
                        .date(LocalDate.of(2024, 12, 1))
                        .build()
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(expenseRepository.findByTripOrderByDateDesc(trip)).willReturn(expenses);
        given(exchangeRateService.getRate(any(), any(), any()))
                .willReturn(new BigDecimal("1472"));

        // when
        TripStatisticsResponse response = statisticsService.getTripStatistics(userId, tripId);

        // then
        List<CategoryExpense> categories = response.getExpensesByCategory();

        assertThat(categories).hasSize(2);
        assertThat(categories)
                .extracting(CategoryExpense::getCategory)
                .contains("기타", "식비");
    }

    @Test
    @DisplayName("여행 통계 조회 실패 - 여행을 찾을 수 없음")
    void 여행_통계_조회_실패_여행을_찾을_수_없음() {
        // given
        Long userId = 1L;
        Long tripId = 999L;

        given(tripRepository.findById(tripId)).willReturn(Optional.empty());
        given(messageUtil.getMessage("error.trip.notFound"))
                .willReturn("여행을 찾을 수 없습니다");

        // when & then
        assertThatThrownBy(() -> statisticsService.getTripStatistics(userId, tripId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("여행을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("여행 통계 조회 실패 - 권한 없음")
    void 여행_통계_조회_실패_권한_없음() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long tripId = 1L;

        User otherUser = User.builder().id(otherUserId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(otherUser)
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(messageUtil.getMessage("error.forbidden"))
                .willReturn("접근 권한이 없습니다");

        // when & then
        assertThatThrownBy(() -> statisticsService.getTripStatistics(userId, tripId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한이 없습니다");
    }
}
