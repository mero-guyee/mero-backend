package io.mero.app.domain.social.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NaverConnectResponse {
    private boolean connected;
    private LocalDateTime connectedAt;
    private String naverNickname;
}
