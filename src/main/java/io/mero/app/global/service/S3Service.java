package io.mero.app.global.service;

/**
 * S3 파일 관리 서비스 인터페이스
 */
public interface S3Service {

    /**
     * S3에서 파일 삭제
     * @param fileUrl 삭제할 파일의 URL
     */
    void deleteFile(String fileUrl);

    /**
     * S3에 파일 업로드
     * @param file 업로드할 파일
     * @return 업로드된 파일의 URL
     */
    // String uploadFile(MultipartFile file);
}
