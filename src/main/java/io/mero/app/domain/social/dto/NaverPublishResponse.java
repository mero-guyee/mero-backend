package io.mero.app.domain.social.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NaverPublishResponse {
    private String postUrl;
    private LocalDateTime publishedAt;
}
