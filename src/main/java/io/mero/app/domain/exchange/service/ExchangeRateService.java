package io.mero.app.domain.exchange.service;

import io.mero.app.domain.exchange.dto.ExchangeRateResponse;
import io.mero.app.domain.exchange.entity.ExchangeRate;
import io.mero.app.domain.exchange.repository.ExchangeRateRepository;
import io.mero.app.domain.user.entity.User;
import io.mero.app.domain.user.repository.UserRepository;
import io.mero.app.global.client.ExchangeRateApiClient;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeRateService {
    private static final int EXCHANGE_RATE_SCALE = 6;

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateApiClient exchangeRateApiClient;
    private final UserRepository userRepository;
    private final MessageUtil messageUtil;

    /**
     * 특정 날짜의 환율 조회
     * ExchangeRate 기능 비활성화 - 항상 1:1 비율 반환
     */
    public BigDecimal getRate(Currency fromCurrency, Currency toCurrency, LocalDate date) {
        // 환율 기능 비활성화 - 모든 통화를 1:1로 처리
        return BigDecimal.ONE;
    }

    /**
     * USD 기준 환율 조회
     */
    private BigDecimal getUsdRate(Currency targetCurrency, LocalDate date) {
        return exchangeRateRepository
                .findByFromCurrencyAndToCurrencyAndDate(Currency.USD, targetCurrency, date)
                .map(ExchangeRate::getRate)
                .orElseGet(() -> findLatestUsdRate(targetCurrency, date));
    }

    /**
     * 가장 최근 USD 환율 찾기
     * ExchangeRate 기능 비활성화 - 사용하지 않음
     */
    private BigDecimal findLatestUsdRate(Currency targetCurrency, LocalDate date) {
        // 환율 기능 비활성화
        return BigDecimal.ONE;
    }

    /**
     * API에서 USD 기준 환율 가져오기
     */
    private BigDecimal fetchUsdRateFromApi(Currency targetCurrency) {
        try {
            ExchangeRateApiClient.ExchangeRateApiResponse response =
                    exchangeRateApiClient.fetchRates("USD");

            BigDecimal rate = response.getRates().get(targetCurrency.name());
            if (rate != null) {
                return rate;
            }

            throw new IllegalStateException(
                    "환율 데이터에 " + targetCurrency + " 정보가 없습니다");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch USD rate for {} from API", targetCurrency, e);
            throw new IllegalStateException(
                    messageUtil.getMessage("exchangeRate.api.unavailable", targetCurrency), e);
        }
    }

    /**
     * 사용자별 환율 표시 (User의 defaultCurrency 기준)
     */
    public List<ExchangeRateResponse> getRatesForUser(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageUtil.getMessage("error.user.notFound")));

        return getRatesForDisplay(user.getDefaultCurrency(), date);
    }

    /**
     * 기본 통화 기준 환율 표시
     */
    public List<ExchangeRateResponse> getRatesForDisplay(Currency baseCurrency, LocalDate date) {
        List<ExchangeRateResponse> displayRates = new ArrayList<>();

        for (Currency currency : Currency.values()) {
            if (currency == baseCurrency) {
                continue;
            }

            BigDecimal rate = getRate(currency, baseCurrency, date);
            BigDecimal baseAmount = getDisplayAmount(currency, baseCurrency);

            ExchangeRateResponse displayRate = ExchangeRateResponse.of(
                    currency,
                    baseCurrency,
                    baseAmount,
                    rate,
                    date
            );

            displayRates.add(displayRate);
        }

        return displayRates;
    }

    /**
     * 통화 조합에 따른 표시 단위
     */
    private BigDecimal getDisplayAmount(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency == toCurrency) {
            return BigDecimal.ONE;
        }

        // KRW → 주요 통화는 1000원 단위
        if (fromCurrency == Currency.KRW && isMajorCurrency(toCurrency)) {
            return new BigDecimal("1000");
        }

        // 소액 통화는 적절한 단위로
        if (isSmallDenomination(fromCurrency)) {
            return getSmallDenominationUnit(fromCurrency);
        }

        return BigDecimal.ONE;
    }

    private boolean isMajorCurrency(Currency currency) {
        return currency == Currency.USD
                || currency == Currency.EUR
                || currency == Currency.GBP;
    }

    private boolean isSmallDenomination(Currency currency) {
        return currency == Currency.JPY
                || currency == Currency.IDR
                || currency == Currency.VND
                || currency == Currency.KRW;
    }

    private BigDecimal getSmallDenominationUnit(Currency currency) {
        return switch (currency) {
            case JPY -> new BigDecimal("100");          // 100엔
            case IDR, VND -> new BigDecimal("10000");   // 10,000루피아/동
            case KRW -> new BigDecimal("1000");         // 1,000원
            default -> BigDecimal.ONE;
        };
    }


    /**
     * 매일 자정 환율 자동 업데이트 (USD 기준만)
     * ExchangeRate 기능 비활성화 - 스케줄러 비활성화
     */
    // @Scheduled(cron = "0 0 0 * * *")
    public void scheduledDailyUpdate() {
        log.info("Exchange rate scheduler is disabled");
        // updateDailyRates();
    }

    /**
     * 오늘 환율 업데이트 (USD 기준만 저장)
     */
    @Transactional
    public void updateDailyRates() {
        LocalDate today = LocalDate.now();

        if (exchangeRateRepository.existsByDate(today)) {
            log.info("Exchange rates for {} already exist, skipping", today);
            return;
        }

        try {
            // USD 기준으로만 가져오기
            ExchangeRateApiClient.ExchangeRateApiResponse response =
                    exchangeRateApiClient.fetchRates("USD");

            List<ExchangeRate> rates = new ArrayList<>();

            // USD → 다른 통화만 저장
            for (Currency targetCurrency : Currency.values()) {
                if (targetCurrency == Currency.USD) {
                    continue;
                }

                BigDecimal rate = response.getRates().get(targetCurrency.name());
                if (rate != null) {
                    ExchangeRate exchangeRate = ExchangeRate.builder()
                            .fromCurrency(Currency.USD)  // 항상 USD
                            .toCurrency(targetCurrency)
                            .rate(rate)
                            .date(today)
                            .build();
                    rates.add(exchangeRate);
                }
            }

            exchangeRateRepository.saveAll(rates);
            log.info("Successfully updated {} USD-based exchange rates for {}",
                    rates.size(), today);

        } catch (Exception e) {
            // 예외를 던지지 않고 로그만 (스케줄러 중단 방지)
            log.error("Failed to update exchange rates for {}, will retry tomorrow", today, e);
        }
    }


}