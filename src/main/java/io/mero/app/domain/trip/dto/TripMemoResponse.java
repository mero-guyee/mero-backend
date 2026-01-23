package io.mero.app.domain.trip.dto;

import io.mero.app.domain.trip.entity.TripMemo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TripMemoResponse {
    private Long id;
    private String clientId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TripMemoResponse from(TripMemo memo) {
        return new TripMemoResponse(
                memo.getId(),
                memo.getClientId(),
                memo.getTitle(),
                memo.getContent(),
                memo.getCreatedAt(),
                memo.getUpdatedAt()
        );
    }
}
