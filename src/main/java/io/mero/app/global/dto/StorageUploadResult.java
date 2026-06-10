package io.mero.app.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StorageUploadResult {
    private String storageKey;
    private String originalFilename;
    private Long fileSize;
    private String mimeType;
}
