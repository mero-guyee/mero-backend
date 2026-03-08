package io.mero.app.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordChangeRequest {

    @NotBlank(message = "{user.password.notBlank}")
    private String currentPassword;

    @NotBlank(message = "{user.password.notBlank}")
    @Size(min = 8, message = "{user.password.size}")
    private String newPassword;
}
