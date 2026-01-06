package io.mero.app.domain.budget.service;

import io.mero.app.domain.budget.dto.BudgetCreateRequest;
import io.mero.app.domain.budget.dto.BudgetResponse;
import io.mero.app.domain.budget.dto.BudgetUpdateRequest;
import io.mero.app.domain.budget.entity.Budget;
import io.mero.app.domain.budget.repository.BudgetRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private MessageUtil messageUtil;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    @DisplayName("예산 생성 성공 - USD")
    void createBudget_Success_USD() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        BudgetCreateRequest request = new BudgetCreateRequest(
                new BigDecimal("4000"),
                Currency.USD
        );

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .build();

        Budget budget = Budget.builder()
                .id(1L)
                .trip(trip)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(budgetRepository.findByTripAndCurrency(trip, Currency.USD)).willReturn(Optional.empty());
        given(budgetRepository.save(any(Budget.class))).willReturn(budget);

        // when
        BudgetResponse response = budgetService.createBudget(userId, tripId, request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("4000"));
        assertThat(response.getCurrency()).isEqualTo(Currency.USD);
        assertThat(response.getTripId()).isEqualTo(tripId);
    }

    @Test
    @DisplayName("예산 생성 성공 - KRW")
    void createBudget_Success_KRW() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        BudgetCreateRequest request = new BudgetCreateRequest(
                new BigDecimal("1000000"),
                Currency.KRW
        );

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .build();

        Budget budget = Budget.builder()
                .id(2L)
                .trip(trip)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(budgetRepository.findByTripAndCurrency(trip, Currency.KRW)).willReturn(Optional.empty());
        given(budgetRepository.save(any(Budget.class))).willReturn(budget);

        // when
        BudgetResponse response = budgetService.createBudget(userId, tripId, request);

        // then
        assertThat(response.getCurrency()).isEqualTo(Currency.KRW);
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("1000000"));
    }

    @Test
    @DisplayName("예산 생성 실패 - 같은 통화 중복")
    void createBudget_Fail_DuplicateCurrency() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        BudgetCreateRequest request = new BudgetCreateRequest(
                new BigDecimal("4000"),
                Currency.USD
        );

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .build();

        Budget existingBudget = Budget.builder()
                .id(1L)
                .trip(trip)
                .amount(new BigDecimal("3000"))
                .currency(Currency.USD)
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(budgetRepository.findByTripAndCurrency(trip, Currency.USD))
                .willReturn(Optional.of(existingBudget));
        given(messageUtil.getMessage("error.budget.duplicateCurrency"))
                .willReturn("A budget for this currency already exists for this trip");

        // when & then
        assertThatThrownBy(() -> budgetService.createBudget(userId, tripId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A budget for this currency already exists for this trip");
    }

    @Test
    @DisplayName("예산 생성 실패 - 권한 없음")
    void createBudget_Fail_NotOwner() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long tripId = 1L;

        BudgetCreateRequest request = new BudgetCreateRequest(
                new BigDecimal("4000"),
                Currency.USD
        );

        User otherUser = User.builder().id(otherUserId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(otherUser)
                .build();

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(messageUtil.getMessage("error.forbidden")).willReturn("You don't have permission to access this resource");

        // when & then
        assertThatThrownBy(() -> budgetService.createBudget(userId, tripId, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You don't have permission to access this resource");
    }

    @Test
    @DisplayName("예산 생성 실패 - 여행을 찾을 수 없음")
    void createBudget_Fail_TripNotFound() {
        // given
        Long userId = 1L;
        Long tripId = 999L;

        BudgetCreateRequest request = new BudgetCreateRequest(
                new BigDecimal("4000"),
                Currency.USD
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.empty());
        given(messageUtil.getMessage("error.trip.notFound")).willReturn("Trip not found");

        // when & then
        assertThatThrownBy(() -> budgetService.createBudget(userId, tripId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Trip not found");
    }

    @Test
    @DisplayName("예산 목록 조회 성공")
    void getBudgetsByTrip_Success() {
        // given
        Long userId = 1L;
        Long tripId = 1L;

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .build();

        List<Budget> budgets = List.of(
                Budget.builder()
                        .id(1L)
                        .trip(trip)
                        .amount(new BigDecimal("4000"))
                        .currency(Currency.USD)
                        .build(),
                Budget.builder()
                        .id(2L)
                        .trip(trip)
                        .amount(new BigDecimal("1000000"))
                        .currency(Currency.KRW)
                        .build()
        );

        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));
        given(budgetRepository.findByTripOrderByCreatedAtDesc(trip)).willReturn(budgets);

        // when
        List<BudgetResponse> responses = budgetService.getBudgetsByTrip(userId, tripId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getCurrency()).isEqualTo(Currency.USD);
        assertThat(responses.get(1).getCurrency()).isEqualTo(Currency.KRW);
    }

    @Test
    @DisplayName("예산 수정 성공")
    void updateBudget_Success() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long budgetId = 1L;

        BudgetUpdateRequest request = new BudgetUpdateRequest(
                new BigDecimal("5000"),
                Currency.USD
        );

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .build();

        Budget budget = Budget.builder()
                .id(budgetId)
                .trip(trip)
                .amount(new BigDecimal("4000"))
                .currency(Currency.USD)
                .build();

        given(budgetRepository.findById(budgetId)).willReturn(Optional.of(budget));

        // when
        BudgetResponse response = budgetService.updateBudget(userId, tripId, budgetId, request);

        // then
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("예산 수정 성공 - 통화 변경")
    void updateBudget_Success_ChangeCurrency() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long budgetId = 1L;

        BudgetUpdateRequest request = new BudgetUpdateRequest(
                new BigDecimal("5000"),
                Currency.EUR
        );

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .build();

        Budget budget = Budget.builder()
                .id(budgetId)
                .trip(trip)
                .amount(new BigDecimal("4000"))
                .currency(Currency.USD)
                .build();

        given(budgetRepository.findById(budgetId)).willReturn(Optional.of(budget));
        given(budgetRepository.findByTripAndCurrency(trip, Currency.EUR))
                .willReturn(Optional.empty());

        // when
        BudgetResponse response = budgetService.updateBudget(userId, tripId, budgetId, request);

        // then
        assertThat(response.getCurrency()).isEqualTo(Currency.EUR);
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("예산 삭제 성공")
    void deleteBudget_Success() {
        // given
        Long userId = 1L;
        Long tripId = 1L;
        Long budgetId = 1L;

        User user = User.builder().id(userId).build();
        Trip trip = Trip.builder()
                .id(tripId)
                .user(user)
                .build();

        Budget budget = Budget.builder()
                .id(budgetId)
                .trip(trip)
                .amount(new BigDecimal("4000"))
                .currency(Currency.USD)
                .build();

        given(budgetRepository.findById(budgetId)).willReturn(Optional.of(budget));

        // when
        budgetService.deleteBudget(userId, tripId, budgetId);

        // then
        verify(budgetRepository).delete(budget);
    }
}
