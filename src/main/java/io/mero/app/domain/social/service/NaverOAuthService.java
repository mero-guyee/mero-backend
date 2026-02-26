package io.mero.app.domain.social.service;

import io.mero.app.domain.social.dto.NaverAuthUrlResponse;
import io.mero.app.domain.social.dto.NaverConnectRequest;
import io.mero.app.domain.social.dto.NaverConnectResponse;
import io.mero.app.domain.social.dto.NaverStatusResponse;
import io.mero.app.domain.social.entity.SocialAccountLink;
import io.mero.app.domain.social.repository.SocialAccountLinkRepository;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.UserRepository;
import io.mero.app.global.client.NaverApiClient;
import io.mero.app.global.enums.SocialProvider;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NaverOAuthService {

    private static final String NAVER_AUTH_URL = "https://nid.naver.com/oauth2.0/authorize";
    private static final long STATE_TTL_MINUTES = 10;

    // key: "userId:state", value: 생성 시각
    private final ConcurrentHashMap<String, LocalDateTime> stateStore = new ConcurrentHashMap<>();

    private final SocialAccountLinkRepository socialAccountLinkRepository;
    private final UserRepository userRepository;
    private final NaverApiClient naverApiClient;
    private final MessageUtil messageUtil;

    @Value("${naver.oauth.client-id}")
    private String clientId;

    @Value("${naver.oauth.redirect-uri}")
    private String redirectUri;

    /**
     * Naver OAuth 인증 URL 및 state 생성
     */
    public NaverAuthUrlResponse getAuthUrl(Long userId) {
        String state = UUID.randomUUID().toString();
        String stateKey = userId + ":" + state;
        stateStore.put(stateKey, LocalDateTime.now());

        String authUrl = NAVER_AUTH_URL
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&state=" + state;

        return new NaverAuthUrlResponse(authUrl, state);
    }

    /**
     * Authorization code 교환 및 연동 완료
     */
    @Transactional
    public NaverConnectResponse connect(Long userId, NaverConnectRequest request) {
        validateState(userId, request.getState());

        NaverApiClient.NaverTokenResponse tokenResponse =
                naverApiClient.exchangeCodeForTokens(request.getCode(), request.getState());

        String naverNickname = naverApiClient.getUserProfile(tokenResponse.getAccessToken())
                .getResponse()
                .getNickname();

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(tokenResponse.getExpiresIn() != null ? tokenResponse.getExpiresIn() : 3600);
        LocalDateTime connectedAt = LocalDateTime.now();

        User user = findUserById(userId);

        Optional<SocialAccountLink> existing =
                socialAccountLinkRepository.findByUserIdAndProvider(userId, SocialProvider.NAVER);

        if (existing.isPresent()) {
            SocialAccountLink link = existing.get();
            link.updateTokens(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), expiresAt);
            link.updateNickname(naverNickname);
        } else {
            SocialAccountLink link = SocialAccountLink.builder()
                    .user(user)
                    .provider(SocialProvider.NAVER)
                    .accessToken(tokenResponse.getAccessToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .accessTokenExpiresAt(expiresAt)
                    .connectedAt(connectedAt)
                    .naverNickname(naverNickname)
                    .build();
            socialAccountLinkRepository.save(link);
        }

        return new NaverConnectResponse(true, connectedAt, naverNickname);
    }

    /**
     * 연동 상태 조회
     */
    public NaverStatusResponse getStatus(Long userId) {
        return socialAccountLinkRepository
                .findByUserIdAndProvider(userId, SocialProvider.NAVER)
                .map(NaverStatusResponse::from)
                .orElse(NaverStatusResponse.disconnected());
    }

    /**
     * 연동 해제
     */
    @Transactional
    public void disconnect(Long userId) {
        if (!socialAccountLinkRepository.existsByUserIdAndProvider(userId, SocialProvider.NAVER)) {
            throw new NotFoundException(messageUtil.getMessage("error.social.notConnected"));
        }
        socialAccountLinkRepository.deleteByUserIdAndProvider(userId, SocialProvider.NAVER);
    }

    private void validateState(Long userId, String state) {
        String stateKey = userId + ":" + state;
        LocalDateTime createdAt = stateStore.remove(stateKey);

        if (createdAt == null) {
            throw new BadRequestException(messageUtil.getMessage("error.social.invalidState"));
        }

        if (createdAt.plusMinutes(STATE_TTL_MINUTES).isBefore(LocalDateTime.now())) {
            throw new BadRequestException(messageUtil.getMessage("error.social.expiredState"));
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(messageUtil.getMessage("error.user.notFound")));
    }
}
