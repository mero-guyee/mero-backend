package io.mero.app.domain.user.service;

import io.mero.app.domain.user.dto.LoginRequest;
import io.mero.app.domain.user.dto.LoginResponse;
import io.mero.app.domain.user.dto.SignUpRequest;
import io.mero.app.domain.user.dto.UserResponse;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.UserRepository;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.enums.Timezone;
import io.mero.app.global.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public UserResponse signUp(SignUpRequest request) {
        validateDuplicateEmail(request.getEmail());
        validateDuplicateNickname(request.getNickname());

        User user = createUser(request);
        User savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다");
        }
    }

    private User createUser(SignUpRequest request) {
        return User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .defaultCurrency(request.getDefaultCurrency() != null ?
                        request.getDefaultCurrency() : Currency.KRW)
                .timezone(request.getTimezone() != null ?
                        request.getTimezone() : Timezone.ASIA_SEOUL)
                .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                accessToken,
                refreshToken
        );
    }
}