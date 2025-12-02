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
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> countries;
    private BigDecimal totalBudget;
    private Currency budgetCurrency;
    private Currency defaultCurrency;
    private LocalDateTime createdAt;

    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getDescription(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getCountries(),
                trip.getTotalBudget(),
                trip.getBudgetCurrency(),
                trip.getDefaultCurrency(),
                trip.getCreatedAt()
        );
    }
}
