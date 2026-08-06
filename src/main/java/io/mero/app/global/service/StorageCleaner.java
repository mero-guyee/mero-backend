package io.mero.app.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;

/**
 * 스토리지 파일 삭제를 트랜잭션 커밋 이후로 미룬다.
 * 커밋 전에 지우면 롤백됐을 때 DB는 살아있는 키를 가리키는데 파일은 이미 없어 이미지가 깨진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageCleaner {

    private final StorageService storageService;

    public void deleteProfileImage(String storageKey) {
        runAfterCommit(storageKey, storageService::deleteProfileImage);
    }

    public void deleteTripCoverImage(String storageKey) {
        runAfterCommit(storageKey, storageService::deleteTripCoverImage);
    }

    public void deleteTripDocument(String storageKey) {
        runAfterCommit(storageKey, storageService::deleteTripDocument);
    }

    public void deleteFootprintPhoto(String storageKey) {
        runAfterCommit(storageKey, storageService::deleteFootprintPhoto);
    }

    private void runAfterCommit(String storageKey, Consumer<String> deleteAction) {
        if (storageKey == null || storageKey.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(storageKey, deleteAction);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(storageKey, deleteAction);
            }
        });
    }

    /**
     * 삭제 실패는 고아 파일만 남길 뿐 DB 상태는 정상이다.
     * 커밋이 끝난 뒤라 예외를 던져 봐야 롤백도 안 되고 호출자에게만 전파되므로, 로그만 남긴다.
     */
    private void deleteQuietly(String storageKey, Consumer<String> deleteAction) {
        try {
            deleteAction.accept(storageKey);
        } catch (Exception e) {
            log.error("스토리지 파일 삭제 실패, 고아 파일로 남음: key={}", storageKey, e);
        }
    }
}
