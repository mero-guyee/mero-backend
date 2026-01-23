package io.mero.app.global.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ImageMimeType {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    WEBP("image/webp", "webp");

    private final String mimeType;
    private final String extension;

    ImageMimeType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public static ImageMimeType fromMimeType(String mimeType) {
        return Arrays.stream(values())
                .filter(type -> type.getMimeType().equalsIgnoreCase(mimeType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 이미지 타입입니다: " + mimeType + ". 허용된 타입: PNG, JPG, WEBP"));
    }

    public static boolean isSupported(String mimeType) {
        return Arrays.stream(values())
                .anyMatch(type -> type.getMimeType().equalsIgnoreCase(mimeType));
    }
}
