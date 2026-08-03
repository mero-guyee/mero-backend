package io.mero.app.domain.footprint.listener;

import io.mero.app.domain.footprint.entity.Photo;
import io.mero.app.global.service.StorageCleaner;
import jakarta.persistence.PreRemove;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PhotoEntityListener {

    private static StorageCleaner storageCleaner;

    @Autowired
    public void setStorageCleaner(StorageCleaner storageCleaner) {
        PhotoEntityListener.storageCleaner = storageCleaner;
    }

    /**
     * 스토리지 파일 삭제는 StorageCleaner가 커밋 이후로 미룬다.
     * 여기서 바로 지우면 이후 롤백됐을 때 사진 행은 살아있는데 파일만 사라진다.
     */
    @PreRemove
    public void preRemove(Photo photo) {
        if (storageCleaner == null) {
            log.warn("StorageCleaner를 사용할 수 없어 파일 삭제를 건너뜁니다: {}", photo.getStorageKey());
            return;
        }
        storageCleaner.deleteFootprintPhoto(photo.getStorageKey());
    }
}
