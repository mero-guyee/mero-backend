package io.mero.app.domain.diary.service;

import io.mero.app.domain.diary.dto.DiaryCreateRequest;
import io.mero.app.domain.diary.dto.DiaryResponse;
import io.mero.app.domain.diary.dto.DiaryUpdateRequest;
import io.mero.app.domain.diary.entity.Diary;
import io.mero.app.domain.diary.repository.DiaryRepository;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final TripRepository tripRepository;
    private final MessageUtil messageUtil;

    @Transactional
    public DiaryResponse createDiary(Long userId, DiaryCreateRequest request) {
        Trip trip = findTripById(request.getTripId());

        validateOwner(trip, userId);

        Diary diary = Diary.builder()
                .trip(trip)
                .content(request.getContent())
                .date(request.getDate())
                .location(request.getLocation())
                .photoUrls(request.getPhotoUrls())
                .build();

        Diary savedDiary = diaryRepository.save(diary);

        return DiaryResponse.from(savedDiary);
    }

    public List<DiaryResponse> getDiariesByTrip(Long userId, Long tripId) {
        Trip trip = findTripById(tripId);

        validateOwner(trip, userId);

        List<Diary> diaries = diaryRepository.findByTripIdOrderByDateDesc(tripId);
        return diaries.stream()
                .map(DiaryResponse::from)
                .toList();
    }

    public DiaryResponse getDiary(Long userId, Long diaryId) {
        Diary diary = findDiaryById(diaryId);

        validateOwner(diary.getTrip(), userId);

        return DiaryResponse.from(diary);
    }

    @Transactional
    public DiaryResponse updateDiary(Long userId, Long diaryId, DiaryUpdateRequest request) {
        Diary diary = findDiaryById(diaryId);

        validateOwner(diary.getTrip(), userId);

        diary.update(
                request.getContent(),
                request.getDate(),
                request.getLocation(),
                request.getPhotoUrls()
        );

        return DiaryResponse.from(diary);
    }

    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {
        Diary diary = findDiaryById(diaryId);

        validateOwner(diary.getTrip(), userId);

        diaryRepository.delete(diary);
    }

    private Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageUtil.getMessage("error.trip.not_found")
                ));
    }

    private Diary findDiaryById(Long diaryId) {
        return diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageUtil.getMessage("error.diary.notFound")
                ));
    }

    private void validateOwner(Trip trip, Long userId) {
        if(!trip.isOwner(userId)) {
            throw new IllegalArgumentException(
                    messageUtil.getMessage("error.forbidden")
            );
        }
    }


}
