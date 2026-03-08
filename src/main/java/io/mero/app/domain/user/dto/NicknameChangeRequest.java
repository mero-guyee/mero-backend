package io.mero.app.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NicknameChangeRequest {

    @NotBlank(message = "{user.nickname.notBlank}")
    @Size(min = 2, max = 10, message = "{user.nickname.size}")
    private String nickname;
}
