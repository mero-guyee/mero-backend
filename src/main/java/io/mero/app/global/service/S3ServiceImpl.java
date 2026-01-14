package io.mero.app.global.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * S3 파일 관리 서비스 구현체
 * TODO: 실제 AWS S3 연동 구현 필요
 */
@Slf4j
@Service
public class S3ServiceImpl implements S3Service {

    @Override
    public void deleteFile(String fileUrl) {
        // TODO: 실제 S3 파일 삭제 로직 구현
        log.info("S3 file deletion requested: {}", fileUrl);
        log.warn("S3 deletion not implemented yet. File URL: {}", fileUrl);
    }
}
