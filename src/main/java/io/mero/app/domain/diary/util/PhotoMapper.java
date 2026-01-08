package io.mero.app.domain.diary.util;

import io.mero.app.domain.diary.entity.Diary;
import io.mero.app.domain.diary.entity.Photo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PhotoMapper {

    /**
     * Convert list of URL strings to Photo entities
     * @param photoUrls List of photo URLs
     * @param diary Parent diary (can be null for new diary)
     * @return List of Photo entities with auto-populated fields
     */
    public static List<Photo> fromUrls(List<String> photoUrls, Diary diary) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            return new ArrayList<>();
        }

        List<Photo> photos = new ArrayList<>();
        for (int i = 0; i < photoUrls.size(); i++) {
            String url = photoUrls.get(i);
            Photo photo = Photo.builder()
                    .diary(diary)
                    .imageUrl(url)
                    .fileName(extractFileName(url))
                    .mimeType(extractMimeType(url))
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
                .map(Photo::getImageUrl)
                .toList();
    }

    /**
     * Extract filename from URL
     * Example: "https://example.com/photos/image.jpg" -> "image.jpg"
     */
    private static String extractFileName(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < url.length() - 1) {
                return url.substring(lastSlash + 1);
            }
        } catch (Exception e) {
            // If parsing fails, return null
        }
        return null;
    }

    /**
     * Extract MIME type from URL extension
     * Example: "image.jpg" -> "image/jpeg"
     */
    private static String extractMimeType(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerUrl.endsWith(".png")) {
            return "image/png";
        } else if (lowerUrl.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerUrl.endsWith(".webp")) {
            return "image/webp";
        }

        return null; // Unknown or no extension
    }
}
