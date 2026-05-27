package io.mero.app.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {

    @NotBlank(message = "{user.email.notBlank}")
    @Email(message = "{user.email.invalid}")
    private String email;

    @NotBlank(message = "{user.password.notBlank}")
    @Size(min = 8, message = "{user.password.size}")
    private String password;

    @NotBlank(message = "{user.nickname.notBlank}")
    @Size(min = 2, max = 10, message = "{user.nickname.size}")
    private String nickname;
}
