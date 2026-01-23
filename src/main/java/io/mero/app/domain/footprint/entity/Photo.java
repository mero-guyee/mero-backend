package io.mero.app.domain.footprint.entity;

import io.mero.app.domain.footprint.listener.PhotoEntityListener;
import io.mero.app.global.entity.BaseEntity;
import io.mero.app.global.enums.ImageMimeType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(PhotoEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "footprint_id", nullable = false)
    private Footprint footprint;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "s3_url", nullable = false, length = 1000)
    private String s3Url;

    @Column(name = "local_file_path", length = 500)
    private String localFilePath;

    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "mime_type", length = 20)
    private ImageMimeType mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", length = 20, nullable = false)
    private UploadStatus uploadStatus;

    @Column(name = "client_id", length = 36, unique = true)
    private String clientId;

    @Builder
    public Photo(Footprint footprint, String s3Key, String s3Url,
                 String originalFilename, Long fileSize, ImageMimeType mimeType,
                 Integer width, Integer height, Integer orderIndex, String clientId) {
        this.footprint = footprint;
        this.s3Key = s3Key;
        this.s3Url = s3Url;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
        this.orderIndex = orderIndex;
        this.uploadStatus = UploadStatus.COMPLETED;
        this.clientId = clientId;
    }

    public void update(String s3Key, String s3Url, String originalFilename,
                       Long fileSize, ImageMimeType mimeType, Integer width, Integer height) {
        this.s3Key = s3Key;
        this.s3Url = s3Url;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
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

    public void updateOrder(Integer orderIndex) {
        if (orderIndex == null || orderIndex < 0) {
            throw new IllegalArgumentException("순서는 0 이상이어야 합니다");
        }
        this.orderIndex = orderIndex;
    }

}
