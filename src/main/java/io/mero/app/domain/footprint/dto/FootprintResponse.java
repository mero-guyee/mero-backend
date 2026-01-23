package io.mero.app.domain.footprint.dto;

import io.mero.app.domain.footprint.entity.Footprint;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class FootprintResponse {

    private Long id;
    private String clientId;
    private Long tripId;
    private String content;
    private LocalDate date;
    private List<LocationResponse> locations;
    private String thumbnailUrl;

    public static FootprintResponse from(Footprint footprint) {
        List<String> photoUrls = footprint.getPhotoUrls();
        String thumbnailUrl = photoUrls.isEmpty() ? null : photoUrls.get(0);

        return new FootprintResponse(
                footprint.getId(),
                footprint.getClientId(),
                footprint.getTrip().getId(),
                footprint.getContent(),
                footprint.getDate(),
                LocationResponse.fromList(footprint.getLocations()),
                thumbnailUrl
        );
    }
}
