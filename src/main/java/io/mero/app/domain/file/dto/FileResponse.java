package io.mero.app.domain.file.dto;

import io.mero.app.domain.file.entity.File;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileResponse {
    private Long id;
    private String fileName;
    private String fileUrl;
    private Long fileSize;

    public static FileResponse from(File file) {
        return new FileResponse(
                file.getId(),
                file.getOriginalFileName(),
                file.getFileUrl(),
                file.getFileSize()
        );
    }
}

