package io.mero.app.domain.user.entity;

import io.mero.app.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 10)
    private String nickname;

    @Column(name = "apple_id", length = 255, unique = true)
    private String appleId;

    @Column(name = "google_id", length = 255, unique = true)
    private String googleId;

    /** 소셜 로그인으로 받아온 프로필 이미지 URL */
    @Column(name = "profile_image_url", length = 500)
    private String socialProfileImageUrl;

    /** 사용자가 직접 업로드한 프로필 이미지의 스토리지 키 (있으면 소셜 이미지보다 우선) */
    @Column(name = "profile_image_key", length = 500)
    private String profileImageKey;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Builder
    public User(Long id, String email, String nickname,
                String socialProfileImageUrl, String appleId, String googleId) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.socialProfileImageUrl = socialProfileImageUrl;
        this.appleId = appleId;
        this.googleId = googleId;
    }

    public void linkAppleId(String appleId) {
        this.appleId = appleId;
    }

    public void linkGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다");
        }
        this.nickname = nickname;
    }

    public void updateSocialProfileImage(String socialProfileImageUrl) {
        this.socialProfileImageUrl = socialProfileImageUrl;
    }

    public void updateProfileImageKey(String profileImageKey) {
        this.profileImageKey = profileImageKey;
    }

    /** 직접 올린 이미지만 지운다. 소셜 이미지가 있으면 다시 그 이미지로 돌아간다. */
    public void deleteProfileImage() {
        this.profileImageKey = null;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
    }
}
