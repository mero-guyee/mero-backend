package io.mero.app.global.dto;

import io.mero.app.global.enums.Currency;
import lombok.Getter;

@Getter
public class CurrencyResponse {

    private final String code;
    private final String displayName;
    private final String symbol;

    private CurrencyResponse(String code, String displayName, String symbol) {
        this.code = code;
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public static CurrencyResponse from(Currency currency) {
        return new CurrencyResponse(currency.getCode(), currency.getDisplayName(), currency.getSymbol());
    }
}
