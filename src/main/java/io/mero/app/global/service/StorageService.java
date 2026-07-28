package io.mero.app.global.service;

import io.mero.app.global.dto.StorageUploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {

    StorageUploadResult uploadTripCoverImage(Long userId, MultipartFile image);
    void deleteTripCoverImage(String storageKey);

    StorageUploadResult uploadProfileImage(Long userId, MultipartFile image);
    void deleteProfileImage(String storageKey);

    List<StorageUploadResult> uploadFootprintPhotos(Long userId, Long tripId, Long footprintId, List<MultipartFile> photos);
    void deleteFootprintPhoto(String storageKey);
    void deleteFootprintPhotos(List<String> storageKeys);

    StorageUploadResult uploadTripDocument(Long userId, Long tripId, MultipartFile document);
    void deleteTripDocument(String storageKey);

    String getImageSignedUrl(String storageKey);
    String getDocumentSignedUrl(String storageKey);
}
