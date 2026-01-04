package io.mero.app.domain.diary.service;

import io.mero.app.domain.diary.dto.DiaryCreateRequest;
import io.mero.app.domain.diary.dto.DiaryResponse;
import io.mero.app.domain.diary.dto.DiaryUpdateRequest;
import io.mero.app.domain.diary.entity.Diary;
import io.mero.app.domain.diary.repository.DiaryRepository;
import io.mero.app.domain.exchange.service.ExchangeRateService;
import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.domain.expense.repository.ExpenseRepository;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final TripRepository tripRepository;
    private final ExpenseRepository expenseRepository;
    private final ExchangeRateService exchangeRateService;
    private final MessageUtil messageUtil;

    @Transactional
    public DiaryResponse createDiary(Long userId, Long tripId, DiaryCreateRequest request) {
        Trip trip = findTripById(tripId);
        validateOwner(trip, userId);

        Diary diary = Diary.builder()
                .trip(trip)
                .title(request.getTitle())
                .content(request.getContent())
                .date(request.getDate())
                .photoUrls(request.getPhotoUrls())
                .location(request.getLocation())
                .build();

        Diary savedDiary = diaryRepository.save(diary);

        return DiaryResponse.from(savedDiary);
    }

    public List<DiaryResponse> getDiaries(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);
        validateOwner(trip, userId);

        List<Diary> diaries = diaryRepository.findByTripIdOrderByDateDesc(tripId);
        return diaries.stream()
                .map(DiaryResponse::from)
                .toList();
    }

    public DiaryResponse getDiary(Long userId, Long tripId, Long diaryId) {
        Diary diary = findDiaryById(diaryId);
        Trip trip = diary.getTrip();

        validateTripMatch(trip, tripId);
        validateOwner(trip, userId);

        List<ExpenseResponse> expenses = expenseRepository.findByDiary(diary)
                .stream()
                .map(expense -> {
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
                })
                .toList();

        return DiaryResponse.from(diary, expenses);
    }

    @Transactional
    public DiaryResponse updateDiary(Long userId, Long tripId, Long diaryId, DiaryUpdateRequest request) {
        Diary diary = findDiaryById(diaryId);
        Trip trip = diary.getTrip();

        validateTripMatch(trip, tripId);
        validateOwner(trip, userId);

        diary.update(
                request.getTitle(),
                request.getContent(),
                request.getDate(),
                request.getLocation(),
                request.getPhotoUrls()
        );

        return DiaryResponse.from(diary);
    }

    @Transactional
    public void deleteDiary(Long userId, Long tripId, Long diaryId) {
        Diary diary = findDiaryById(diaryId);
        Trip trip = diary.getTrip();

        validateTripMatch(trip, tripId);
        validateOwner(trip, userId);

        diaryRepository.delete(diary);
    }

    private Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.trip.notFound")));
    }

    private Diary findDiaryById(Long diaryId) {
        return diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.diary.notFound")));
    }

    private void validateOwner(Trip trip, Long userId) {
        if (!trip.isOwner(userId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }

    private void validateTripMatch(Trip trip, Long tripId) {
        if (!trip.getId().equals(tripId)) {
            throw new ForbiddenException(
                    messageUtil.getMessage("error.forbidden"));
        }
    }
}
