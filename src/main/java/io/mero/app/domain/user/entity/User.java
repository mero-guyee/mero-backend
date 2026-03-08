package io.mero.app.domain.user.entity;

import io.mero.app.global.entity.BaseEntity;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.enums.Timezone;
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

    @Column(nullable = false, unique = true, length = 10)
    private String nickname;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "apple_id", length = 255, unique = true)
    private String appleId;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_currency", length = 20)
    private Currency defaultCurrency;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Timezone timezone;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Builder
    public User(Long id, String email, String nickname, String passwordHash,
                String profileImageUrl, Currency defaultCurrency, Timezone timezone, String appleId) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.profileImageUrl = profileImageUrl;
        this.defaultCurrency = defaultCurrency != null ? defaultCurrency : Currency.KRW;
        this.timezone = timezone != null ? timezone : Timezone.ASIA_SEOUL;
        this.appleId = appleId;
        this.emailVerified = false;
    }

    public void linkAppleId(String appleId) {
        this.appleId = appleId;
    }

    public void verifyEmail() {
        this.emailVerified = true;
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

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void deleteProfileImage() {
        this.profileImageUrl = null;
    }

    public void updateDefaultCurrency(Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException("통화는 필수입니다");
        }
        this.defaultCurrency = currency;
    }

    public void updateTimezone(Timezone timezone) {
        if (timezone == null) {
            throw new IllegalArgumentException("타임존은 필수입니다");
        }
        this.timezone = timezone;
    }

    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
    }
}
