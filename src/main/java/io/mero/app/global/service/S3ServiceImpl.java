package io.mero.app.global.service;

import io.mero.app.global.exception.FileDeleteException;
import io.mero.app.global.exception.FileUploadException;
import io.mero.app.global.exception.InvalidFileException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    // Trip 이미지
    @Override
    public String uploadTripImage(Long userId, MultipartFile image) {
        validateImageFile(image);
        String path = generateTripImagePath(userId);
        return upload(image, path);
    }

    // Footprint 사진들
    @Override
    public List<String> uploadFootprintPhotos(Long userId, Long tripId, Long footprintId,
                                              List<MultipartFile> photos) {
        return null;
    }

    // 여행 문서
    @Override
    public String uploadTripDocument(Long userId, Long tripId, MultipartFile document) {
        return null;
    }

    // 공통 업로드 로직
    private String upload(MultipartFile file, String path) {
        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String fullPath = path + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fullPath)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return generateS3Url(fullPath);
        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", e.getMessage());
            throw new FileUploadException("파일 업로드 실패", e);
        }
    }

    private String generateS3Url(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    @Override
    public void deleteTripImage(String imageUrl) {
        deleteFile(imageUrl);
    }

    @Override
    public void deleteFootprintPhoto(String photoUrl) {
        deleteFile(photoUrl);
    }

    @Override
    public void deleteFootprintPhotos(List<String> photoUrls) {
        photoUrls.forEach(this::deleteFile);
    }

    @Override
    public void deleteTripDocument(String documentUrl) {
        deleteFile(documentUrl);
    }

    // 공통 삭제 로직
    private void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            String key = extractKeyFromUrl(fileUrl);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 완료: {}", key);
        } catch (Exception e) {
            log.error("파일 삭제 실패: {}", e.getMessage());
            throw new FileDeleteException("파일 삭제 실패", e);
        }
    }

    // 경로 생성 헬퍼 메서드
    private String generateTripImagePath(Long userId) {
        return String.format("users/%d/trips/images/", userId);
    }

    private String generateFootprintPhotoPath(Long userId, Long tripId, Long footprintId) {
        return String.format("users/%d/trips/%d/footprints/%d/photos/",
                userId, tripId, footprintId);
    }

    private String generateTripDocumentPath(Long userId, Long tripId) {
        return String.format("users/%d/trips/%d/documents/", userId, tripId);
    }

    // 검증 메서드
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("파일이 비어있습니다");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InvalidFileException("이미지 파일만 업로드 가능합니다");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new InvalidFileException("파일 크기는 10MB를 초과할 수 없습니다");
        }
    }

    private String extractKeyFromUrl(String fileUrl) {
        String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucket, region);
        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }

        String altPrefix = String.format("https://s3.%s.amazonaws.com/%s/", region, bucket);
        if (fileUrl.startsWith(altPrefix)) {
            return fileUrl.substring(altPrefix.length());
        }
        throw new IllegalArgumentException("잘못된 S3 URL 형식입니다: " + fileUrl);
    }
}
