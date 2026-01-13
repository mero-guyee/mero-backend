package io.mero.app.domain.footprint.entity;

import io.mero.app.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "footprint_id", nullable = false)
    private Footprint footprint;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", length = 20, nullable = false)
    private UploadStatus uploadStatus;

    @Column(name = "is_synced", nullable = false)
    private Boolean isSynced = false;

    @Builder
    public Photo(Footprint footprint, String imageUrl, String fileName,
                 String mimeType, Integer orderIndex) {
        this.footprint = footprint;
        this.imageUrl = imageUrl;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.orderIndex = orderIndex;
        this.uploadStatus = UploadStatus.COMPLETED;
        this.isSynced = false;
    }

    public void setFootprint(Footprint footprint) {
        this.footprint = footprint;
    }

    public void startUpload() {
        this.uploadStatus = UploadStatus.UPLOADING;
    }

    public void completeUpload() {
        this.uploadStatus = UploadStatus.COMPLETED;
    }

    public void failUpload() {
        this.uploadStatus = UploadStatus.FAILED;
    }

    public void markAsSynced() {
        this.isSynced = true;
    }

    public void markAsUnsynced() {
        this.isSynced = false;
    }

    public void updateOrder(Integer orderIndex) {
        if (orderIndex == null || orderIndex < 0) {
            throw new IllegalArgumentException("순서는 0 이상이어야 합니다");
        }
        this.orderIndex = orderIndex;
    }

}
