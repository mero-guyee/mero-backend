package io.mero.app.global.dto;

import io.mero.app.global.enums.Currency;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class MetaResponse {

    private final List<CurrencyItem> currencies;

    public MetaResponse() {
        this.currencies = Arrays.stream(Currency.values())
                .map(CurrencyItem::from)
                .toList();
    }

    @Getter
    public static class CurrencyItem {
        private final String code;
        private final String displayName;
        private final String symbol;

        private CurrencyItem(String code, String displayName, String symbol) {
            this.code = code;
            this.displayName = displayName;
            this.symbol = symbol;
        }

        public static CurrencyItem from(Currency currency) {
            return new CurrencyItem(currency.getCode(), currency.getDisplayName(), currency.getSymbol());
        }
    }
}
