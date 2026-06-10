package io.mero.app.domain.footprint.entity;

public enum UploadStatus {

    PENDING("대기 중"),
    UPLOADING("업로드 중"),
    COMPLETED("완료"),
    FAILED("실패");

    private final String description;

    UploadStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
