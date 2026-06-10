package io.mero.app.global.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum DocumentMimeType {
    PDF("application/pdf", "pdf"),
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg");

    private final String mimeType;
    private final String extension;

    DocumentMimeType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public static DocumentMimeType fromMimeType(String mimeType) {
        return Arrays.stream(values())
                .filter(type -> type.getMimeType().equalsIgnoreCase(mimeType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 파일 타입입니다: " + mimeType + ". 허용된 타입: PDF, JPG, PNG"));
    }

    public static boolean isSupported(String mimeType) {
        return Arrays.stream(values())
                .anyMatch(type -> type.getMimeType().equalsIgnoreCase(mimeType));
    }
}
