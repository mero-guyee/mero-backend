package io.mero.app.domain.trip.dto;

import io.mero.app.domain.file.dto.FileResponse;
import io.mero.app.domain.file.entity.File;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.global.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    private List<FileResponse> files;

    public static TripDetailResponse from(Trip trip, List<File> files) {
        return new TripDetailResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getCountries(),
                trip.getCoverImageUrl(),
                trip.getCreatedAt(),
                files == null ? Collections.emptyList() :
                        files.stream()
                        .map(FileResponse::from)
                        .collect(Collectors.toList())
        );
    }
}

