package io.mero.app.domain.diary.controller;

import io.mero.app.domain.diary.dto.DiaryCreateRequest;
import io.mero.app.domain.diary.dto.DiaryResponse;
import io.mero.app.domain.diary.dto.DiaryUpdateRequest;
import io.mero.app.domain.diary.service.DiaryService;
import io.mero.app.global.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @PostMapping
    public ResponseEntity<DiaryResponse> createDiary(@Valid @RequestBody DiaryCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        DiaryResponse response = diaryService.createDiary(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(@PathVariable Long diaryId) {
        Long userId = SecurityUtil.getCurrentUserId();
        DiaryResponse response = diaryService.getDiary(userId, diaryId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> updateDiary(
            @PathVariable Long diaryId
            , @Valid @RequestBody DiaryUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        DiaryResponse response = diaryService.updateDiary(userId, diaryId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> deleteDiary(@PathVariable Long diaryId) {
        Long userId = SecurityUtil.getCurrentUserId();
        diaryService.deleteDiary(userId, diaryId);
        return ResponseEntity.noContent().build();
    }

}
