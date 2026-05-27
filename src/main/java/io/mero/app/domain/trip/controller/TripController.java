package io.mero.app.domain.trip.controller;

import io.mero.app.domain.trip.dto.TripCreateRequest;
import io.mero.app.domain.trip.dto.TripDetailResponse;
import io.mero.app.domain.trip.dto.TripDocumentResponse;
import io.mero.app.domain.trip.dto.TripMemoCreateRequest;
import io.mero.app.domain.trip.dto.TripMemoResponse;
import io.mero.app.domain.trip.dto.TripMemoUpdateRequest;
import io.mero.app.domain.trip.dto.TripResponse;
import io.mero.app.domain.trip.dto.TripUpdateRequest;
import io.mero.app.domain.trip.service.TripService;
import io.mero.app.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

@Tag(name = "Trip", description = "여행 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@Validated
public class TripController {

    private final TripService tripService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Operation(summary = "여행 생성", description = "새로운 여행을 생성합니다")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TripResponse> createTrip(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        TripCreateRequest request;
        try {
            request = objectMapper.readValue(dataJson, TripCreateRequest.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        Set<ConstraintViolation<TripCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = SecurityUtil.getCurrentUserId();
        TripResponse response = tripService.createTrip(userId, request, image);
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
    public ResponseEntity<TripDetailResponse> getTrip(@PathVariable Long tripId) {
        Long userId = SecurityUtil.getCurrentUserId();
        TripDetailResponse response = tripService.getTrip(userId, tripId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "여행 수정", description = "여행 정보를 수정합니다")
    @PutMapping("/{tripId}")
    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable Long tripId,
            @RequestBody @Valid TripUpdateRequest request
    ) {
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

    @Operation(summary = "여행 대표 이미지 업로드/수정", description = "여행의 대표 이미지를 업로드하거나 수정합니다")
    @PostMapping(value = "/{tripId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TripResponse> uploadTripImage(
            @PathVariable Long tripId,
            @RequestPart("image") MultipartFile image
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        TripResponse response = tripService.updateTripImage(userId, tripId, image);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "여행 대표 이미지 삭제", description = "여행의 대표 이미지를 삭제합니다")
    @DeleteMapping("/{tripId}/image")
    public ResponseEntity<Void> deleteTripImage(@PathVariable Long tripId) {
        Long userId = SecurityUtil.getCurrentUserId();
        tripService.deleteTripImage(userId, tripId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "여행 문서 목록 조회", description = "여행의 문서 목록을 조회합니다")
    @GetMapping("/{tripId}/documents")
    public ResponseEntity<List<TripDocumentResponse>> getTripDocuments(@PathVariable Long tripId) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<TripDocumentResponse> responses = tripService.getTripDocuments(userId, tripId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "여행 문서 업로드", description = "여행에 문서를 업로드합니다. clientId 기반 멱등 처리.")
    @PostMapping(value = "/{tripId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TripDocumentResponse> uploadTripDocument(
            @PathVariable Long tripId,
            @RequestParam("clientId")
            @NotBlank(message = "{tripDocument.clientId.notBlank}")
            @Size(max = 36, message = "{tripDocument.clientId.size}")
            String clientId,
            @RequestPart("file") MultipartFile file
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        TripDocumentResponse response = tripService.uploadTripDocument(userId, tripId, clientId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "여행 문서 삭제", description = "여행의 문서를 삭제합니다")
    @DeleteMapping("/{tripId}/documents/{documentId}")
    public ResponseEntity<Void> deleteTripDocument(
            @PathVariable Long tripId,
            @PathVariable Long documentId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        tripService.deleteTripDocument(userId, tripId, documentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "여행 메모 생성", description = "여행에 메모를 생성합니다")
    @PostMapping("/{tripId}/memos")
    public ResponseEntity<TripMemoResponse> createTripMemo(
            @PathVariable Long tripId,
            @RequestBody @Valid TripMemoCreateRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        TripMemoResponse response = tripService.createTripMemo(userId, tripId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "여행 메모 목록 조회", description = "여행의 모든 메모를 조회합니다")
    @GetMapping("/{tripId}/memos")
    public ResponseEntity<List<TripMemoResponse>> getTripMemos(@PathVariable Long tripId) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<TripMemoResponse> responses = tripService.getTripMemos(userId, tripId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "여행 메모 상세 조회", description = "특정 메모의 상세 정보를 조회합니다")
    @GetMapping("/{tripId}/memos/{memoId}")
    public ResponseEntity<TripMemoResponse> getTripMemo(
            @PathVariable Long tripId,
            @PathVariable Long memoId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        TripMemoResponse response = tripService.getTripMemo(userId, tripId, memoId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "여행 메모 수정", description = "메모를 수정합니다")
    @PutMapping("/{tripId}/memos/{memoId}")
    public ResponseEntity<TripMemoResponse> updateTripMemo(
            @PathVariable Long tripId,
            @PathVariable Long memoId,
            @RequestBody @Valid TripMemoUpdateRequest request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        TripMemoResponse response = tripService.updateTripMemo(userId, tripId, memoId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "여행 메모 삭제", description = "메모를 삭제합니다")
    @DeleteMapping("/{tripId}/memos/{memoId}")
    public ResponseEntity<Void> deleteTripMemo(
            @PathVariable Long tripId,
            @PathVariable Long memoId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        tripService.deleteTripMemo(userId, tripId, memoId);
        return ResponseEntity.noContent().build();
    }
}
