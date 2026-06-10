package io.mero.app.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "{google.idToken.notBlank}")
    private String idToken;
}
