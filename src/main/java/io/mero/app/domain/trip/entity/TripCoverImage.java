package io.mero.app.domain.trip.entity;

import io.mero.app.global.entity.BaseEntity;
import io.mero.app.global.enums.ImageMimeType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trip_cover_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripCoverImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    /** 컬럼명 s3_key는 S3에서 옮겨오기 전 이름이라 그대로 두었다. 값은 스토리지 구현체와 무관한 객체 키다. */
    @Column(name = "s3_key", nullable = false)
    private String storageKey;

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

    @Builder
    public TripCoverImage(Trip trip, String storageKey,
                          String originalFilename, Long fileSize, ImageMimeType mimeType,
                          Integer width, Integer height) {
        this.trip = trip;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
    }

    public void update(String storageKey, String originalFilename,
                       Long fileSize, ImageMimeType mimeType, Integer width, Integer height) {
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
    }
}
