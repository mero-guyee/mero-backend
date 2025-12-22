package io.mero.app.domain.exchange.dto;

import io.mero.app.domain.exchange.entity.ExchangeRate;
import io.mero.app.global.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ExchangeRateResponse {

    private Currency fromCurrency;     // 표시할 통화 (JPY, USD, EUR 등)
    private Currency toCurrency;       // 기준 통화 (KRW, USD 등)
    private BigDecimal baseAmount;     // 기준 금액
    private BigDecimal convertedAmount; // 변환된 금액
    private LocalDate date;
    private String displayText;

    public static ExchangeRateResponse of(Currency fromCurrency,
                                                 Currency toCurrency,
                                                 BigDecimal baseAmount,
                                                 BigDecimal rate,
                                                 LocalDate date) {
        BigDecimal convertedAmount = baseAmount.multiply(rate)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        String displayText = String.format("%.0f %s = %.2f %s",
                baseAmount,
                fromCurrency.name(),
                convertedAmount,
                toCurrency.name());

        return new ExchangeRateResponse(
                fromCurrency,
                toCurrency,
                baseAmount,
                convertedAmount,
                date,
                displayText
        );
    }
}
