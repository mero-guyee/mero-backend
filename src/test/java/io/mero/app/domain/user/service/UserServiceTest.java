package io.mero.app.domain.user.service;

import io.mero.app.domain.expense.service.ExpenseCategoryService;
import io.mero.app.domain.user.dto.AppleLoginRequest;
import io.mero.app.domain.user.dto.GoogleLoginRequest;
import io.mero.app.domain.user.dto.LoginResponse;
import io.mero.app.domain.user.dto.NicknameChangeRequest;
import io.mero.app.domain.user.dto.TokenRefreshRequest;
import io.mero.app.domain.user.dto.TokenRefreshResponse;
import io.mero.app.domain.user.dto.UserResponse;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.UserRepository;
import io.mero.app.domain.user.service.AppleAuthService.AppleClaims;
import io.mero.app.domain.user.service.GoogleAuthService.GoogleClaims;
import io.mero.app.global.dto.StorageUploadResult;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.UnauthorizedException;
import io.mero.app.global.service.StorageCleaner;
import io.mero.app.global.service.StorageService;
import io.mero.app.global.util.TokenHasher;
import io.mero.app.global.jwt.JwtTokenProvider;
import io.mero.app.global.util.MessageUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExpenseCategoryService expenseCategoryService;

    @Mock
    private AppleAuthService appleAuthService;

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private StorageService storageService;

    @Mock
    private StorageCleaner storageCleaner;

    @Mock
    private MessageUtil messageUtil;

    @InjectMocks
    private UserService userService;

    // ===== 프로필 이미지 =====
    // 실제 삭제 시점(커밋 이후)은 StorageCleanerTest에서 검증한다.

    @Test
    @DisplayName("프로필 이미지 교체 - 새 키로 바뀌고 이전 파일 정리를 맡긴다")
    void 프로필_이미지_교체_성공() {
        // given
        long userId = 1L;
        String previousKey = "users/1/profile/old.jpg";
        String newKey = "users/1/profile/new.jpg";

        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .build();
        user.updateProfileImageKey(previousKey);

        MultipartFile image = new MockMultipartFile("image", "new.jpg", "image/jpeg", "content".getBytes());
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(storageService.uploadProfileImage(userId, image))
                .willReturn(new StorageUploadResult(newKey, "new.jpg", 7L, "image/jpeg"));
        given(storageService.getImageSignedUrl(newKey)).willReturn("https://signed/new.jpg");

        // when
        UserResponse response = userService.updateProfileImage(userId, image);

        // then
        assertThat(response.getProfileImage()).isEqualTo("https://signed/new.jpg");
        assertThat(user.getProfileImageKey()).isEqualTo(newKey);
        verify(storageCleaner).deleteProfileImage(previousKey);
    }

    @Test
    @DisplayName("프로필 이미지 삭제 - 키를 비우고 파일 정리를 맡긴다")
    void 프로필_이미지_삭제_성공() {
        // given
        long userId = 1L;
        String storageKey = "users/1/profile/old.jpg";

        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .build();
        user.updateProfileImageKey(storageKey);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.deleteProfileImage(userId);

        // then
        assertThat(user.getProfileImageKey()).isNull();
        verify(storageCleaner).deleteProfileImage(storageKey);
    }

    @Test
    @DisplayName("프로필 이미지 삭제 - 직접 올린 이미지가 없으면 아무것도 하지 않는다")
    void 프로필_이미지_삭제_이미지_없으면_무시() {
        // given
        long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .build();
        user.updateProfileImage("https://google/profile.jpg");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.deleteProfileImage(userId);

        // then - 소셜 이미지는 건드리지 않는다
        assertThat(user.getProfileImageUrl()).isEqualTo("https://google/profile.jpg");
        verify(storageCleaner, never()).deleteProfileImage(any());
    }

    // ===== 토큰 재발급 =====

    @Test
    @DisplayName("토큰 재발급 성공")
    void 토큰_재발급_성공() {
        // given
        String oldRefreshToken = "old-refresh-token";
        long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .nickname("테스트유저")
                .build();

        user.updateRefreshToken(TokenHasher.sha256(oldRefreshToken));

        given(jwtTokenProvider.validateRefreshToken(oldRefreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserIdFrom(oldRefreshToken)).willReturn(1L);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(userId)).willReturn("new-access-token");
        given(jwtTokenProvider.createRefreshToken(userId)).willReturn("new-refresh-token");

        TokenRefreshRequest request = new TokenRefreshRequest(oldRefreshToken);

        // when
        TokenRefreshResponse response = userService.refreshToken(request);

        //then
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(user.getRefreshToken()).isEqualTo(TokenHasher.sha256("new-refresh-token"));

        verify(jwtTokenProvider).validateRefreshToken(oldRefreshToken);
        verify(jwtTokenProvider).getUserIdFrom(oldRefreshToken);
        verify(userRepository).findById(userId);
        verify(jwtTokenProvider).createAccessToken(userId);
        verify(jwtTokenProvider).createRefreshToken(userId);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
    void 토큰_재발급_실패_유효하지_않은_토큰() {
        // given
        String invalidToken = "invalid-token";

        given(jwtTokenProvider.validateRefreshToken(invalidToken)).willReturn(false);
        given(messageUtil.getMessage("error.invalid.token")).willReturn("유효하지 않은 토큰입니다");

        TokenRefreshRequest request = new TokenRefreshRequest(invalidToken);

        // when & then
        assertThatThrownBy(() -> userService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("유효하지 않은 토큰입니다");

        verify(jwtTokenProvider).validateRefreshToken(invalidToken);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 기존 토큰과 불일치")
    void 토큰_재발급_실패_기존_토큰과_불일치() {
        // given
        String requestToken = "request-token";
        String savedToken = "saved-token";
        long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .nickname("테스트유저")
                .build();

        user.updateRefreshToken(savedToken);

        given(jwtTokenProvider.validateRefreshToken(requestToken)).willReturn(true);
        given(jwtTokenProvider.getUserIdFrom(requestToken)).willReturn(1L);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(messageUtil.getMessage("error.invalid.token")).willReturn("유효하지 않은 토큰입니다");

        TokenRefreshRequest request = new TokenRefreshRequest(requestToken);

        // when & then
        assertThatThrownBy(() -> userService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("유효하지 않은 토큰입니다");
    }

    // ===== 로그아웃 =====

    @Test
    @DisplayName("로그아웃 성공")
    void 로그아웃_성공() {
        // given
        long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .nickname("테스트유저")
                .build();

        user.updateRefreshToken("some-hash");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.logout(userId);

        // then
        assertThat(user.getRefreshToken()).isNull();
        verify(userRepository).findById(userId);
    }

    // ===== Apple 로그인 =====

    @Test
    @DisplayName("Apple 로그인 성공 - 기존 Apple 계정")
    void Apple_로그인_성공_기존_계정() {
        // given
        AppleLoginRequest request = new AppleLoginRequest();
        AppleClaims claims = new AppleClaims("apple-user-id-1", "apple@example.com");

        User user = User.builder()
                .id(1L)
                .email("apple@example.com")
                .nickname("user1a2b3c")
                .appleId("apple-user-id-1")
                .build();

        given(appleAuthService.validate(any())).willReturn(claims);
        given(userRepository.findByAppleId("apple-user-id-1")).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");

        // when
        LoginResponse response = userService.appleLogin(request);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.isNewUser()).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Apple 로그인 성공 - 신규 사용자 생성")
    void Apple_로그인_성공_신규_사용자() {
        // given
        AppleLoginRequest request = new AppleLoginRequest();
        AppleClaims claims = new AppleClaims("apple-user-id-new", "newapple@example.com");

        User newUser = User.builder()
                .id(2L)
                .email("newapple@example.com")
                .appleId("apple-user-id-new")
                .build();

        given(appleAuthService.validate(any())).willReturn(claims);
        given(userRepository.findByAppleId("apple-user-id-new")).willReturn(Optional.empty());
        given(userRepository.findByEmail("newapple@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(newUser);
        given(jwtTokenProvider.createAccessToken(2L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(2L)).willReturn("refresh-token");

        // when
        LoginResponse response = userService.appleLogin(request);

        // then
        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.getEmail()).isEqualTo("newapple@example.com");
        assertThat(response.getNickname()).isNull();
        assertThat(response.isNewUser()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    // ===== Google 로그인 =====

    @Test
    @DisplayName("Google 로그인 성공 - 기존 Google 계정")
    void Google_로그인_성공_기존_계정() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest();
        GoogleClaims claims = new GoogleClaims("google-user-id-1", "google@example.com",
                "https://lh3.googleusercontent.com/a/new");

        User user = User.builder()
                .id(10L)
                .email("google@example.com")
                .nickname("user-google-1")
                .googleId("google-user-id-1")
                .profileImageUrl("https://lh3.googleusercontent.com/a/old")
                .build();

        given(googleAuthService.validate(any())).willReturn(claims);
        given(userRepository.findByGoogleId("google-user-id-1")).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(10L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(10L)).willReturn("refresh-token");

        // when
        LoginResponse response = userService.googleLogin(request);

        // then
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("google@example.com");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(user.getRefreshToken()).isEqualTo(TokenHasher.sha256("refresh-token"));
        assertThat(user.getProfileImageUrl()).isEqualTo("https://lh3.googleusercontent.com/a/new");
        assertThat(response.getProfileImage()).isEqualTo("https://lh3.googleusercontent.com/a/new");
        assertThat(response.isNewUser()).isFalse();
        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Google 로그인 성공 - 동일 이메일 기존 계정에 googleId 연결")
    void Google_로그인_성공_동일_이메일_계정_연결() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest();
        GoogleClaims claims = new GoogleClaims("google-user-id-2", "existing@example.com",
                "https://lh3.googleusercontent.com/a/linked");

        User existing = User.builder()
                .id(20L)
                .email("existing@example.com")
                .nickname("기존유저")
                .build();

        given(googleAuthService.validate(any())).willReturn(claims);
        given(userRepository.findByGoogleId("google-user-id-2")).willReturn(Optional.empty());
        given(userRepository.findByEmail("existing@example.com")).willReturn(Optional.of(existing));
        given(jwtTokenProvider.createAccessToken(20L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(20L)).willReturn("refresh-token");

        // when
        LoginResponse response = userService.googleLogin(request);

        // then
        assertThat(response.getUserId()).isEqualTo(20L);
        assertThat(existing.getGoogleId()).isEqualTo("google-user-id-2");
        assertThat(existing.getProfileImageUrl()).isEqualTo("https://lh3.googleusercontent.com/a/linked");
        assertThat(response.isNewUser()).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Google 로그인 성공 - 신규 사용자 생성")
    void Google_로그인_성공_신규_사용자() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest();
        GoogleClaims claims = new GoogleClaims("google-user-id-new", "newgoogle@example.com",
                "https://lh3.googleusercontent.com/a/newuser");

        User newUser = User.builder()
                .id(30L)
                .email("newgoogle@example.com")
                .googleId("google-user-id-new")
                .build();

        given(googleAuthService.validate(any())).willReturn(claims);
        given(userRepository.findByGoogleId("google-user-id-new")).willReturn(Optional.empty());
        given(userRepository.findByEmail("newgoogle@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(newUser);
        given(jwtTokenProvider.createAccessToken(30L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(30L)).willReturn("refresh-token");

        // when
        LoginResponse response = userService.googleLogin(request);

        // then
        assertThat(response.getUserId()).isEqualTo(30L);
        assertThat(response.getEmail()).isEqualTo("newgoogle@example.com");
        assertThat(response.getNickname()).isNull();
        assertThat(response.getProfileImage()).isEqualTo("https://lh3.googleusercontent.com/a/newuser");
        assertThat(response.isNewUser()).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getProfileImageUrl())
                .isEqualTo("https://lh3.googleusercontent.com/a/newuser");
        verify(expenseCategoryService).createDefaultCategoriesForUser(newUser);
    }

    @Test
    @DisplayName("Google 로그인 실패 - 토큰 검증 실패")
    void Google_로그인_실패_토큰_검증_실패() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest();

        given(googleAuthService.validate(any()))
                .willThrow(new BadRequestException("Google 토큰 검증에 실패했습니다"));

        // when & then
        assertThatThrownBy(() -> userService.googleLogin(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Google 토큰 검증에 실패했습니다");

        verify(userRepository, never()).findByGoogleId(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // ===== 닉네임 변경 =====

    @Test
    @DisplayName("닉네임 변경 성공")
    void 닉네임_변경_성공() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("기존닉네임")
                .build();

        NicknameChangeRequest request = mock(NicknameChangeRequest.class);
        given(request.getNickname()).willReturn("새닉네임");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.changeNickname(userId, request);

        // then
        assertThat(user.getNickname()).isEqualTo("새닉네임");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("닉네임 변경 성공 - 다른 사용자가 쓰는 닉네임도 허용")
    void 닉네임_변경_성공_중복_허용() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("기존닉네임")
                .build();

        NicknameChangeRequest request = mock(NicknameChangeRequest.class);
        given(request.getNickname()).willReturn("중복닉네임");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.changeNickname(userId, request);

        // then
        assertThat(user.getNickname()).isEqualTo("중복닉네임");
    }
}
