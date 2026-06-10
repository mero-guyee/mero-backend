package io.mero.app.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AppleLoginRequest {

    @NotBlank(message = "{apple.identityToken.notBlank}")
    private String identityToken;
}
