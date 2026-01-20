package io.mero.app.global.service;

import io.mero.app.global.dto.S3UploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * S3 파일 관리 서비스 인터페이스
 */
public interface S3Service {

    // Trip 커버 이미지
    S3UploadResult uploadTripCoverImage(Long userId, MultipartFile image);
    void deleteTripCoverImage(String s3Key);

    // Footprint 사진
    List<S3UploadResult> uploadFootprintPhotos(Long userId, Long tripId, Long footprintId, List<MultipartFile> photos);
    void deleteFootprintPhoto(String s3Key);
    void deleteFootprintPhotos(List<String> s3Keys);

    // 여행 관련 파일
    S3UploadResult uploadTripDocument(Long userId, Long tripId, MultipartFile document);
    void deleteTripDocument(String s3Key);
}
