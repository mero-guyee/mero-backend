package io.mero.app.global.controller;

import io.mero.app.global.dto.CurrencyResponse;
import io.mero.app.global.enums.Currency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Tag(name = "Currency", description = "통화 API")
@RestController
@RequestMapping("/api/currencies")
public class CurrencyController {

    @Operation(summary = "통화 목록 조회", description = "지원하는 통화 목록을 반환합니다")
    @GetMapping
    public ResponseEntity<List<CurrencyResponse>> getCurrencies() {
        List<CurrencyResponse> currencies = Arrays.stream(Currency.values())
                .map(CurrencyResponse::from)
                .toList();
        return ResponseEntity.ok(currencies);
    }
}
