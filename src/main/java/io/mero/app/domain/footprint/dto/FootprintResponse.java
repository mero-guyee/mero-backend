package io.mero.app.domain.footprint.dto;

import io.mero.app.domain.footprint.entity.Footprint;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public class FootprintResponse {

    private Long id;
    private String clientId;
    private Long tripId;
    private String content;
    private LocalDate date;
    private String weatherInfo;
    private List<LocationResponse> locations;
    private String thumbnailUrl;
    private List<PhotoResponse> photos;

    public static FootprintResponse from(Footprint footprint, String thumbnailUrl, List<PhotoResponse> photos) {
        return new FootprintResponse(
                footprint.getId(),
                footprint.getClientId(),
                footprint.getTrip().getId(),
                footprint.getContent(),
                footprint.getDate(),
                footprint.getWeatherInfo(),
                LocationResponse.fromList(footprint.getLocations()),
                thumbnailUrl,
                photos == null ? Collections.emptyList() : photos
        );
    }
}
