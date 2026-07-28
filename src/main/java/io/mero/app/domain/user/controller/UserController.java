package io.mero.app.domain.user.controller;

import io.mero.app.domain.user.dto.NicknameChangeRequest;
import io.mero.app.domain.user.dto.UserResponse;
import io.mero.app.domain.user.service.UserService;
import io.mero.app.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @Operation(summary = "프로필 이미지 업로드/수정", description = "프로필 이미지를 업로드하거나 교체합니다. 이미지 파일만 가능하며 최대 10MB입니다.")
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadProfileImage(@RequestPart("image") MultipartFile image) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(userService.updateProfileImage(userId, image));
    }

    @Operation(summary = "프로필 이미지 삭제",
            description = "직접 올린 프로필 이미지를 삭제합니다. 소셜 로그인 프로필 이미지가 있으면 그 이미지로 돌아갑니다.")
    @DeleteMapping("/profile-image")
    public ResponseEntity<Void> deleteProfileImage() {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.deleteProfileImage(userId);
        return ResponseEntity.noContent().build();
    }
}
