package io.mero.app.domain.user.controller;

import io.mero.app.domain.user.dto.NicknameChangeRequest;
import io.mero.app.domain.user.dto.PasswordChangeRequest;
import io.mero.app.domain.user.dto.UserResponse;
import io.mero.app.domain.user.service.UserService;
import io.mero.app.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 정보를 조회합니다")
    @GetMapping
    public ResponseEntity<UserResponse> getMe() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(userService.getMe(userId));
    }

    @Operation(summary = "닉네임 변경", description = "닉네임을 변경합니다")
    @PatchMapping("/nickname")
    public ResponseEntity<Void> changeNickname(@Valid @RequestBody NicknameChangeRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.changeNickname(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인 후 새 비밀번호로 변경합니다")
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }
}
