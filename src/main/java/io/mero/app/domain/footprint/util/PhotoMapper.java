package io.mero.app.domain.footprint.util;

import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.footprint.entity.Photo;
import io.mero.app.global.dto.StorageUploadResult;
import io.mero.app.global.enums.ImageMimeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PhotoMapper {

    public static List<Photo> fromUploadResults(List<StorageUploadResult> uploadResults, Footprint footprint) {
        if (uploadResults == null || uploadResults.isEmpty()) {
            return new ArrayList<>();
        }

        List<Photo> photos = new ArrayList<>();
        for (int i = 0; i < uploadResults.size(); i++) {
            StorageUploadResult result = uploadResults.get(i);
            Photo photo = Photo.builder()
                    .footprint(footprint)
                    .s3Key(result.getStorageKey())
                    .s3Url(result.getStorageUrl())
                    .originalFilename(result.getOriginalFilename())
                    .fileSize(result.getFileSize())
                    .mimeType(ImageMimeType.fromMimeType(result.getMimeType()))
                    .orderIndex(i)
                    .build();
            photos.add(photo);
        }
        return photos;
    }

    public static List<String> toUrls(List<Photo> photos) {
        if (photos == null || photos.isEmpty()) {
            return Collections.emptyList();
        }
        return photos.stream()
                .sorted(Comparator.comparing(Photo::getOrderIndex))
                .map(Photo::getS3Url)
                .toList();
    }

    public static List<String> toStorageKeys(List<Photo> photos) {
        if (photos == null || photos.isEmpty()) {
            return Collections.emptyList();
        }
        return photos.stream()
                .sorted(Comparator.comparing(Photo::getOrderIndex))
                .map(Photo::getS3Key)
                .toList();
    }
}
