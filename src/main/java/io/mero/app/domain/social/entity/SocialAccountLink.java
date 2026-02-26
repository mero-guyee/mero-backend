package io.mero.app.domain.social.entity;

import io.mero.app.domain.user.entity.User;
import io.mero.app.global.entity.BaseEntity;
import io.mero.app.global.enums.SocialProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "social_account_links",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "provider"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccountLink extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "access_token", nullable = false, length = 1000)
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, length = 1000)
    private String refreshToken;

    @Column(name = "access_token_expires_at", nullable = false)
    private LocalDateTime accessTokenExpiresAt;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @Column(name = "naver_nickname", length = 100)
    private String naverNickname;

    @Builder
    public SocialAccountLink(User user, SocialProvider provider, String accessToken,
                             String refreshToken, LocalDateTime accessTokenExpiresAt,
                             LocalDateTime connectedAt, String naverNickname) {
        this.user = user;
        this.provider = provider;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.connectedAt = connectedAt;
        this.naverNickname = naverNickname;
    }

    public boolean isAccessTokenExpiringSoon() {
        return LocalDateTime.now().isAfter(accessTokenExpiresAt.minusMinutes(5));
    }

    public void updateTokens(String accessToken, String refreshToken, LocalDateTime expiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresAt = expiresAt;
    }

    public void updateNickname(String naverNickname) {
        this.naverNickname = naverNickname;
    }
}
