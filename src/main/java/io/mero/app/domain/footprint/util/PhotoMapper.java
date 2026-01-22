package io.mero.app.domain.footprint.util;

import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.footprint.entity.Photo;
import io.mero.app.global.dto.S3UploadResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PhotoMapper {

    /**
     * Convert list of S3UploadResult to Photo entities
     * @param uploadResults List of S3 upload results
     * @param footprint Parent footprint (can be null for new footprint)
     * @return List of Photo entities
     */
    public static List<Photo> fromUploadResults(List<S3UploadResult> uploadResults, Footprint footprint) {
        if (uploadResults == null || uploadResults.isEmpty()) {
            return new ArrayList<>();
        }

        List<Photo> photos = new ArrayList<>();
        for (int i = 0; i < uploadResults.size(); i++) {
            S3UploadResult result = uploadResults.get(i);
            Photo photo = Photo.builder()
                    .footprint(footprint)
                    .s3Key(result.getS3Key())
                    .s3Url(result.getS3Url())
                    .originalFilename(result.getOriginalFilename())
                    .fileSize(result.getFileSize())
                    .mimeType(result.getMimeType())
                    .orderIndex(i)
                    .build();
            photos.add(photo);
        }
        return photos;
    }

    /**
     * Convert Photo entities to URL string list
     */
    public static List<String> toUrls(List<Photo> photos) {
        if (photos == null || photos.isEmpty()) {
            return Collections.emptyList();
        }
        return photos.stream()
                .sorted(Comparator.comparing(Photo::getOrderIndex))
                .map(Photo::getS3Url)
                .toList();
    }

    /**
     * Convert Photo entities to S3 key string list
     */
    public static List<String> toS3Keys(List<Photo> photos) {
        if (photos == null || photos.isEmpty()) {
            return Collections.emptyList();
        }
        return photos.stream()
                .sorted(Comparator.comparing(Photo::getOrderIndex))
                .map(Photo::getS3Key)
                .toList();
    }
}
