package io.mero.app.domain.expense.service;

import io.mero.app.domain.diary.entity.Diary;
import io.mero.app.domain.diary.repository.DiaryRepository;
import io.mero.app.domain.expense.dto.ExpenseCreateRequest;
import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.domain.expense.dto.ExpenseUpdateRequest;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.repository.ExpenseRepository;
import io.mero.app.domain.exchange.service.ExchangeRateService;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final TripRepository tripRepository;
    private final DiaryRepository diaryRepository;
    private final ExchangeRateService exchangeRateService;
    private final MessageUtil messageUtil;

    @Transactional
    public ExpenseResponse createExpense(Long userId, ExpenseCreateRequest request) {
        Trip trip = findTripById(request.getTripId());
        validateOwner(trip, userId);

        // Diary 연결 (선택)
        Diary diary = null;
        if (request.getDiaryId() != null) {
            diary = findDiaryById(request.getDiaryId());
            validateDiaryBelongsToTrip(diary, trip);
        }

        // 환율 결정
        BigDecimal exchangeRate;
        boolean isCustomRate = false;
        String rateSource = null;

        if (request.getCustomExchangeRate() != null) {
            exchangeRate = request.getCustomExchangeRate();
            isCustomRate = true;
            rateSource = request.getExchangeRateSource();
        } else {
            exchangeRate = exchangeRateService.getRate(
                    request.getCurrency(),
                    trip.getDefaultCurrency(),
                    request.getDate()
            );
        }

        Expense expense = Expense.builder()
                .trip(trip)
                .diary(diary)  // ← 추가
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .category(request.getCategory())
                .description(request.getDescription())
                .date(request.getDate())
                .location(request.getLocation())
                .build();

        Expense savedExpense = expenseRepository.save(expense);

        return ExpenseResponse.from(
                savedExpense,
                exchangeRate,
                trip.getDefaultCurrency(),
                isCustomRate,
                rateSource
        );
    }

    public List<ExpenseResponse> getExpensesByTrip(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(trip, userId);

        List<Expense> expenses = expenseRepository.findByTripOrderByDateDesc(trip);

        return expenses.stream()
                .map(expense -> {
                    BigDecimal exchangeRate = exchangeRateService.getRate(
                            expense.getCurrency(),
                            trip.getDefaultCurrency(),
                            expense.getDate()
                    );
                    return ExpenseResponse.from(
                            expense,
                            exchangeRate,
                            trip.getDefaultCurrency(),
                            false,
                            null
                    );
                })
                .collect(Collectors.toList());
    }

    public ExpenseResponse getExpense(Long userId, Long expenseId) {
        Expense expense = findExpenseById(expenseId);
        validateOwner(expense.getTrip(), userId);

        BigDecimal exchangeRate = exchangeRateService.getRate(
                expense.getCurrency(),
                expense.getTrip().getDefaultCurrency(),
                expense.getDate()
        );

        return ExpenseResponse.from(
                expense,
                exchangeRate,
                expense.getTrip().getDefaultCurrency(),
                false,
                null
        );
    }

    @Transactional
    public ExpenseResponse updateExpense(Long userId, Long expenseId, ExpenseUpdateRequest request) {
        Expense expense = findExpenseById(expenseId);
        validateOwner(expense.getTrip(), userId);

        // Diary 연결/해제
        if (request.getDiaryId() != null) {
            Diary diary = findDiaryById(request.getDiaryId());
            validateDiaryBelongsToTrip(diary, expense.getTrip());
            expense.linkToDiary(diary);
        } else {
            expense.unlinkFromDiary();
        }

        // 환율 결정
        BigDecimal exchangeRate;
        boolean isCustomRate = false;
        String rateSource = null;

        if (request.getCustomExchangeRate() != null) {
            exchangeRate = request.getCustomExchangeRate();
            isCustomRate = true;
            rateSource = request.getExchangeRateSource();
        } else {
            exchangeRate = exchangeRateService.getRate(
                    request.getCurrency(),
                    expense.getTrip().getDefaultCurrency(),
                    request.getDate()
            );
        }

        expense.update(
                request.getAmount(),
                request.getCurrency(),
                request.getCategory(),
                request.getDescription(),
                request.getDate(),
                request.getLocation()
        );

        return ExpenseResponse.from(
                expense,
                exchangeRate,
                expense.getTrip().getDefaultCurrency(),
                isCustomRate,
                rateSource
        );
    }

    @Transactional
    public void deleteExpense(Long userId, Long expenseId) {
        Expense expense = findExpenseById(expenseId);
        validateOwner(expense.getTrip(), userId);

        expenseRepository.delete(expense);
    }

    private Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageUtil.getMessage("error.trip.notFound")));
    }

    private Expense findExpenseById(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageUtil.getMessage("error.expense.notFound")));
    }

    private Diary findDiaryById(Long diaryId) {
        return diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageUtil.getMessage("error.diary.notFound")));
    }

    private void validateOwner(Trip trip, Long userId) {
        if (!trip.isOwner(userId)) {
            throw new IllegalArgumentException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }

    private void validateDiaryBelongsToTrip(Diary diary, Trip trip) {
        if (!diary.getTrip().equals(trip)) {
            throw new IllegalArgumentException("같은 여행의 일기만 연결할 수 있습니다");
        }
    }
}