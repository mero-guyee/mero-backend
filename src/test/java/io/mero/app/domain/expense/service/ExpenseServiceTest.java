package io.mero.app.domain.expense.service;

import io.mero.app.domain.budget.entity.Budget;
import io.mero.app.domain.budget.repository.BudgetRepository;
import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.footprint.repository.FootprintRepository;
import io.mero.app.domain.expense.dto.ExpenseCreateRequest;
import io.mero.app.domain.expense.dto.ExpenseListResponse;
import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.domain.expense.dto.ExpenseUpdateRequest;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.entity.ExpenseCategory;
import io.mero.app.domain.expense.repository.ExpenseCategoryRepository;
import io.mero.app.domain.expense.repository.ExpenseRepository;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.domain.user.entity.User;
import io.mero.app.global.enums.Currency;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private FootprintRepository footprintRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private ExpenseCategoryRepository expenseCategoryRepository;

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
                "test-client-id-1",
                1L,
                null,  // footprintId
                new BigDecimal("100"),
                Currency.USD,
                1L,  // categoryId
                "스타벅스",
                LocalDate.of(2024, 12, 8),
                "도쿄"
        );

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("password")
                .nickname("테스트")
                .build();

        Trip trip = Trip.builder()
                .id(1L)
                .user(user)
                .title("일본 여행")
                .build();

        ExpenseCategory category = ExpenseCategory.builder()
                .user(user)
                .name("식비")
                .icon("🍔")
                .color("#FF5733")
                .isDefault(false)
                .displayOrder(1)
                .build();

        Expense expense = Expense.builder()
                .id(1L)
                .trip(trip)
                .footprint(null)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .category(category)
                .description(request.getDescription())
                .date(request.getDate())
                .location(request.getLocation())
                .build();

        given(tripRepository.findById(1L)).willReturn(Optional.of(trip));
        given(expenseRepository.findByClientIdAndTripId("test-client-id-1", 1L)).willReturn(Optional.empty());
        given(expenseCategoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(expenseRepository.save(any(Expense.class))).willReturn(expense);

        // when
        ExpenseResponse response = expenseService.createExpense(userId, request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("100"));
        assertThat(response.getCurrency()).isEqualTo(Currency.USD);
        assertThat(response.getFootprintId()).isNull();
    }

    @Test
    @DisplayName("지출 생성 성공 - Footprint 연결")
    void createExpense_Success_WithFootprint() {
        // given
        Long userId = 1L;
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                "test-client-id-2",
                1L,
                1L,  // footprintId
                new BigDecimal("100"),
                Currency.USD,
                1L,  // categoryId
                "일기에 기록한 스타벅스",
                LocalDate.of(2024, 12, 8),
                "도쿄"
        );

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(1L)
                .user(user)
                .build();

        Footprint footprint = Footprint.builder()
                .id(1L)
                .trip(trip)
                .date(LocalDate.of(2024, 12, 8))
                .build();

        ExpenseCategory category = ExpenseCategory.builder()
                .user(user)
                .name("식비")
                .icon("🍔")
                .color("#FF5733")
                .isDefault(false)
                .displayOrder(1)
                .build();

        Expense expense = Expense.builder()
                .id(1L)
                .trip(trip)
                .footprint(footprint)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .category(category)
                .description(request.getDescription())
                .date(request.getDate())
                .location(request.getLocation())
                .build();

        given(tripRepository.findById(1L)).willReturn(Optional.of(trip));
        given(expenseRepository.findByClientIdAndTripId("test-client-id-2", 1L)).willReturn(Optional.empty());
        given(footprintRepository.findById(1L)).willReturn(Optional.of(footprint));
        given(expenseCategoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(expenseRepository.save(any(Expense.class))).willReturn(expense);

        // when
        ExpenseResponse response = expenseService.createExpense(userId, request);

        // then
        assertThat(response.getFootprintId()).isEqualTo(1L);
        verify(footprintRepository).findById(1L);
    }

    @Test
    @DisplayName("지출 생성 실패 - 다른 여행의 Footprint 연결 시도")
    void createExpense_Fail_FootprintFromDifferentTrip() {
        // given
        Long userId = 1L;
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                "test-client-id-3",
                1L,
                2L,  // 다른 여행의 footprintId
                new BigDecimal("100"),
                Currency.USD,
                1L,  // categoryId
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

        Footprint footprint = Footprint.builder()
                .id(2L)
                .trip(trip2)  // 다른 여행!
                .build();

        given(tripRepository.findById(1L)).willReturn(Optional.of(trip1));
        given(expenseRepository.findByClientIdAndTripId("test-client-id-3", 1L)).willReturn(Optional.empty());
        given(footprintRepository.findById(2L)).willReturn(Optional.of(footprint));
        given(messageUtil.getMessage("error.footprint.tripMismatch")).willReturn("같은 여행의 Footprint만 연결할 수 있습니다");

        // when & then
        assertThatThrownBy(() -> expenseService.createExpense(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("같은 여행의 Footprint만 연결할 수 있습니다");
    }

    @Test
    @DisplayName("지출 생성 실패 - 권한 없음")
    void createExpense_Fail_NotOwner() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;

        ExpenseCreateRequest request = new ExpenseCreateRequest(
                "test-client-id-4", 1L, null, new BigDecimal("100"), Currency.USD,
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
                .build();

        Footprint footprint = Footprint.builder()
                .id(1L)
                .trip(trip)
                .build();

        ExpenseCategory category = ExpenseCategory.builder()
                .user(user)
                .name("식비")
                .icon("🍔")
                .color("#FF5733")
                .isDefault(false)
                .displayOrder(1)
                .build();

        List<Expense> expenses = List.of(
                Expense.builder()
                        .id(1L)
                        .trip(trip)
                        .footprint(footprint)
                        .amount(new BigDecimal("100"))
                        .currency(Currency.USD)
                        .category(category)
                        .date(LocalDate.now())
                        .build(),
                Expense.builder()
                        .id(2L)
                        .trip(trip)
                        .footprint(null)
                        .amount(new BigDecimal("5000"))
                        .currency(Currency.JPY)
                        .category(category)
                        .date(LocalDate.now())
                        .build()
        );

        List<Budget> budgets = List.of();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(expenseRepository.findByTripOrderByDateDesc(trip)).willReturn(expenses);
        given(budgetRepository.findByTripOrderByCreatedAtDesc(trip)).willReturn(budgets);

        // when
        ExpenseListResponse response = expenseService.getExpensesByTrip(userId, tripId);

        // then
        assertThat(response.getExpenses()).hasSize(2);
        assertThat(response.getExpenses().get(0).getFootprintId()).isEqualTo(1L);
        assertThat(response.getExpenses().get(1).getFootprintId()).isNull();
        assertThat(response.getCurrencyUsages()).isEmpty();
    }

    @Test
    @DisplayName("여행의 지출 목록 조회 성공 - 화폐별 사용량 포함")
    void getExpensesByTrip_Success_WithCurrencyUsages() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .build();

        ExpenseCategory category = ExpenseCategory.builder()
                .user(user)
                .name("식비")
                .icon("🍔")
                .color("#FF5733")
                .isDefault(false)
                .displayOrder(1)
                .build();

        List<Expense> expenses = List.of(
                Expense.builder()
                        .id(1L)
                        .trip(trip)
                        .amount(new BigDecimal("50"))
                        .currency(Currency.USD)
                        .category(category)
                        .date(LocalDate.now())
                        .build(),
                Expense.builder()
                        .id(2L)
                        .trip(trip)
                        .amount(new BigDecimal("30"))
                        .currency(Currency.USD)
                        .category(category)
                        .date(LocalDate.now())
                        .build(),
                Expense.builder()
                        .id(3L)
                        .trip(trip)
                        .amount(new BigDecimal("100000"))
                        .currency(Currency.KRW)
                        .category(category)
                        .date(LocalDate.now())
                        .build()
        );

        List<Budget> budgets = List.of(
                Budget.builder()
                        .id(1L)
                        .trip(trip)
                        .amount(new BigDecimal("200"))
                        .currency(Currency.USD)
                        .build(),
                Budget.builder()
                        .id(2L)
                        .trip(trip)
                        .amount(new BigDecimal("500000"))
                        .currency(Currency.KRW)
                        .build()
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(expenseRepository.findByTripOrderByDateDesc(trip)).willReturn(expenses);
        given(budgetRepository.findByTripOrderByCreatedAtDesc(trip)).willReturn(budgets);

        // when
        ExpenseListResponse response = expenseService.getExpensesByTrip(userId, tripId);

        // then
        assertThat(response.getExpenses()).hasSize(3);
        assertThat(response.getCurrencyUsages()).hasSize(2);

        // USD: 50 + 30 = 80, budget = 200, usage = 40%
        assertThat(response.getCurrencyUsages().get(0).getCurrency()).isEqualTo(Currency.USD);
        assertThat(response.getCurrencyUsages().get(0).getTotalSpent()).isEqualByComparingTo(new BigDecimal("80"));
        assertThat(response.getCurrencyUsages().get(0).getBudget()).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(response.getCurrencyUsages().get(0).getUsagePercent()).isEqualByComparingTo(new BigDecimal("40.00"));

        // KRW: 100000, budget = 500000, usage = 20%
        assertThat(response.getCurrencyUsages().get(1).getCurrency()).isEqualTo(Currency.KRW);
        assertThat(response.getCurrencyUsages().get(1).getTotalSpent()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(response.getCurrencyUsages().get(1).getBudget()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(response.getCurrencyUsages().get(1).getUsagePercent()).isEqualByComparingTo(new BigDecimal("20.00"));
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
                .build();
        Footprint footprint = Footprint.builder()
                .id(1L)
                .trip(trip)
                .build();
        ExpenseCategory category = ExpenseCategory.builder()
                .user(user)
                .name("식비")
                .icon("🍔")
                .color("#FF5733")
                .isDefault(false)
                .displayOrder(1)
                .build();
        Expense expense = Expense.builder()
                .id(expenseId)
                .trip(trip)
                .footprint(footprint)
                .amount(new BigDecimal("100"))
                .currency(Currency.USD)
                .category(category)
                .date(LocalDate.now())
                .build();

        given(expenseRepository.findById(expenseId)).willReturn(Optional.of(expense));

        // when
        ExpenseResponse response = expenseService.getExpense(userId, expenseId);

        // then
        assertThat(response.getId()).isEqualTo(expenseId);
        assertThat(response.getFootprintId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("지출 수정 성공 - Footprint 연결")
    void updateExpense_Success_LinkFootprint() {
        // given
        Long userId = 1L;
        Long expenseId = 1L;

        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                1L,  // footprintId 연결
                new BigDecimal("150"),
                Currency.USD,
                1L,  // categoryId
                "수정된 설명",
                LocalDate.now(),
                "도쿄"
        );

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(1L)
                .user(user)
                .build();
        Footprint footprint = Footprint.builder()
                .id(1L)
                .trip(trip)
                .build();
        ExpenseCategory category = ExpenseCategory.builder()
                .user(user)
                .name("식비")
                .icon("🍔")
                .color("#FF5733")
                .isDefault(false)
                .displayOrder(1)
                .build();
        Expense expense = Expense.builder()
                .id(expenseId)
                .trip(trip)
                .footprint(null)  // 처음엔 없음
                .amount(new BigDecimal("100"))
                .currency(Currency.USD)
                .category(category)
                .date(LocalDate.now())
                .build();

        given(expenseRepository.findById(expenseId)).willReturn(Optional.of(expense));
        given(footprintRepository.findById(1L)).willReturn(Optional.of(footprint));
        given(expenseCategoryRepository.findById(1L)).willReturn(Optional.of(category));

        // when
        ExpenseResponse response = expenseService.updateExpense(userId, expenseId, request);

        // then
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("150"));
        assertThat(response.getDescription()).isEqualTo("수정된 설명");
        verify(footprintRepository).findById(1L);
    }

    @Test
    @DisplayName("지출 수정 성공 - Footprint 연결 해제")
    void updateExpense_Success_UnlinkFootprint() {
        // given
        Long userId = 1L;
        Long expenseId = 1L;

        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                null,  // footprintId null (연결 해제)
                new BigDecimal("150"),
                Currency.USD,
                1L,  // categoryId
                "수정된 설명",
                LocalDate.now(),
                "도쿄"
        );

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(1L)
                .user(user)
                .build();
        Footprint footprint = Footprint.builder()
                .id(1L)
                .trip(trip)
                .build();
        ExpenseCategory category = ExpenseCategory.builder()
                .user(user)
                .name("식비")
                .icon("🍔")
                .color("#FF5733")
                .isDefault(false)
                .displayOrder(1)
                .build();
        Expense expense = Expense.builder()
                .id(expenseId)
                .trip(trip)
                .footprint(footprint)  // 처음엔 있음
                .amount(new BigDecimal("100"))
                .currency(Currency.USD)
                .category(category)
                .date(LocalDate.now())
                .build();

        given(expenseRepository.findById(expenseId)).willReturn(Optional.of(expense));
        given(expenseCategoryRepository.findById(1L)).willReturn(Optional.of(category));

        // when
        ExpenseResponse response = expenseService.updateExpense(userId, expenseId, request);

        // then
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("150"));
        // footprint 연결 해제 확인은 expense.getFootprint()가 null인지 확인
    }

    @Test
    @DisplayName("지출 수정 실패 - 다른 여행의 Footprint 연결 시도")
    void updateExpense_Fail_FootprintFromDifferentTrip() {
        // given
        Long userId = 1L;
        Long expenseId = 1L;

        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                2L,  // 다른 여행의 footprintId
                new BigDecimal("150"),
                Currency.USD,
                1L,  // categoryId
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
        Footprint footprint = Footprint.builder()
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
        given(footprintRepository.findById(2L)).willReturn(Optional.of(footprint));
        given(messageUtil.getMessage("error.footprint.tripMismatch")).willReturn("같은 여행의 Footprint만 연결할 수 있습니다");

        // when & then
        assertThatThrownBy(() -> expenseService.updateExpense(userId, expenseId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("같은 여행의 Footprint만 연결할 수 있습니다");
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
        assertThat(expense.isDeleted()).isTrue();
        verify(expenseRepository, never()).delete(any());
    }

    @Test
    @DisplayName("지출 생성 실패 - 여행을 찾을 수 없음")
    void createExpense_Fail_TripNotFound() {
        // given
        Long userId = 1L;
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                "test-client-id-5", 999L, null, new BigDecimal("100"), Currency.USD,
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
    @DisplayName("지출 생성 실패 - Footprint를 찾을 수 없음")
    void createExpense_Fail_FootprintNotFound() {
        // given
        Long userId = 1L;
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                "test-client-id-6", 1L, 999L, new BigDecimal("100"), Currency.USD,
                null, null, LocalDate.now(), null
        );

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(1L)
                .user(user)
                .build();

        given(tripRepository.findById(1L)).willReturn(Optional.of(trip));
        given(footprintRepository.findById(999L)).willReturn(Optional.empty());
        given(messageUtil.getMessage("error.footprint.notFound"))
                .willReturn("Footprint를 찾을 수 없습니다");

        // when & then
        assertThatThrownBy(() -> expenseService.createExpense(userId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Footprint를 찾을 수 없습니다");
    }
}