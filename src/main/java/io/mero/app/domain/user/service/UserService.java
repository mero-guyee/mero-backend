package io.mero.app.domain.user.service;

import io.mero.app.domain.expense.service.ExpenseCategoryService;
import io.mero.app.domain.user.dto.*;
import io.mero.app.domain.user.service.AppleAuthService.AppleClaims;
import io.mero.app.domain.user.service.GoogleAuthService.GoogleClaims;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.UserRepository;
import io.mero.app.global.exception.BadRequestException;
import io.mero.app.global.exception.DuplicateException;
import io.mero.app.global.exception.NotFoundException;
import io.mero.app.global.exception.UnauthorizedException;
import io.mero.app.global.jwt.JwtTokenProvider;
import io.mero.app.global.util.MessageUtil;
import io.mero.app.global.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ExpenseCategoryService categoryService;
    private final AppleAuthService appleAuthService;
    private final GoogleAuthService googleAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MessageUtil messageUtil;

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
        user.updateRefreshToken(TokenHasher.sha256(refreshToken));

        return new LoginResponse(user.getId(), user.getEmail(), user.getNickname(),
                user.getProfileImageUrl(), accessToken, refreshToken);
    }

    private User createAppleUser(AppleClaims claims) {
        User user = User.builder()
                .email(claims.email())
                .appleId(claims.appleUserId())
                .build();
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

        if (claims.picture() != null) {
            user.updateProfileImage(claims.picture());
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        user.updateRefreshToken(TokenHasher.sha256(refreshToken));

        return new LoginResponse(user.getId(), user.getEmail(), user.getNickname(),
                user.getProfileImageUrl(), accessToken, refreshToken);
    }

    private User createGoogleUser(GoogleClaims claims) {
        User user = User.builder()
                .email(claims.email())
                .googleId(claims.googleUserId())
                .profileImageUrl(claims.picture())
                .build();
        User savedUser = userRepository.save(user);
        categoryService.createDefaultCategoriesForUser(savedUser);
        return savedUser;
    }

    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(messageUtil.getMessage("error.user.notFound")));
        return UserResponse.from(user);
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
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException(messageUtil.getMessage("error.invalid.token"));
        }

        Long userId = jwtTokenProvider.getUserIdFrom(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(messageUtil.getMessage("error.user.notFound")));

        if (!TokenHasher.sha256(refreshToken).equals(user.getRefreshToken())) {
            throw new UnauthorizedException(messageUtil.getMessage("error.invalid.token"));
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        user.updateRefreshToken(TokenHasher.sha256(newRefreshToken));

        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(messageUtil.getMessage("error.user.notFound")))
                .clearRefreshToken();
    }
}
