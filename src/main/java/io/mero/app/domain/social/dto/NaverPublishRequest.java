package io.mero.app.domain.social.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NaverPublishRequest {
    private Integer categoryNo;
    private boolean allowComment = true;
    private boolean isPublicPost = true;
}
