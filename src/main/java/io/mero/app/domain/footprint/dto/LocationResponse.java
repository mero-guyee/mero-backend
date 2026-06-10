package io.mero.app.domain.footprint.dto;

import io.mero.app.domain.footprint.entity.FootprintLocation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Getter
@AllArgsConstructor
public class LocationResponse {

    private String placeName;
    private String country;
    private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public static LocationResponse from(FootprintLocation location) {
        if (location == null) {
            return null;
        }
        return new LocationResponse(
                location.getPlaceName(),
                location.getCountry(),
                location.getCity(),
                location.getLatitude(),
                location.getLongitude()
        );
    }

    public static List<LocationResponse> fromList(List<FootprintLocation> locations) {
        if (locations == null || locations.isEmpty()) {
            return List.of();
        }
        return locations.stream()
                .sorted(Comparator.comparing(FootprintLocation::getSortOrder))
                .map(LocationResponse::from)
                .toList();
    }
}
