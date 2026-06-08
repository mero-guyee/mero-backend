package io.mero.app.domain.footprint.dto;

import io.mero.app.domain.footprint.entity.Photo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PhotoResponse {

    private Long id;
    private String clientId;
    private String s3Url;
    private String originalFilename;
    private Long fileSize;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Integer orderIndex;

    public static PhotoResponse from(Photo photo, String s3Url) {
        return PhotoResponse.builder()
                .id(photo.getId())
                .clientId(photo.getClientId())
                .s3Url(s3Url)
                .originalFilename(photo.getOriginalFilename())
                .fileSize(photo.getFileSize())
                .mimeType(photo.getMimeType() != null ? photo.getMimeType().getMimeType() : null)
                .width(photo.getWidth())
                .height(photo.getHeight())
                .orderIndex(photo.getOrderIndex())
                .build();
    }
}
