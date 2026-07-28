package io.mero.app.domain.user.controller;

import io.mero.app.domain.user.dto.*;
import io.mero.app.domain.user.service.UserService;
import io.mero.app.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새로운 Access Token을 발급합니다")
    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponse> login(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = userService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃", description = "현재 사용자의 Refresh Token을 무효화합니다")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.logout(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Apple 로그인", description = "Apple Identity Token으로 로그인합니다. 신규 유저는 자동으로 계정이 생성되며 응답의 isNewUser가 true입니다.")
    @PostMapping("/apple")
    public ResponseEntity<LoginResponse> appleLogin(@Valid @RequestBody AppleLoginRequest request) {
        LoginResponse response = userService.appleLogin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Google 로그인", description = "Google ID Token으로 로그인합니다. 신규 유저는 자동으로 계정이 생성되며 응답의 isNewUser가 true입니다.")
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        LoginResponse response = userService.googleLogin(request);
        return ResponseEntity.ok(response);
    }

}