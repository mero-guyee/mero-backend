package io.mero.app.domain.footprint.dto;

import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.expense.dto.ExpenseResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class FootprintResponse {

    private Long id;
    private Long tripId;
    private String content;
    private LocalDate date;
    private List<LocationResponse> locations;
    private String weatherInfo;
    private List<String> photoUrls;
    private List<ExpenseResponse> expenses;
    private LocalDateTime createdAt;

    public static FootprintResponse from(Footprint footprint) {
        return new FootprintResponse(
                footprint.getId(),
                footprint.getTrip().getId(),
                footprint.getContent(),
                footprint.getDate(),
                LocationResponse.fromList(footprint.getLocations()),
                footprint.getWeatherInfo(),
                footprint.getPhotoUrls(),
                null,
                footprint.getCreatedAt()
        );
    }

    public static FootprintResponse from(Footprint footprint, List<ExpenseResponse> expenses) {
        return new FootprintResponse(
                footprint.getId(),
                footprint.getTrip().getId(),
                footprint.getContent(),
                footprint.getDate(),
                LocationResponse.fromList(footprint.getLocations()),
                footprint.getWeatherInfo(),
                footprint.getPhotoUrls(),
                expenses,
                footprint.getCreatedAt()
        );
    }
}
