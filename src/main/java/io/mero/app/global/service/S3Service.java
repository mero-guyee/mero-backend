package io.mero.app.global.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * S3 파일 관리 서비스 인터페이스
 */
public interface S3Service {

    // Trip
    String uploadTripImage(Long userId, MultipartFile image);
    void deleteTripImage(String imageUrl);

    // Footprint 사진
    List<String> uploadFootprintPhotos(Long userId, Long tripId, Long footprintId, List<MultipartFile> photos);
    void deleteFootprintPhoto(String photoUrl);
    void deleteFootprintPhotos(List<String> photoUrls);

    // 여행 관련 파일
    String uploadTripDocument(Long userId, Long tripId, MultipartFile document);
    void deleteTripDocument(String documentUrl);
}


