package io.mero.app.domain.trip.dto;

import io.mero.app.global.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TripUpdateRequest {

    @NotBlank(message = "{trip.title.notBlank}")
    @Size(max = 100, message = "{trip.title.size}")
    private String title;

    @NotNull(message = "{trip.startDate.notNull}")
    private LocalDate startDate;

    @NotNull(message = "{trip.endDate.notNull}")
    private LocalDate endDate;

    private List<String> countries;
    private Currency defaultCurrency;
    private String imageUrl;

}
