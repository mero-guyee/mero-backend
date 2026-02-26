package io.mero.app.domain.social.controller;

import io.mero.app.domain.social.dto.NaverPublishRequest;
import io.mero.app.domain.social.dto.NaverPublishResponse;
import io.mero.app.domain.social.service.NaverPublishService;
import io.mero.app.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Social - Naver Publish", description = "네이버 블로그 게시 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/trips/{tripId}/footprints/{footprintId}/publish")
@RequiredArgsConstructor
public class NaverPublishController {

    private final NaverPublishService naverPublishService;

    @Operation(summary = "네이버 블로그 게시", description = "발자취 내용을 네이버 블로그에 게시합니다")
    @PostMapping("/naver")
    public ResponseEntity<NaverPublishResponse> publishToNaver(
            @PathVariable Long tripId,
            @PathVariable Long footprintId,
            @RequestBody(required = false) NaverPublishRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        NaverPublishRequest publishRequest = request != null ? request : new NaverPublishRequest();
        return ResponseEntity.ok(naverPublishService.publish(userId, tripId, footprintId, publishRequest));
    }
}
