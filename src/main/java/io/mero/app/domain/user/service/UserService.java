package io.mero.app.domain.user.service;

import io.mero.app.domain.expense.service.ExpenseCategoryService;
import io.mero.app.domain.user.dto.*;
import io.mero.app.domain.user.service.AppleAuthService.AppleClaims;
import io.mero.app.domain.user.service.GoogleAuthService.GoogleClaims;
import io.mero.app.domain.user.entity.EmailToken;
import io.mero.app.domain.user.entity.EmailTokenType;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.EmailTokenRepository;
import io.mero.app.domain.user.repository.UserRepository;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.enums.Timezone;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.DuplicateException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.exception.UnauthorizedException;
import io.mero.app.global.jwt.JwtTokenProvider;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final ExpenseCategoryService categoryService;
    private final EmailService emailService;
    private final AppleAuthService appleAuthService;
    private final GoogleAuthService googleAuthService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MessageUtil messageUtil;

    @Transactional
    public void signUp(SignUpRequest request) {
        validateDuplicateEmail(request.getEmail());
        validateDuplicateNickname(request.getNickname());

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .defaultCurrency(request.getDefaultCurrency() != null ? request.getDefaultCurrency() : Currency.KRW)
                .timezone(request.getTimezone() != null ? request.getTimezone() : Timezone.ASIA_SEOUL)
                .build();
        User savedUser = userRepository.save(user);
        categoryService.createDefaultCategoriesForUser(savedUser);

        String token = issueEmailToken(savedUser.getEmail(), EmailTokenType.EMAIL_VERIFICATION, 24);
        emailService.sendVerificationEmail(savedUser.getEmail(), token);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailToken emailToken = emailTokenRepository.findByTokenAndType(token, EmailTokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new BadRequestException(messageUtil.getMessage("error.email.invalidToken")));

        if (emailToken.isUsed()) {
            throw new BadRequestException(messageUtil.getMessage("error.email.alreadyVerified"));
        }
        if (emailToken.isExpired()) {
            throw new BadRequestException(messageUtil.getMessage("error.email.expiredToken"));
        }

        User user = userRepository.findByEmail(emailToken.getEmail())
                .orElseThrow(() -> new NotFoundException(messageUtil.getMessage("error.user.notFound")));

        user.verifyEmail();
        emailToken.use();
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(messageUtil.getMessage("error.user.notFound")));

        if (user.isEmailVerified()) {
            throw new BadRequestException(messageUtil.getMessage("error.email.alreadyVerified"));
        }

        String token = issueEmailToken(email, EmailTokenType.EMAIL_VERIFICATION, 24);
        emailService.sendVerificationEmail(email, token);
    }

    @Transactional
    public LoginResponse appleLogin(AppleLoginRequest request) {
        AppleClaims claims = appleAuthService.validate(request.getIdentityToken());

        User user = userRepository.findByAppleId(claims.appleUserId())
                .orElseGet(() -> {
                    if (claims.email() != null) {
                        return userRepository.findByEmail(claims.email())
                                .map(existing -> {
                                    existing.linkAppleId(claims.appleUserId());
                                    return existing;
                                })
                                .orElseGet(() -> createAppleUser(claims));
                    }
                    throw new BadRequestException(messageUtil.getMessage("error.apple.noEmail"));
                });

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        user.updateRefreshToken(refreshToken);

        return new LoginResponse(user.getId(), user.getEmail(), user.getNickname(), accessToken, refreshToken);
    }

    private User createAppleUser(AppleClaims claims) {
        String nickname = generateUniqueNickname();
        User user = User.builder()
                .email(claims.email())
                .nickname(nickname)
                .appleId(claims.appleUserId())
                .defaultCurrency(Currency.KRW)
                .timezone(Timezone.ASIA_SEOUL)
                .build();
        user.verifyEmail();
        User savedUser = userRepository.save(user);
        categoryService.createDefaultCategoriesForUser(savedUser);
        return savedUser;
    }

    @Transactional
    public LoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleClaims claims = googleAuthService.validate(request.getIdToken());

        User user = userRepository.findByGoogleId(claims.googleUserId())
                .orElseGet(() -> userRepository.findByEmail(claims.email())
                        .map(existing -> {
                            existing.linkGoogleId(claims.googleUserId());
                            return existing;
                        })
                        .orElseGet(() -> createGoogleUser(claims)));

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        user.updateRefreshToken(refreshToken);

        return new LoginResponse(user.getId(), user.getEmail(), user.getNickname(), accessToken, refreshToken);
    }

    private User createGoogleUser(GoogleClaims claims) {
        String nickname = generateUniqueNickname();
        User user = User.builder()
                .email(claims.email())
                .nickname(nickname)
                .googleId(claims.googleUserId())
                .defaultCurrency(Currency.KRW)
                .timezone(Timezone.ASIA_SEOUL)
                .build();
        user.verifyEmail();
        User savedUser = userRepository.save(user);
        categoryService.createDefaultCategoriesForUser(savedUser);
        return savedUser;
    }

    private String generateUniqueNickname() {
        String nickname;
        do {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
            nickname = "user" + suffix;
        } while (userRepository.existsByNickname(nickname));
        return nickname;
    }

    @Transactional
    public void changeNickname(Long userId, NicknameChangeRequest request) {
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new DuplicateException(messageUtil.getMessage("error.duplicate.nickname"));
        }
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(messageUtil.getMessage("error.user.notFound")))
                .updateNickname(request.getNickname());
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException(messageUtil.getMessage("error.invalid.login")));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException(messageUtil.getMessage("error.invalid.login"));
        }

        if (!user.isEmailVerified()) {
            throw new UnauthorizedException(messageUtil.getMessage("error.email.notVerified"));
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        user.updateRefreshToken(refreshToken);

        return new LoginResponse(user.getId(), user.getEmail(), user.getNickname(), accessToken, refreshToken);
    }

    @Transactional
    public void requestPasswordReset(EmailRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = issueEmailToken(user.getEmail(), EmailTokenType.PASSWORD_RESET, 1);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });
    }

    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request) {
        EmailToken emailToken = emailTokenRepository.findByTokenAndType(request.getToken(), EmailTokenType.PASSWORD_RESET)
                .orElseThrow(() -> new BadRequestException(messageUtil.getMessage("error.email.invalidToken")));

        if (emailToken.isUsed()) {
            throw new BadRequestException(messageUtil.getMessage("error.email.invalidToken"));
        }
        if (emailToken.isExpired()) {
            throw new BadRequestException(messageUtil.getMessage("error.email.expiredToken"));
        }

        User user = userRepository.findByEmail(emailToken.getEmail())
                .orElseThrow(() -> new NotFoundException(messageUtil.getMessage("error.user.notFound")));

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        emailToken.use();
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(messageUtil.getMessage("error.user.notFound")));

        if (user.getPasswordHash() == null) {
            throw new BadRequestException(messageUtil.getMessage("error.password.appleUser"));
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException(messageUtil.getMessage("error.password.mismatch"));
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException(messageUtil.getMessage("error.invalid.token"));
        }

        Long userId = jwtTokenProvider.getUserIdFrom(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(messageUtil.getMessage("error.user.notFound")));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new UnauthorizedException(messageUtil.getMessage("error.invalid.token"));
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        user.updateRefreshToken(newRefreshToken);

        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException(messageUtil.getMessage("error.invalid.token"));
        }

        Long userId = jwtTokenProvider.getUserIdFrom(refreshToken);
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(messageUtil.getMessage("error.user.notFound")))
                .clearRefreshToken();
    }

    private String issueEmailToken(String email, EmailTokenType type, int expiresInHours) {
        emailTokenRepository.deleteByEmailAndType(email, type);
        String token = UUID.randomUUID().toString();
        emailTokenRepository.save(EmailToken.builder()
                .token(token)
                .email(email)
                .type(type)
                .expiresAt(LocalDateTime.now().plusHours(expiresInHours))
                .build());
        return token;
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateException(messageUtil.getMessage("error.duplicate.email"));
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new DuplicateException(messageUtil.getMessage("error.duplicate.nickname"));
        }
    }
}
