package io.mero.app.domain.footprint.dto;

import io.mero.app.domain.footprint.entity.Photo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PhotoResponse {

    private Long id;
    private String s3Url;
    private String originalFilename;
    private Long fileSize;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Integer orderIndex;

    public static PhotoResponse from(Photo photo) {
        return PhotoResponse.builder()
                .id(photo.getId())
                .s3Url(photo.getS3Url())
                .originalFilename(photo.getOriginalFilename())
                .fileSize(photo.getFileSize())
                .mimeType(photo.getMimeType())
                .width(photo.getWidth())
                .height(photo.getHeight())
                .orderIndex(photo.getOrderIndex())
                .build();
    }
}
