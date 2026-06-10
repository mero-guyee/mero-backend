package io.mero.app.domain.footprint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequest {

    @Size(max = 200, message = "{location.placeName.size}")
    private String placeName;

    @Size(max = 100, message = "{location.country.size}")
    private String country;

    @Size(max = 100, message = "{location.city.size}")
    private String city;

    private BigDecimal latitude;

    private BigDecimal longitude;
}
