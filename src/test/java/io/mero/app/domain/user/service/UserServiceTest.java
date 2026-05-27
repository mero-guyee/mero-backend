package io.mero.app.domain.user.service;

import io.mero.app.domain.expense.service.ExpenseCategoryService;
import io.mero.app.domain.user.dto.AppleLoginRequest;
import io.mero.app.domain.user.dto.GoogleLoginRequest;
import io.mero.app.domain.user.dto.LoginRequest;
import io.mero.app.domain.user.dto.LoginResponse;
import io.mero.app.domain.user.dto.NicknameChangeRequest;
import io.mero.app.domain.user.dto.PasswordChangeRequest;
import io.mero.app.domain.user.dto.PasswordResetConfirmRequest;
import io.mero.app.domain.user.dto.SignUpRequest;
import io.mero.app.domain.user.dto.TokenRefreshRequest;
import io.mero.app.domain.user.dto.TokenRefreshResponse;
import io.mero.app.domain.user.entity.EmailToken;
import io.mero.app.domain.user.entity.EmailTokenType;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.EmailTokenRepository;
import io.mero.app.domain.user.repository.UserRepository;
import io.mero.app.domain.user.service.AppleAuthService.AppleClaims;
import io.mero.app.domain.user.service.GoogleAuthService.GoogleClaims;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.DuplicateException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.exception.UnauthorizedException;
import io.mero.app.global.util.TokenHasher;
import io.mero.app.global.jwt.JwtTokenProvider;
import io.mero.app.global.util.MessageUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailTokenRepository emailTokenRepository;

    @Mock
    private ExpenseCategoryService expenseCategoryService;

    @Mock
    private EmailService emailService;

    @Mock
    private AppleAuthService appleAuthService;

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MessageUtil messageUtil;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 성공")
    void 회원가입_성공() {
        // given
        SignUpRequest request = new SignUpRequest(
                "test@example.com",
                "password123",
                "테스트유저"
        );

        User savedUser = User.builder()
                .email(request.getEmail())
                .passwordHash(request.getPassword())
                .nickname(request.getNickname())
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        userService.signUp(request);

        // then
        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(any(), any());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void 회원가입_실패_이메일_중복() {
        // given
        SignUpRequest request = new SignUpRequest(
                "duplicate@example.com",
                "password123",
                "테스트유저"
        );

        given(userRepository.existsByEmail(request.getEmail())).willReturn(true);
        given(messageUtil.getMessage("error.duplicate.email")).willReturn("이미 사용 중인 이메일입니다");

        // when & then
        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("이미 사용 중인 이메일입니다");

        verify(userRepository).existsByEmail(request.getEmail());
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void 회원가입_실패_닉네임_중복() {
        // given
        SignUpRequest request = new SignUpRequest(
                "test@example.com",
                "password123",
                "중복닉네임"
        );

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.existsByNickname(request.getNickname())).willReturn(true);
        given(messageUtil.getMessage("error.duplicate.nickname")).willReturn("이미 사용 중인 닉네임입니다");

        // when & then
        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("이미 사용 중인 닉네임입니다");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).existsByNickname(request.getNickname());
    }

    @Test
    @DisplayName("로그인 성공")
    void 로그인_성공() {
        // given
        LoginRequest request = new LoginRequest("test@email.com", "password123");

        User user = User.builder()
                .id(1L)
                .email("test@email.com")
                .passwordHash("encodedPassword")
                .nickname("테스트유저")
                .build();
        user.verifyEmail();

        given(userRepository.findByEmail(request.getEmail()))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .willReturn(true);
        given(jwtTokenProvider.createAccessToken(user.getId()))
                .willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(user.getId()))
                .willReturn("refresh-token");

        // when
        LoginResponse response = userService.login(request);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getNickname()).isEqualTo("테스트유저");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(user.getRefreshToken()).isEqualTo(TokenHasher.sha256("refresh-token"));

        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder).matches(request.getPassword(), user.getPasswordHash());
        verify(jwtTokenProvider).createRefreshToken(user.getId());
        verify(jwtTokenProvider).createRefreshToken(user.getId());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void 로그인_실패_존재하지_않는_이메일() {
        // given
        LoginRequest request = new LoginRequest("test@email.com", "password123");

        given(userRepository.findByEmail(request.getEmail()))
                .willReturn(Optional.empty());
        given(messageUtil.getMessage("error.invalid.login")).willReturn("이메일 또는 비밀번호가 일치하지 않습니다");

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("이메일 또는 비밀번호가 일치하지 않습니다");

        verify(userRepository).findByEmail(request.getEmail());

    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void 로그인_실패_비밀번호_불일치() {
        // given
        LoginRequest request = new LoginRequest("test@email.com", "password123");

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash("encodedPassword")
                .build();

        given(userRepository.findByEmail(request.getEmail()))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .willReturn(false);
        given(messageUtil.getMessage("error.invalid.login")).willReturn("이메일 또는 비밀번호가 일치하지 않습니다");

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("이메일 또는 비밀번호가 일치하지 않습니다");

        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder).matches(request.getPassword(), user.getPasswordHash());
    }
    
    @Test
    @DisplayName("토큰 재발급 성공")
    void 토큰_재발급_성공() {
        // given
        String oldRefreshToken = "old-refresh-token";
        long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .passwordHash("encodedPassword")
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
                .passwordHash("encodedPassword")
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
    
    @Test
    @DisplayName("로그아웃 성공")
    void 로그아웃_성공() {
        // given
        long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .passwordHash("encodedPassword")
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

    // ===== 이메일 인증 =====

    @Test
    @DisplayName("이메일 인증 성공")
    void 이메일_인증_성공() {
        // given
        String token = "valid-token";

        EmailToken emailToken = EmailToken.builder()
                .token(token)
                .email("test@example.com")
                .type(EmailTokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        User user = User.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .build();

        given(emailTokenRepository.findByTokenAndType(token, EmailTokenType.EMAIL_VERIFICATION))
                .willReturn(Optional.of(emailToken));
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        // when
        userService.verifyEmail(token);

        // then
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(emailToken.isUsed()).isTrue();
    }

    @Test
    @DisplayName("이메일 인증 실패 - 유효하지 않은 토큰")
    void 이메일_인증_실패_유효하지_않은_토큰() {
        // given
        given(emailTokenRepository.findByTokenAndType(anyString(), eq(EmailTokenType.EMAIL_VERIFICATION)))
                .willReturn(Optional.empty());
        given(messageUtil.getMessage("error.email.invalidToken")).willReturn("유효하지 않은 인증 토큰입니다");

        // when & then
        assertThatThrownBy(() -> userService.verifyEmail("invalid-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("유효하지 않은 인증 토큰입니다");
    }

    @Test
    @DisplayName("이메일 인증 실패 - 이미 사용된 토큰")
    void 이메일_인증_실패_이미_사용된_토큰() {
        // given
        EmailToken usedToken = EmailToken.builder()
                .token("used-token")
                .email("test@example.com")
                .type(EmailTokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        usedToken.use();

        given(emailTokenRepository.findByTokenAndType("used-token", EmailTokenType.EMAIL_VERIFICATION))
                .willReturn(Optional.of(usedToken));
        given(messageUtil.getMessage("error.email.alreadyVerified")).willReturn("이미 인증이 완료된 계정입니다");

        // when & then
        assertThatThrownBy(() -> userService.verifyEmail("used-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 인증이 완료된 계정입니다");
    }

    @Test
    @DisplayName("이메일 인증 실패 - 만료된 토큰")
    void 이메일_인증_실패_만료된_토큰() {
        // given
        EmailToken expiredToken = EmailToken.builder()
                .token("expired-token")
                .email("test@example.com")
                .type(EmailTokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        given(emailTokenRepository.findByTokenAndType("expired-token", EmailTokenType.EMAIL_VERIFICATION))
                .willReturn(Optional.of(expiredToken));
        given(messageUtil.getMessage("error.email.expiredToken")).willReturn("만료된 인증 토큰입니다");

        // when & then
        assertThatThrownBy(() -> userService.verifyEmail("expired-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("만료된 인증 토큰입니다");
    }

    // ===== 인증 메일 재발송 =====

    @Test
    @DisplayName("인증 메일 재발송 성공")
    void 인증_메일_재발송_성공() {
        // given
        String email = "test@example.com";

        User user = User.builder()
                .email(email)
                .nickname("테스트유저")
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        userService.resendVerificationEmail(email);

        // then
        verify(emailService).sendVerificationEmail(eq(email), anyString());
    }

    @Test
    @DisplayName("인증 메일 재발송 실패 - 이미 인증된 계정")
    void 인증_메일_재발송_실패_이미_인증된_계정() {
        // given
        String email = "verified@example.com";

        User user = User.builder()
                .email(email)
                .nickname("테스트유저")
                .build();
        user.verifyEmail();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(messageUtil.getMessage("error.email.alreadyVerified")).willReturn("이미 인증이 완료된 계정입니다");

        // when & then
        assertThatThrownBy(() -> userService.resendVerificationEmail(email))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 인증이 완료된 계정입니다");
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
        user.verifyEmail();

        given(appleAuthService.validate(any())).willReturn(claims);
        given(userRepository.findByAppleId("apple-user-id-1")).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");

        // when
        LoginResponse response = userService.appleLogin(request);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getAccessToken()).isEqualTo("access-token");
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
                .nickname("user1a2b3c")
                .appleId("apple-user-id-new")
                .build();
        newUser.verifyEmail();

        given(appleAuthService.validate(any())).willReturn(claims);
        given(userRepository.findByAppleId("apple-user-id-new")).willReturn(Optional.empty());
        given(userRepository.findByEmail("newapple@example.com")).willReturn(Optional.empty());
        given(userRepository.existsByNickname(anyString())).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(newUser);
        given(jwtTokenProvider.createAccessToken(2L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(2L)).willReturn("refresh-token");

        // when
        LoginResponse response = userService.appleLogin(request);

        // then
        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.getEmail()).isEqualTo("newapple@example.com");
        verify(userRepository).save(any(User.class));
    }

    // ===== Google 로그인 =====

    @Test
    @DisplayName("Google 로그인 성공 - 기존 Google 계정")
    void Google_로그인_성공_기존_계정() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest();
        GoogleClaims claims = new GoogleClaims("google-user-id-1", "google@example.com");

        User user = User.builder()
                .id(10L)
                .email("google@example.com")
                .nickname("user-google-1")
                .googleId("google-user-id-1")
                .build();
        user.verifyEmail();

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
        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Google 로그인 성공 - 동일 이메일 기존 계정에 googleId 연결")
    void Google_로그인_성공_동일_이메일_계정_연결() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest();
        GoogleClaims claims = new GoogleClaims("google-user-id-2", "existing@example.com");

        User existing = User.builder()
                .id(20L)
                .email("existing@example.com")
                .nickname("기존유저")
                .passwordHash("encodedPassword")
                .build();
        existing.verifyEmail();

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
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Google 로그인 성공 - 신규 사용자 생성")
    void Google_로그인_성공_신규_사용자() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest();
        GoogleClaims claims = new GoogleClaims("google-user-id-new", "newgoogle@example.com");

        User newUser = User.builder()
                .id(30L)
                .email("newgoogle@example.com")
                .nickname("user1a2b3c")
                .googleId("google-user-id-new")
                .build();
        newUser.verifyEmail();

        given(googleAuthService.validate(any())).willReturn(claims);
        given(userRepository.findByGoogleId("google-user-id-new")).willReturn(Optional.empty());
        given(userRepository.findByEmail("newgoogle@example.com")).willReturn(Optional.empty());
        given(userRepository.existsByNickname(anyString())).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(newUser);
        given(jwtTokenProvider.createAccessToken(30L)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(30L)).willReturn("refresh-token");

        // when
        LoginResponse response = userService.googleLogin(request);

        // then
        assertThat(response.getUserId()).isEqualTo(30L);
        assertThat(response.getEmail()).isEqualTo("newgoogle@example.com");
        assertThat(newUser.isEmailVerified()).isTrue();
        verify(userRepository).save(any(User.class));
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

        given(userRepository.existsByNickname("새닉네임")).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.changeNickname(userId, request);

        // then
        assertThat(user.getNickname()).isEqualTo("새닉네임");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("닉네임 변경 실패 - 닉네임 중복")
    void 닉네임_변경_실패_닉네임_중복() {
        // given
        Long userId = 1L;

        NicknameChangeRequest request = mock(NicknameChangeRequest.class);
        given(request.getNickname()).willReturn("중복닉네임");

        given(userRepository.existsByNickname("중복닉네임")).willReturn(true);
        given(messageUtil.getMessage("error.duplicate.nickname")).willReturn("이미 사용 중인 닉네임입니다");

        // when & then
        assertThatThrownBy(() -> userService.changeNickname(userId, request))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("이미 사용 중인 닉네임입니다");
    }

    // ===== 비밀번호 변경 =====

    @Test
    @DisplayName("비밀번호 변경 성공")
    void 비밀번호_변경_성공() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("테스트유저")
                .passwordHash("encodedOldPassword")
                .build();

        PasswordChangeRequest request = mock(PasswordChangeRequest.class);
        given(request.getCurrentPassword()).willReturn("oldPassword1");
        given(request.getNewPassword()).willReturn("newPassword1");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("oldPassword1", "encodedOldPassword")).willReturn(true);
        given(passwordEncoder.encode("newPassword1")).willReturn("encodedNewPassword");

        // when
        userService.changePassword(userId, request);

        // then
        assertThat(user.getPasswordHash()).isEqualTo("encodedNewPassword");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void 비밀번호_변경_실패_현재_비밀번호_불일치() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("테스트유저")
                .passwordHash("encodedOldPassword")
                .build();

        PasswordChangeRequest request = mock(PasswordChangeRequest.class);
        given(request.getCurrentPassword()).willReturn("wrongPassword");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPassword", "encodedOldPassword")).willReturn(false);
        given(messageUtil.getMessage("error.password.mismatch")).willReturn("현재 비밀번호가 일치하지 않습니다");

        // when & then
        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("현재 비밀번호가 일치하지 않습니다");
    }

    // ===== 비밀번호 재설정 =====

    @Test
    @DisplayName("비밀번호 재설정 요청 성공")
    void 비밀번호_재설정_요청_성공() {
        // given
        String email = "test@example.com";

        User user = User.builder()
                .email(email)
                .nickname("테스트유저")
                .build();

        io.mero.app.domain.user.dto.EmailRequest request = mock(io.mero.app.domain.user.dto.EmailRequest.class);
        given(request.getEmail()).willReturn(email);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        userService.requestPasswordReset(request);

        // then
        verify(emailService).sendPasswordResetEmail(eq(email), anyString());
    }

    @Test
    @DisplayName("비밀번호 재설정 성공")
    void 비밀번호_재설정_성공() {
        // given
        String resetToken = "valid-reset-token";

        EmailToken emailToken = EmailToken.builder()
                .token(resetToken)
                .email("test@example.com")
                .type(EmailTokenType.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        User user = User.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .passwordHash("oldHash")
                .build();

        PasswordResetConfirmRequest request = mock(PasswordResetConfirmRequest.class);
        given(request.getToken()).willReturn(resetToken);
        given(request.getNewPassword()).willReturn("newPassword1");

        given(emailTokenRepository.findByTokenAndType(eq(resetToken), eq(EmailTokenType.PASSWORD_RESET)))
                .willReturn(Optional.of(emailToken));
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newPassword1")).willReturn("newHash");

        // when
        userService.resetPassword(request);

        // then
        assertThat(emailToken.isUsed()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("newHash");
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 만료된 토큰")
    void 비밀번호_재설정_실패_만료된_토큰() {
        // given
        EmailToken expiredToken = EmailToken.builder()
                .token("expired-token")
                .email("test@example.com")
                .type(EmailTokenType.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        PasswordResetConfirmRequest request = mock(PasswordResetConfirmRequest.class);
        given(request.getToken()).willReturn("expired-token");

        given(emailTokenRepository.findByTokenAndType(eq("expired-token"), eq(EmailTokenType.PASSWORD_RESET)))
                .willReturn(Optional.of(expiredToken));
        given(messageUtil.getMessage("error.email.expiredToken")).willReturn("만료된 토큰입니다");

        // when & then
        assertThatThrownBy(() -> userService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("만료된 토큰입니다");
    }

    // ===== 로그인 실패 - 이메일 미인증 =====

    @Test
    @DisplayName("로그인 실패 - 이메일 미인증")
    void 로그인_실패_이메일_미인증() {
        // given
        LoginRequest request = new LoginRequest("test@email.com", "password123");

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash("encodedPassword")
                .nickname("테스트유저")
                .build();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).willReturn(true);
        given(messageUtil.getMessage("error.email.notVerified")).willReturn("이메일 인증이 필요합니다");

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("이메일 인증이 필요합니다");
    }
}