package io.mero.app.domain.footprint.controller;

import io.mero.app.domain.footprint.dto.FootprintCreateRequest;
import io.mero.app.domain.footprint.dto.FootprintResponse;
import io.mero.app.domain.footprint.dto.FootprintUpdateRequest;
import io.mero.app.domain.footprint.service.FootprintService;
import io.mero.app.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Footprint", description = "여행 발자취 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/trips/{tripId}/footprints")
@RequiredArgsConstructor
public class FootprintController {

    private final FootprintService footprintService;

    @Operation(summary = "발자취 생성", description = "새로운 여행 발자취를 작성합니다")
    @PostMapping
    public ResponseEntity<FootprintResponse> createFootprint(
            @PathVariable Long tripId,
            @Valid @RequestBody FootprintCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        FootprintResponse response = footprintService.createFootprint(userId, tripId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "발자취 목록 조회", description = "특정 여행의 발자취 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<List<FootprintResponse>> getFootprints(@PathVariable Long tripId) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<FootprintResponse> response = footprintService.getFootprints(userId, tripId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "발자취 상세 조회", description = "특정 발자취의 상세 정보를 조회합니다 (연결된 경비 포함)")
    @GetMapping("/{footprintId}")
    public ResponseEntity<FootprintResponse> getFootprint(
            @PathVariable Long tripId,
            @PathVariable Long footprintId) {
        Long userId = SecurityUtil.getCurrentUserId();
        FootprintResponse response = footprintService.getFootprint(userId, tripId, footprintId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "발자취 수정", description = "발자취 내용을 수정합니다")
    @PutMapping("/{footprintId}")
    public ResponseEntity<FootprintResponse> updateFootprint(
            @PathVariable Long tripId,
            @PathVariable Long footprintId,
            @Valid @RequestBody FootprintUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        FootprintResponse response = footprintService.updateFootprint(userId, tripId, footprintId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "발자취 삭제", description = "발자취를 삭제합니다")
    @DeleteMapping("/{footprintId}")
    public ResponseEntity<Void> deleteFootprint(
            @PathVariable Long tripId,
            @PathVariable Long footprintId) {
        Long userId = SecurityUtil.getCurrentUserId();
        footprintService.deleteFootprint(userId, tripId, footprintId);
        return ResponseEntity.noContent().build();
    }
}
