package io.mero.app.domain.footprint.listener;

import io.mero.app.domain.footprint.entity.Photo;
import io.mero.app.global.service.S3Service;
import jakarta.persistence.PreRemove;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Photo가 삭제될 때 S3에 있는 실제 이미지 파일도 함께 삭제
 */
@Slf4j
@Component
public class PhotoEntityListener {

    private static S3Service s3Service;

    @Autowired
    public void setS3Service(S3Service s3Service) {
        PhotoEntityListener.s3Service = s3Service;
    }

    /**
     * Photo 엔티티가 삭제되기 전에 호출되는 콜백
     * S3에 있는 실제 이미지 파일을 삭제
     * @param photo 삭제될 Photo 엔티티
     */
    @PreRemove
    public void preRemove(Photo photo) {
        try {
            if (photo.getS3Key() != null && !photo.getS3Key().isEmpty()) {
                log.info("Deleting S3 file for photo: {}, s3Key: {}", photo.getId(), photo.getS3Key());

                if (s3Service != null) {
                    s3Service.deleteFootprintPhoto(photo.getS3Key());
                    log.info("Successfully deleted S3 file: {}", photo.getS3Key());
                } else {
                    log.warn("S3Service is not available. Skipping S3 file deletion for: {}", photo.getS3Key());
                }
            }
        } catch (Exception e) {
            log.error("Failed to delete S3 file: {} - {}", photo.getS3Key(), e.getMessage(), e);
            // S3 파일 삭제 실패 시에도 DB 레코드는 삭제되도록 예외를 던지지 않음
        }
    }
}
