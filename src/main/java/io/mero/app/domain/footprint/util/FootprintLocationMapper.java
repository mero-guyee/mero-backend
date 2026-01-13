package io.mero.app.domain.footprint.util;

import io.mero.app.domain.footprint.dto.LocationRequest;
import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.footprint.entity.FootprintLocation;

import java.util.ArrayList;
import java.util.List;


public class FootprintLocationMapper {

    public static List<FootprintLocation> fromRequests(List<LocationRequest> locationRequests, Footprint footprint) {
        if (locationRequests == null || locationRequests.isEmpty()) {
            return new ArrayList<>();
        }

        List<FootprintLocation> locations = new ArrayList<>();
        for (int i = 0; i < locationRequests.size(); i++) {
            LocationRequest request = locationRequests.get(i);
            FootprintLocation location = FootprintLocation.builder()
                    .footprint(footprint)
                    .placeName(request.getPlaceName())
                    .country(request.getCountry())
                    .city(request.getCity())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .order(i)
                    .build();
            locations.add(location);
        }
        return locations;
    }
}
