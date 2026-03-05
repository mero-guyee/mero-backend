package io.mero.app.global.dto;

import io.mero.app.global.enums.Currency;
import io.mero.app.global.enums.Timezone;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class MetaResponse {

    private final List<CurrencyItem> currencies;
    private final List<TimezoneItem> timezones;

    public MetaResponse() {
        this.currencies = Arrays.stream(Currency.values())
                .map(CurrencyItem::from)
                .toList();
        this.timezones = Arrays.stream(Timezone.values())
                .map(TimezoneItem::from)
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

    @Getter
    public static class TimezoneItem {
        private final String zoneId;
        private final String displayName;
        private final String offset;

        private TimezoneItem(String zoneId, String displayName, String offset) {
            this.zoneId = zoneId;
            this.displayName = displayName;
            this.offset = offset;
        }

        public static TimezoneItem from(Timezone timezone) {
            return new TimezoneItem(timezone.getZoneId(), timezone.getDisplayName(), timezone.getOffset());
        }
    }
}
