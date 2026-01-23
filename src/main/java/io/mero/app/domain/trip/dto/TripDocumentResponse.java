package io.mero.app.domain.trip.dto;

import io.mero.app.domain.trip.entity.TripDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TripDocumentResponse {
    private Long id;
    private String clientId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;

    public static TripDocumentResponse from(TripDocument document) {
        return new TripDocumentResponse(
                document.getId(),
                document.getClientId(),
                document.getOriginalFileName(),
                document.getFileUrl(),
                document.getFileSize()
        );
    }
}
