package io.mero.app.domain.trip.dto;

import io.mero.app.domain.trip.entity.Trip;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class TripDetailResponse {

    private Long id;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> countries;
    private String imageUrl;
    private LocalDateTime createdAt;

    public static TripDetailResponse from(Trip trip, String imageUrl) {
        return new TripDetailResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getCountries(),
                imageUrl,
                trip.getCreatedAt()
        );
    }
}

