package io.mero.app.domain.diary.dto;

import io.mero.app.domain.diary.entity.Diary;
import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.global.embedded.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class DiaryResponse {

    private Long id;
    private Long tripId;
    private String content;
    private LocalDate date;
    private Location location;
    private String weatherInfo;
    private List<String> photoUrls;
    private List<ExpenseResponse> expenses;
    private LocalDateTime createdAt;

    public static DiaryResponse from(Diary diary) {
        return new DiaryResponse(
                diary.getId(),
                diary.getTrip().getId(),
                diary.getContent(),
                diary.getDate(),
                diary.getLocation(),
                diary.getWeatherInfo(),
                diary.getPhotoUrls(),
                null,
                diary.getCreatedAt()
        );
    }

    public static DiaryResponse from(Diary diary, List<ExpenseResponse> expenses) {
        return new DiaryResponse(
                diary.getId(),
                diary.getTrip().getId(),
                diary.getContent(),
                diary.getDate(),
                diary.getLocation(),
                diary.getWeatherInfo(),
                diary.getPhotoUrls(),
                expenses,
                diary.getCreatedAt()
        );
    }
}
