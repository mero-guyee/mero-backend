package io.mero.app.domain.exchange.controller;

import io.mero.app.domain.exchange.dto.ExchangeRateResponse;
import io.mero.app.domain.exchange.service.ExchangeRateService;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    /**
     * 환율 조회 (인증 불필요, baseCurrency 파라미터로)
     * GET /api/exchange-rates?baseCurrency=KRW
     * GET /api/exchange-rates?baseCurrency=USD
     */
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
    @PostMapping("/update")
    public ResponseEntity<String> updateRatesManually() {
        exchangeRateService.updateDailyRates();
        return ResponseEntity.ok("환율 업데이트 완료");
    }

}
