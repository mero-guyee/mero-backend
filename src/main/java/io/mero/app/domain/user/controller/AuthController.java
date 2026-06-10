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

    @Operation(summary = "회원가입", description = "입력한 이메일로 인증 메일을 발송합니다. 인증 완료 후 계정이 생성됩니다.")
    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@Valid @RequestBody SignUpRequest request) {
        userService.signUp(request);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

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

    @Operation(summary = "Apple 로그인", description = "Apple Identity Token으로 로그인합니다. 신규 유저는 자동으로 계정이 생성됩니다.")
    @PostMapping("/apple")
    public ResponseEntity<LoginResponse> appleLogin(@Valid @RequestBody AppleLoginRequest request) {
        LoginResponse response = userService.appleLogin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Google 로그인", description = "Google ID Token으로 로그인합니다. 신규 유저는 자동으로 계정이 생성됩니다.")
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        LoginResponse response = userService.googleLogin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이메일 인증", description = "이메일로 발송된 링크를 통해 계정을 인증합니다")
    @GetMapping("/email/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok("이메일 인증이 완료되었습니다. 앱으로 돌아가서 로그인해 주세요.");
    }

    @Operation(summary = "인증 메일 재발송", description = "이메일 인증 메일을 재발송합니다")
    @PostMapping("/email/resend")
    public ResponseEntity<Void> resendVerificationEmail(@Valid @RequestBody EmailRequest request) {
        userService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 재설정 요청", description = "입력한 이메일로 비밀번호 재설정 코드를 발송합니다")
    @PostMapping("/password/reset-request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        userService.requestPasswordReset(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 재설정", description = "이메일로 받은 코드로 비밀번호를 변경합니다")
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

}