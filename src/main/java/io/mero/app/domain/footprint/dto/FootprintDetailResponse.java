package io.mero.app.domain.footprint.dto;

import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.domain.footprint.entity.Footprint;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public class FootprintDetailResponse {

    private Long id;
    private Long tripId;
    private String content;
    private LocalDate date;
    private List<LocationResponse> locations;
    private String weatherInfo;
    private List<String> photoUrls;
    private List<ExpenseResponse> expenses;

    public static FootprintDetailResponse from(Footprint footprint, List<String> photoUrls, List<ExpenseResponse> expenses) {
        return new FootprintDetailResponse(
                footprint.getId(),
                footprint.getTrip().getId(),
                footprint.getContent(),
                footprint.getDate(),
                LocationResponse.fromList(footprint.getLocations()),
                footprint.getWeatherInfo(),
                photoUrls,
                expenses == null ? Collections.emptyList() : expenses
        );
    }
}
