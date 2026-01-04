package io.mero.app.domain.diary.controller;

import io.mero.app.domain.diary.dto.DiaryCreateRequest;
import io.mero.app.domain.diary.dto.DiaryResponse;
import io.mero.app.domain.diary.dto.DiaryUpdateRequest;
import io.mero.app.domain.diary.service.DiaryService;
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

@Tag(name = "Diary", description = "일기 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/trips/{tripId}/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(summary = "일기 생성", description = "새로운 여행 일기를 작성합니다")
    @PostMapping
    public ResponseEntity<DiaryResponse> createDiary(
            @PathVariable Long tripId,
            @Valid @RequestBody DiaryCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        DiaryResponse response = diaryService.createDiary(userId, tripId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "일기 목록 조회", description = "특정 여행의 일기 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<List<DiaryResponse>> getDiaries(@PathVariable Long tripId) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<DiaryResponse> response = diaryService.getDiaries(userId, tripId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "일기 상세 조회", description = "특정 일기의 상세 정보를 조회합니다 (연결된 경비 포함)")
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(
            @PathVariable Long tripId,
            @PathVariable Long diaryId) {
        Long userId = SecurityUtil.getCurrentUserId();
        DiaryResponse response = diaryService.getDiary(userId, tripId, diaryId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "일기 수정", description = "일기 내용을 수정합니다")
    @PutMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> updateDiary(
            @PathVariable Long tripId,
            @PathVariable Long diaryId,
            @Valid @RequestBody DiaryUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        DiaryResponse response = diaryService.updateDiary(userId, tripId, diaryId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "일기 삭제", description = "일기를 삭제합니다")
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @PathVariable Long tripId,
            @PathVariable Long diaryId) {
        Long userId = SecurityUtil.getCurrentUserId();
        diaryService.deleteDiary(userId, tripId, diaryId);
        return ResponseEntity.noContent().build();
    }
}