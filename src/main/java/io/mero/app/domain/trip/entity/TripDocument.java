package io.mero.app.domain.trip.entity;

import io.mero.app.domain.footprint.entity.UploadStatus;
import io.mero.app.global.entity.BaseEntity;
import io.mero.app.global.enums.DocumentMimeType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "trip_documents")
public class TripDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column
    private String fileUrl;

    @Column(name = "local_file_path", length = 500)
    private String localFilePath;

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
                        String storedFileName, String fileUrl,
                        Long fileSize, DocumentMimeType contentType, String clientId) {
        this.trip = trip;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadStatus = UploadStatus.COMPLETED;
        this.clientId = clientId;
    }

    public String getStorageKey() {
        return storedFileName;
    }
}
