package io.mero.app.domain.footprint.listener;

import io.mero.app.domain.footprint.entity.Photo;
import io.mero.app.global.service.StorageService;
import jakarta.persistence.PreRemove;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PhotoEntityListener {

    private static StorageService storageService;

    @Autowired
    public void setStorageService(StorageService storageService) {
        PhotoEntityListener.storageService = storageService;
    }

    @PreRemove
    public void preRemove(Photo photo) {
        try {
            if (photo.getS3Key() != null && !photo.getS3Key().isEmpty()) {
                if (storageService != null) {
                    storageService.deleteFootprintPhoto(photo.getS3Key());
                    log.info("스토리지 파일 삭제 완료 - photoId: {}, key: {}", photo.getId(), photo.getS3Key());
                } else {
                    log.warn("StorageService를 사용할 수 없어 파일 삭제를 건너뜁니다: {}", photo.getS3Key());
                }
            }
        } catch (Exception e) {
            log.error("스토리지 파일 삭제 실패 - key: {}, error: {}", photo.getS3Key(), e.getMessage(), e);
        }
    }
}
