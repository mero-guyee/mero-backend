package io.mero.app.domain.user.dto;

import io.mero.app.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String nickname;
    private String profileImage;
    private LocalDateTime createdAt;

    public static UserResponse from(User user, String profileImage) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                profileImage,
                user.getCreatedAt()
        );
    }
}
