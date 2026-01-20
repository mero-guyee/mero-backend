package io.mero.app.domain.trip.entity;

import io.mero.app.global.entity.BaseEntity;
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

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "s3_url", nullable = false, length = 1000)
    private String s3Url;

    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Builder
    public TripCoverImage(Trip trip, String s3Key, String s3Url,
                          String originalFilename, Long fileSize, String mimeType,
                          Integer width, Integer height) {
        this.trip = trip;
        this.s3Key = s3Key;
        this.s3Url = s3Url;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
    }

    public void update(String s3Key, String s3Url, String originalFilename,
                       Long fileSize, String mimeType, Integer width, Integer height) {
        this.s3Key = s3Key;
        this.s3Url = s3Url;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
    }
}
