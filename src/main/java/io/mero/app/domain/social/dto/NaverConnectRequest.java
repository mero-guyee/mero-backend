package io.mero.app.domain.social.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NaverConnectRequest {

    @NotBlank(message = "{social.code.notBlank}")
    private String code;

    @NotBlank(message = "{social.state.notBlank}")
    private String state;
}
