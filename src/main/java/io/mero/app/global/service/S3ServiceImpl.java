package io.mero.app.global.service;

import io.mero.app.global.dto.S3UploadResult;
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

    @Override
    public S3UploadResult uploadTripCoverImage(Long userId, MultipartFile image) {
        validateImageFile(image);
        String path = generateTripImagePath(userId);
        return upload(image, path);
    }

    @Override
    public List<S3UploadResult> uploadFootprintPhotos(Long userId, Long tripId, Long footprintId,
                                                       List<MultipartFile> photos) {
        return photos.stream()
                .peek(this::validateImageFile)
                .map(photo -> {
                    String path = generateFootprintPhotoPath(userId, tripId, footprintId);
                    return upload(photo, path);
                })
                .collect(Collectors.toList());
    }

    @Override
    public S3UploadResult uploadTripDocument(Long userId, Long tripId, MultipartFile document) {
        validateDocumentFile(document);
        String path = generateTripDocumentPath(userId, tripId);
        return upload(document, path);
    }

    private S3UploadResult upload(MultipartFile file, String path) {
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

            String s3Url = generateS3Url(fullPath);

            return new S3UploadResult(
                    fullPath,
                    s3Url,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", e.getMessage());
            throw new FileUploadException("파일 업로드 실패", e);
        }
    }

    private String generateS3Url(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    @Override
    public void deleteTripCoverImage(String s3Key) {
        deleteByKey(s3Key);
    }

    @Override
    public void deleteFootprintPhoto(String s3Key) {
        deleteByKey(s3Key);
    }

    @Override
    public void deleteFootprintPhotos(List<String> s3Keys) {
        s3Keys.forEach(this::deleteByKey);
    }

    @Override
    public void deleteTripDocument(String s3Key) {
        deleteByKey(s3Key);
    }

    private void deleteByKey(String s3Key) {
        if (s3Key == null || s3Key.isEmpty()) {
            return;
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 완료: {}", s3Key);
        } catch (Exception e) {
            log.error("파일 삭제 실패: {}", e.getMessage());
            throw new FileDeleteException("파일 삭제 실패", e);
        }
    }

    private String generateTripImagePath(Long userId) {
        return String.format("users/%d/trips/cover/", userId);
    }

    private String generateFootprintPhotoPath(Long userId, Long tripId, Long footprintId) {
        return String.format("users/%d/trips/%d/footprints/%d/photos/", userId, tripId, footprintId);
    }

    private String generateTripDocumentPath(Long userId, Long tripId) {
        return String.format("users/%d/trips/%d/documents/", userId, tripId);
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("파일이 비어있습니다");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InvalidFileException("이미지 파일만 업로드 가능합니다");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new InvalidFileException("파일 크기는 10MB를 초과할 수 없습니다");
        }
    }

    private void validateDocumentFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("파일이 비어있습니다");
        }

        if (file.getSize() > 20 * 1024 * 1024) {
            throw new InvalidFileException("파일 크기는 20MB를 초과할 수 없습니다");
        }
    }
}
