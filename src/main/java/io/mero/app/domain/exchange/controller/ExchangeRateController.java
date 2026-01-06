package io.mero.app.domain.exchange.controller;

import io.mero.app.domain.exchange.dto.ExchangeRateResponse;
import io.mero.app.domain.exchange.service.ExchangeRateService;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// ExchangeRate 기능 비활성화 - Controller 비활성화
// @Tag(name = "Exchange Rate", description = "환율 API")
// @RestController
// @RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    /**
     * 환율 조회 (인증 불필요, baseCurrency 파라미터로)
     * GET /api/exchange-rates?baseCurrency=KRW
     * GET /api/exchange-rates?baseCurrency=USD
     */
    @Operation(summary = "환율 조회", description = "특정 통화의 환율 정보를 조회합니다")
    @GetMapping
    public ResponseEntity<List<ExchangeRateResponse>> getRatesByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "KRW") Currency baseCurrency
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<ExchangeRateResponse> response =
                exchangeRateService.getRatesForDisplay(baseCurrency, targetDate);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 기본 통화 기준 환율 조회 (인증 필요)
     */
    @Operation(summary = "환율 조회", description = "내 기본 통화 기준 환율 정보를 조회합니다")
    @GetMapping("/me")
    public ResponseEntity<List<ExchangeRateResponse>> getMyRates(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<ExchangeRateResponse> response =
                exchangeRateService.getRatesForUser(userId, targetDate);
        return ResponseEntity.ok(response);
    }

    /**
     * 환율 수동 업데이트 (개발/테스트용)
     */
    @Operation(summary = "환율 수동 업데이트", description = "관리자용 환율 수동 업데이트 (ADMIN 권한 필요)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/update")
    public ResponseEntity<String> updateRatesManually() {
        exchangeRateService.updateDailyRates();
        return ResponseEntity.ok("환율 업데이트 완료");
    }

}
