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

        boolean isNewUser = false;
        User user = userRepository.findByAppleId(claims.appleUserId()).orElse(null);

        if (user == null) {
            if (claims.email() == null) {
                throw new BadRequestException(messageUtil.getMessage("error.apple.noEmail"));
            }
            user = userRepository.findByEmail(claims.email()).orElse(null);
            if (user != null) {
                user.linkAppleId(claims.appleUserId());
            } else {
                user = createAppleUser(claims);
                isNewUser = true;
            }
        }

        return issueLoginResponse(user, isNewUser);
    }

    private User createAppleUser(AppleClaims claims) {
        return saveNewUser(User.builder()
                .email(claims.email())
                .appleId(claims.appleUserId())
                .build());
    }

    @Transactional
    public LoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleClaims claims = googleAuthService.validate(request.getIdToken());

        boolean isNewUser = false;
        User user = userRepository.findByGoogleId(claims.googleUserId()).orElse(null);

        if (user == null) {
            user = userRepository.findByEmail(claims.email()).orElse(null);
            if (user != null) {
                user.linkGoogleId(claims.googleUserId());
            } else {
                user = createGoogleUser(claims);
                isNewUser = true;
            }
        }

        if (claims.picture() != null) {
            user.updateProfileImage(claims.picture());
        }

        return issueLoginResponse(user, isNewUser);
    }

    private User createGoogleUser(GoogleClaims claims) {
        return saveNewUser(User.builder()
                .email(claims.email())
                .googleId(claims.googleUserId())
                .profileImageUrl(claims.picture())
                .build());
    }

    /** 신규 가입자를 저장하고 기본 지출 카테고리를 함께 만들어 준다. */
    private User saveNewUser(User user) {
        User savedUser = userRepository.save(user);
        categoryService.createDefaultCategoriesForUser(savedUser);
        return savedUser;
    }

    /** 액세스/리프레시 토큰을 새로 발급하고 리프레시 토큰 해시를 저장한 뒤 응답을 만든다. */
    private LoginResponse issueLoginResponse(User user, boolean isNewUser) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        user.updateRefreshToken(TokenHasher.sha256(refreshToken));

        return new LoginResponse(user.getId(), user.getEmail(), user.getNickname(),
                user.getProfileImageUrl(), accessToken, refreshToken, isNewUser);
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
