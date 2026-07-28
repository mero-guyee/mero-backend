package io.mero.app.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private Long userId;
    private String email;
    private String nickname;
    private String profileImage;
    private String accessToken;
    private String refreshToken;

    /** 이번 로그인에서 계정이 새로 생성됐는지 여부. true면 클라이언트는 온보딩(닉네임 입력)으로 보낸다. */
    @JsonProperty("isNewUser")
    private boolean isNewUser;
}