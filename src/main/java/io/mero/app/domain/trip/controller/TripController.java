package io.mero.app.domain.trip.controller;

import io.mero.app.domain.diary.service.DiaryService;
import io.mero.app.domain.expense.service.ExpenseService;
import io.mero.app.domain.trip.dto.TripCreateRequest;
import io.mero.app.domain.trip.dto.TripResponse;
import io.mero.app.domain.trip.dto.TripUpdateRequest;
import io.mero.app.domain.trip.service.TripService;
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

@Tag(name = "Trip", description = "여행 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @Operation(summary = "여행 생성", description = "새로운 여행을 생성합니다")
    @PostMapping
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody TripCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        TripResponse response = tripService.createTrip(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "여행 목록 조회", description = "사용자의 모든 여행을 조회합니다")
    @GetMapping
    public ResponseEntity<List<TripResponse>> getTrips() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<TripResponse> responses = tripService.getTrips(userId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "여행 상세 조회", description = "특정 여행의 상세 정보를 조회합니다")
    @GetMapping("/{tripId}")
    public ResponseEntity<TripResponse> getTrip(@PathVariable Long tripId) {
        Long userId = SecurityUtil.getCurrentUserId();
        TripResponse response = tripService.getTrip(userId, tripId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "여행 수정", description = "여행 정보를 수정합니다")
    @PutMapping("/{tripId}")
    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable Long tripId,
            @Valid @RequestBody TripUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        TripResponse response = tripService.updateTrip(userId, tripId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "여행 삭제", description = "여행을 삭제합니다")
    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long tripId) {
        Long userId = SecurityUtil.getCurrentUserId();
        tripService.deleteTrip(userId, tripId);
        return ResponseEntity.noContent().build();
    }
}
