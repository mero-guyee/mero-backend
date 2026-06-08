package io.mero.app.domain.trip.dto;

import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.global.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class TripResponse {

    private Long id;
    private String clientId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> countries;
    private String imageUrl;
    private LocalDateTime createdAt;

    public static TripResponse from(Trip trip, String imageUrl) {
        return new TripResponse(
                trip.getId(),
                trip.getClientId(),
                trip.getTitle(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getCountries(),
                imageUrl,
                trip.getCreatedAt()
        );
    }
}
