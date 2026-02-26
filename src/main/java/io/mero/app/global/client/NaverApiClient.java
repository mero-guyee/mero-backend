package io.mero.app.global.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.util.MessageUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverApiClient {

    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String PROFILE_URL = "https://openapi.naver.com/v1/nid/me";
    private static final String WRITE_POST_URL = "https://openapi.naver.com/blog/writePost.json";

    private final RestTemplate restTemplate;
    private final MessageUtil messageUtil;

    @Value("${naver.oauth.client-id}")
    private String clientId;

    @Value("${naver.oauth.client-secret}")
    private String clientSecret;

    @Value("${naver.oauth.redirect-uri}")
    private String redirectUri;

    /**
     * Authorization code를 access token으로 교환
     */
    public NaverTokenResponse exchangeCodeForTokens(String code, String state) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            params.add("code", code);
            params.add("state", state);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<NaverTokenResponse> response = restTemplate.exchange(
                    TOKEN_URL, HttpMethod.POST, request, NaverTokenResponse.class);

            NaverTokenResponse body = response.getBody();
            if (body == null || body.getAccessToken() == null) {
                log.error("Naver token exchange failed: {}", body);
                throw new BadRequestException(messageUtil.getMessage("error.social.tokenExchangeFailed"));
            }
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error exchanging Naver authorization code", e);
            throw new BadRequestException(messageUtil.getMessage("error.social.tokenExchangeFailed"));
        }
    }

    /**
     * Access token 갱신
     */
    public NaverTokenResponse refreshAccessToken(String refreshToken) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("refresh_token", refreshToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<NaverTokenResponse> response = restTemplate.exchange(
                    TOKEN_URL, HttpMethod.GET, request, NaverTokenResponse.class);

            NaverTokenResponse body = response.getBody();
            if (body == null || body.getAccessToken() == null) {
                log.error("Naver token refresh failed: {}", body);
                throw new BadRequestException(messageUtil.getMessage("error.social.tokenRefreshFailed"));
            }
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error refreshing Naver access token", e);
            throw new BadRequestException(messageUtil.getMessage("error.social.tokenRefreshFailed"));
        }
    }

    /**
     * 네이버 사용자 프로필 조회 (별명 포함)
     */
    public NaverProfileResponse getUserProfile(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<NaverProfileResponse> response = restTemplate.exchange(
                    PROFILE_URL, HttpMethod.GET, request, NaverProfileResponse.class);

            NaverProfileResponse body = response.getBody();
            if (body == null || body.getResponse() == null) {
                log.error("Naver profile fetch failed: {}", body);
                throw new BadRequestException(messageUtil.getMessage("error.social.profileFetchFailed"));
            }
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching Naver user profile", e);
            throw new BadRequestException(messageUtil.getMessage("error.social.profileFetchFailed"));
        }
    }

    /**
     * 네이버 블로그에 게시글 작성
     */
    public NaverWritePostResponse writePost(String accessToken, String title, String contents,
                                            Integer categoryNo, boolean allowComment, boolean isPublicPost) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + accessToken);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("title", title);
            body.add("contents", contents);
            body.add("allowComment", allowComment ? "1" : "0");
            body.add("isPublicPost", isPublicPost ? "1" : "0");
            if (categoryNo != null) {
                body.add("categoryNo", categoryNo.toString());
            }

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<NaverWritePostResponse> response = restTemplate.exchange(
                    WRITE_POST_URL, HttpMethod.POST, request, NaverWritePostResponse.class);

            NaverWritePostResponse responseBody = response.getBody();
            if (responseBody == null || responseBody.getResult() == null) {
                log.error("Naver blog write post failed: {}", responseBody);
                throw new BadRequestException(messageUtil.getMessage("error.social.publishFailed"));
            }
            return responseBody;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error writing Naver blog post", e);
            throw new BadRequestException(messageUtil.getMessage("error.social.publishFailed"));
        }
    }

    @Getter
    public static class NaverTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private Long expiresIn;

        private String error;

        @JsonProperty("error_description")
        private String errorDescription;
    }

    @Getter
    public static class NaverProfileResponse {
        private String resultcode;
        private String message;
        private NaverProfileDetail response;
    }

    @Getter
    public static class NaverProfileDetail {
        private String id;
        private String nickname;
        private String name;
        private String email;

        @JsonProperty("profile_image")
        private String profileImage;
    }

    @Getter
    public static class NaverWritePostResponse {
        private NaverWritePostResult result;
    }

    @Getter
    public static class NaverWritePostResult {
        @JsonProperty("postUrl")
        private String postUrl;
    }
}
