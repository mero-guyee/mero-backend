package io.mero.app.domain.social.service;

import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.footprint.repository.FootprintRepository;
import io.mero.app.domain.social.dto.NaverPublishRequest;
import io.mero.app.domain.social.dto.NaverPublishResponse;
import io.mero.app.domain.social.entity.SocialAccountLink;
import io.mero.app.domain.social.repository.SocialAccountLinkRepository;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.repository.TripRepository;
import io.mero.app.global.client.NaverApiClient;
import io.mero.app.global.enums.SocialProvider;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.util.MessageUtil;
import io.mero.app.global.util.NaverBlogContentConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NaverPublishService {

    private final FootprintRepository footprintRepository;
    private final TripRepository tripRepository;
    private final SocialAccountLinkRepository socialAccountLinkRepository;
    private final NaverApiClient naverApiClient;
    private final NaverBlogContentConverter contentConverter;
    private final MessageUtil messageUtil;

    /**
     * 발자취를 네이버 블로그에 게시
     */
    @Transactional
    public NaverPublishResponse publish(Long userId, Long tripId, Long footprintId,
                                        NaverPublishRequest request) {
        Trip trip = findTripById(tripId);
        validateOwner(trip, userId);

        Footprint footprint = findFootprintById(footprintId);
        validateTripMatch(footprint.getTrip(), tripId);

        SocialAccountLink link = socialAccountLinkRepository
                .findByUserIdAndProvider(userId, SocialProvider.NAVER)
                .orElseThrow(() -> new BadRequestException(
                        messageUtil.getMessage("error.social.notConnected")));

        // 토큰 만료 임박 시 갱신
        if (link.isAccessTokenExpiringSoon()) {
            NaverApiClient.NaverTokenResponse refreshed =
                    naverApiClient.refreshAccessToken(link.getRefreshToken());
            LocalDateTime expiresAt = LocalDateTime.now()
                    .plusSeconds(refreshed.getExpiresIn() != null ? refreshed.getExpiresIn() : 3600);
            link.updateTokens(refreshed.getAccessToken(), refreshed.getRefreshToken(), expiresAt);
        }

        String title = footprint.getTitle() != null && !footprint.getTitle().isBlank()
                ? footprint.getTitle()
                : footprint.getDate().toString();
        String contents = contentConverter.convert(footprint);

        NaverApiClient.NaverWritePostResponse postResponse = naverApiClient.writePost(
                link.getAccessToken(),
                title,
                contents,
                request.getCategoryNo(),
                request.isAllowComment(),
                request.isPublicPost()
        );

        String postUrl = postResponse.getResult().getPostUrl();
        return new NaverPublishResponse(postUrl, LocalDateTime.now());
    }

    private Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.trip.notFound")));
    }

    private Footprint findFootprintById(Long footprintId) {
        return footprintRepository.findById(footprintId)
                .orElseThrow(() -> new NotFoundException(
                        messageUtil.getMessage("error.footprint.notFound")));
    }

    private void validateOwner(Trip trip, Long userId) {
        if (!trip.isOwner(userId)) {
            throw new ForbiddenException(messageUtil.getMessage("error.forbidden"));
        }
    }

    private void validateTripMatch(Trip trip, Long tripId) {
        if (!trip.getId().equals(tripId)) {
            throw new ForbiddenException(messageUtil.getMessage("error.forbidden"));
        }
    }
}
