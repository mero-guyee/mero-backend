package io.mero.app.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class S3UploadResult {
    private String s3Key;
    private String s3Url;
    private String originalFilename;
    private Long fileSize;
    private String mimeType;
}
