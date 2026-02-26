package io.mero.app.domain.social.dto;

import io.mero.app.domain.social.entity.SocialAccountLink;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NaverStatusResponse {
    private boolean connected;
    private LocalDateTime connectedAt;
    private String naverNickname;

    public static NaverStatusResponse from(SocialAccountLink link) {
        return new NaverStatusResponse(true, link.getConnectedAt(), link.getNaverNickname());
    }

    public static NaverStatusResponse disconnected() {
        return new NaverStatusResponse(false, null, null);
    }
}
