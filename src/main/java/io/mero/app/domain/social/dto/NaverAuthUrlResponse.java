package io.mero.app.domain.social.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NaverAuthUrlResponse {
    private String authUrl;
    private String state;
}
