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

@Tag(name = "Diary", description = "일기 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(summary = "일기 생성", description = "새로운 여행 일기를 작성합니다")
    @PostMapping
    public ResponseEntity<DiaryResponse> createDiary(@Valid @RequestBody DiaryCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        DiaryResponse response = diaryService.createDiary(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "일기 상세 조회", description = "특정 일기의 상세 정보를 조회합니다 (연결된 경비 포함)")
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(@PathVariable Long diaryId) {
        Long userId = SecurityUtil.getCurrentUserId();
        DiaryResponse response = diaryService.getDiary(userId, diaryId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "일기 수정", description = "일기 내용을 수정합니다")
    @PutMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> updateDiary(
            @PathVariable Long diaryId
            , @Valid @RequestBody DiaryUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        DiaryResponse response = diaryService.updateDiary(userId, diaryId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "일기 삭제", description = "일기를 삭제합니다")
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> deleteDiary(@PathVariable Long diaryId) {
        Long userId = SecurityUtil.getCurrentUserId();
        diaryService.deleteDiary(userId, diaryId);
        return ResponseEntity.noContent().build();
    }

}
