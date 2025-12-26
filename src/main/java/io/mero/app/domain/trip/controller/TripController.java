package io.mero.app.domain.trip.controller;

import io.mero.app.domain.diary.dto.DiaryResponse;
import io.mero.app.domain.diary.service.DiaryService;
import io.mero.app.domain.expense.dto.ExpenseResponse;
import io.mero.app.domain.expense.service.ExpenseService;
import io.mero.app.domain.trip.dto.TripCreateRequest;
import io.mero.app.domain.trip.dto.TripResponse;
import io.mero.app.domain.trip.dto.TripUpdateRequest;
import io.mero.app.domain.trip.service.TripService;
import io.mero.app.global.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final DiaryService diaryService;
    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody TripCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        TripResponse response = tripService.createTrip(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TripResponse>> getTrips() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<TripResponse> responses = tripService.getTrips(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<TripResponse> getTrip(@PathVariable Long tripId) {
        Long userId = SecurityUtil.getCurrentUserId();
        TripResponse response = tripService.getTrip(userId, tripId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tripId}")
    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable Long tripId,
            @Valid @RequestBody TripUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        TripResponse response = tripService.updateTrip(userId, tripId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long tripId) {
        Long userId = SecurityUtil.getCurrentUserId();
        tripService.deleteTrip(userId, tripId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tripId}/diaries")
    public ResponseEntity<List<DiaryResponse>> getDiariesByTrip(@PathVariable Long tripId) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<DiaryResponse> responses = diaryService.getDiariesByTrip(userId, tripId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{tripId}/expenses")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByTrip(@PathVariable Long tripId) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<ExpenseResponse> responses = expenseService.getExpensesByTrip(userId, tripId);
        return ResponseEntity.ok(responses);
    }

}
