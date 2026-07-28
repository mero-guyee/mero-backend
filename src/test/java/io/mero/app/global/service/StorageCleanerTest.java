package io.mero.app.global.service;

import io.mero.app.global.exception.FileDeleteException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StorageCleanerTest {

    @Mock
    private StorageService storageService;

    @InjectMocks
    private StorageCleaner storageCleaner;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void fireAfterCommit() {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
    }

    @Test
    @DisplayName("트랜잭션 안에서는 커밋 전까지 파일을 지우지 않는다")
    void 커밋_전에는_삭제하지_않는다() {
        // given
        String storageKey = "users/1/profile/old.jpg";
        TransactionSynchronizationManager.initSynchronization();

        // when
        storageCleaner.deleteProfileImage(storageKey);

        // then
        verify(storageService, never()).deleteProfileImage(storageKey);
    }

    @Test
    @DisplayName("커밋되면 그때 파일을 지운다")
    void 커밋되면_삭제한다() {
        // given
        String storageKey = "users/1/trips/cover/old.jpg";
        TransactionSynchronizationManager.initSynchronization();
        storageCleaner.deleteTripCoverImage(storageKey);

        // when
        fireAfterCommit();

        // then
        verify(storageService).deleteTripCoverImage(storageKey);
    }

    @Test
    @DisplayName("롤백되면(커밋 콜백이 실행되지 않으면) 파일은 그대로 남는다")
    void 롤백되면_파일은_유지된다() {
        // given
        String storageKey = "users/1/trips/1/documents/ticket.pdf";
        TransactionSynchronizationManager.initSynchronization();

        // when - 커밋 콜백을 실행하지 않는다
        storageCleaner.deleteTripDocument(storageKey);

        // then
        verify(storageService, never()).deleteTripDocument(storageKey);
    }

    @Test
    @DisplayName("트랜잭션 밖에서는 즉시 지운다")
    void 트랜잭션_밖에서는_즉시_삭제한다() {
        // given
        String storageKey = "users/1/profile/old.jpg";

        // when
        storageCleaner.deleteProfileImage(storageKey);

        // then
        verify(storageService).deleteProfileImage(storageKey);
    }

    @Test
    @DisplayName("삭제 실패해도 예외가 호출자에게 전파되지 않는다")
    void 삭제_실패해도_예외가_전파되지_않는다() {
        // given
        String storageKey = "users/1/profile/old.jpg";
        willThrow(new FileDeleteException("파일 삭제 실패", new RuntimeException()))
                .given(storageService).deleteProfileImage(storageKey);

        TransactionSynchronizationManager.initSynchronization();
        storageCleaner.deleteProfileImage(storageKey);

        // when & then - 이미 커밋된 뒤라 예외를 던져 봐야 롤백되지 않는다
        assertThatCode(this::fireAfterCommit).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("키가 비어 있으면 아무것도 하지 않는다")
    void 키가_비어있으면_아무것도_하지_않는다() {
        // when
        storageCleaner.deleteFootprintPhoto(null);
        storageCleaner.deleteFootprintPhoto("");

        // then
        verifyNoInteractions(storageService);
    }
}
