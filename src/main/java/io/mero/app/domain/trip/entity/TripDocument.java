package io.mero.app.domain.trip.entity;

import io.mero.app.domain.footprint.entity.UploadStatus;
import io.mero.app.global.entity.BaseEntity;
import io.mero.app.global.enums.DocumentMimeType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "trip_documents")
@SQLRestriction("deleted_at IS NULL")
public class TripDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20, nullable = false)
    private DocumentMimeType contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", length = 20, nullable = false)
    private UploadStatus uploadStatus;

    @Column(name = "client_id", length = 36, unique = true)
    private String clientId;

    @Builder
    public TripDocument(Trip trip, String originalFileName,
                        String storageKey,
                        Long fileSize, DocumentMimeType contentType, String clientId) {
        this.trip = trip;
        this.originalFileName = originalFileName;
        this.storageKey = storageKey;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadStatus = UploadStatus.COMPLETED;
        this.clientId = clientId;
    }
}
