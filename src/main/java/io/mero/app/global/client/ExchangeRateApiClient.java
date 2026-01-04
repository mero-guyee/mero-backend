package io.mero.app.global.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.mero.app.global.util.MessageUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateApiClient {
    private static final String BASE_URL = "https://open.er-api.com/v6/latest/";
    private final RestTemplate restTemplate;
    private final MessageUtil messageUtil;

    /**
     * 특정 통화 기준 환율 조회
     */
    public ExchangeRateApiResponse fetchRates(String baseCurrency) {
        try {
            String url = BASE_URL + baseCurrency;
            log.info("Fetching exchange rates from: {}", url);

            ExchangeRateApiResponse response = restTemplate.getForObject(
                    url,
                    ExchangeRateApiResponse.class
            );

            if (response != null && "success".equals(response.getResult())) {
                log.info("Successfully fetched {} exchange rates", response.getRates().size());
                return response;
            } else {
                log.error("Failed to fetch exchange rates: {}", response);
                throw new RuntimeException(messageUtil.getMessage("exchangeRate.rate.fetchFailed"));
            }
        } catch (Exception e) {
            log.error("Error fetching exchange rates", e);
            throw new RuntimeException(messageUtil.getMessage("exchangeRate.rate.callFailed") + e.getMessage());
        }
    }

    /**
     * API 응답 DTO
     */
    @Getter
    public static class ExchangeRateApiResponse {
        private String result;

        @JsonProperty("base_code")
        private String baseCode;

        @JsonProperty("time_last_update_unix")
        private Long timeLastUpdateUnix;

        private Map<String, BigDecimal> rates;
    }

}
