package io.mero.app.global.controller;

import io.mero.app.global.dto.MetaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Meta", description = "공통 메타 데이터 API")
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    @Operation(summary = "메타 데이터 조회", description = "통화 및 타임존 목록을 반환합니다")
    @GetMapping
    public ResponseEntity<MetaResponse> getMeta() {
        return ResponseEntity.ok(new MetaResponse());
    }
}
