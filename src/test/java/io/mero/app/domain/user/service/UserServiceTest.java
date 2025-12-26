package io.mero.app.domain.user.service;

import io.mero.app.domain.user.dto.*;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.UserRepository;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.enums.Timezone;
import io.mero.app.global.exception.DuplicateException;
import io.mero.app.global.exception.ForbiddenException;
import io.mero.app.global.exception.UnauthorizedException;
import io.mero.app.global.jwt.JwtTokenProvider;
import io.mero.app.global.util.MessageUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

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
                "테스트유저",
                null,
                null
        );

        User savedUser = User.builder()
                .email(request.getEmail())
                .passwordHash(request.getPassword())
                .nickname(request.getNickname())
                .defaultCurrency(Currency.KRW)
                .timezone(Timezone.ASIA_SEOUL)
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        UserResponse response = userService.signUp(request);

        // then
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getNickname()).isEqualTo("테스트유저");
        assertThat(response.getDefaultCurrency()).isEqualTo(Currency.KRW);
        assertThat(response.getTimezone()).isEqualTo(Timezone.ASIA_SEOUL);

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void 회원가입_실패_이메일_중복() {
        // given
        SignUpRequest request = new SignUpRequest(
                "duplicate@example.com",
                "password123",
                "테스트유저",
                null,
                null
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
    @DisplayName("회원가입 - 기본값 설정")
    void 회원가입_기본값_설정() {
        // given
        SignUpRequest request = new SignUpRequest(
                "test@example.com",
                "password123",
                "테스트유저",
                null,  // defaultCurrency null
                null   // timezone null
        );

        User savedUser = User.builder()
                .email(request.getEmail())
                .passwordHash(request.getPassword())
                .nickname(request.getNickname())
                .defaultCurrency(Currency.KRW)
                .timezone(Timezone.ASIA_SEOUL)
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        UserResponse response = userService.signUp(request);

        // then
        assertThat(response.getDefaultCurrency()).isEqualTo(Currency.KRW);
        assertThat(response.getTimezone()).isEqualTo(Timezone.ASIA_SEOUL);
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void 회원가입_실패_닉네임_중복() {
        // given
        SignUpRequest request = new SignUpRequest(
                "test@example.com",
                "password123",
                "중복닉네임",
                null,
                null
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
                .defaultCurrency(Currency.KRW)
                .timezone(Timezone.ASIA_SEOUL)
                .build();

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
        assertThat(user.getRefreshToken()).isEqualTo("refresh-token");

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

        user.updateRefreshToken(oldRefreshToken);

        given(jwtTokenProvider.validateToken(oldRefreshToken)).willReturn(true);
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
        assertThat(user.getRefreshToken()).isEqualTo("new-refresh-token");

        verify(jwtTokenProvider).validateToken(oldRefreshToken);
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

        given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);
        given(messageUtil.getMessage("error.invalid.token")).willReturn("유효하지 않은 토큰입니다");

        TokenRefreshRequest request = new TokenRefreshRequest(invalidToken);

        // when & then
        assertThatThrownBy(() -> userService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("유효하지 않은 토큰입니다");

        verify(jwtTokenProvider).validateToken(invalidToken);
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

        given(jwtTokenProvider.validateToken(requestToken)).willReturn(true);
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
        String refreshToken = "valid-refresh-token";
        long userId = 1L;

        User user = User.builder()
                .id(userId)
                .email("test@email.com")
                .passwordHash("encodedPassword")
                .nickname("테스트유저")
                .build();

        user.updateRefreshToken(refreshToken);

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserIdFrom(refreshToken)).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        LogoutRequest request = new LogoutRequest(refreshToken);

        // when
        userService.logout(request);

        // then
        assertThat(user.getRefreshToken()).isNull();

        verify(jwtTokenProvider).validateToken(refreshToken);
        verify(jwtTokenProvider).getUserIdFrom(refreshToken);
        verify(userRepository).findById(userId);
    }
    
    @Test
    @DisplayName("로그아웃 실패 - 유효하지 않는 토큰")
    void 로그아웃_실패_유효하지_않는_토큰() {
        // given
        String invalidToken = "invalid-token";

        given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);
        given(messageUtil.getMessage("error.invalid.token")).willReturn("유효하지 않은 토큰입니다");

        LogoutRequest request = new LogoutRequest(invalidToken);
    
        // when & then
        assertThatThrownBy(() -> userService.logout(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("유효하지 않은 토큰입니다");

        verify(jwtTokenProvider).validateToken(invalidToken);
    }
}