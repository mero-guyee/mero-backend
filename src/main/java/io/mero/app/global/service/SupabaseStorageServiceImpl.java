package io.mero.app.global.service;

import io.mero.app.global.dto.StorageUploadResult;
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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseStorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${storage.bucket.images}")
    private String imagesBucket;

    @Value("${storage.bucket.docs}")
    private String docsBucket;

    @Value("${storage.signed-url-expiry-seconds:3600}")
    private long signedUrlExpirySeconds;

    @Override
    public StorageUploadResult uploadTripCoverImage(Long userId, MultipartFile image) {
        validateImageFile(image);
        String path = String.format("users/%d/trips/cover/", userId);
        return upload(image, path, imagesBucket);
    }

    @Override
    public List<StorageUploadResult> uploadFootprintPhotos(Long userId, Long tripId, Long footprintId,
                                                           List<MultipartFile> photos) {
        return photos.stream()
                .peek(this::validateImageFile)
                .map(photo -> {
                    String path = String.format("users/%d/trips/%d/footprints/%d/photos/", userId, tripId, footprintId);
                    return upload(photo, path, imagesBucket);
                })
                .collect(Collectors.toList());
    }

    @Override
    public StorageUploadResult uploadTripDocument(Long userId, Long tripId, MultipartFile document) {
        validateDocumentFile(document);
        String path = String.format("users/%d/trips/%d/documents/", userId, tripId);
        return upload(document, path, docsBucket);
    }

    private StorageUploadResult upload(MultipartFile file, String path, String bucket) {
        try {
            String extension = extractExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + extension;
            String fullPath = path + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fullPath)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            return new StorageUploadResult(fullPath, file.getOriginalFilename(), file.getSize(), file.getContentType());
        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", e.getMessage());
            throw new FileUploadException("파일 업로드 실패", e);
        }
    }

    @Override
    public void deleteTripCoverImage(String storageKey) {
        deleteByKey(imagesBucket, storageKey);
    }

    @Override
    public void deleteFootprintPhoto(String storageKey) {
        deleteByKey(imagesBucket, storageKey);
    }

    @Override
    public void deleteFootprintPhotos(List<String> storageKeys) {
        storageKeys.forEach(key -> deleteByKey(imagesBucket, key));
    }

    @Override
    public void deleteTripDocument(String storageKey) {
        deleteByKey(docsBucket, storageKey);
    }

    @Override
    public String getImageSignedUrl(String storageKey) {
        return generateSignedUrl(imagesBucket, storageKey);
    }

    @Override
    public String getDocumentSignedUrl(String storageKey) {
        return generateSignedUrl(docsBucket, storageKey);
    }

    private String generateSignedUrl(String bucket, String storageKey) {
        if (storageKey == null || storageKey.isEmpty()) {
            return null;
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(signedUrlExpirySeconds))
                .getObjectRequest(getObjectRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private void deleteByKey(String bucket, String storageKey) {
        if (storageKey == null || storageKey.isEmpty()) {
            return;
        }
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            log.info("스토리지 파일 삭제 완료: {}", storageKey);
        } catch (Exception e) {
            log.error("파일 삭제 실패: {}", e.getMessage());
            throw new FileDeleteException("파일 삭제 실패", e);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot) : "";
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
