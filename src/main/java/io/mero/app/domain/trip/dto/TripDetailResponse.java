package io.mero.app.domain.trip.dto;

import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.entity.TripDocument;
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

    private List<TripDocumentResponse> documents;

    public static TripDetailResponse from(Trip trip, List<TripDocument> documents) {
        return new TripDetailResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getCountries(),
                trip.getCoverImageUrl(),
                trip.getCreatedAt(),
                documents == null ? Collections.emptyList() :
                        documents.stream()
                        .map(TripDocumentResponse::from)
                        .collect(Collectors.toList())
        );
    }
}

