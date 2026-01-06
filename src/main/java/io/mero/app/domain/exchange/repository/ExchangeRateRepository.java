package io.mero.app.domain.exchange.repository;

import io.mero.app.domain.exchange.entity.ExchangeRate;
import io.mero.app.global.enums.Currency;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByFromCurrencyAndToCurrencyAndDate(
            Currency fromCurrency, Currency toCurrency, LocalDate date);

    boolean existsByDate(LocalDate date);

    // 문제가 있는 메서드 제거 - ExchangeRate 기능 비활성화
    // Optional<ExchangeRate> findFirstByFromCurrencyAndToCurrencyAndDateLessThanEqualOrderByDateDesc(
    //         Currency fromCurrency, Currency toCurrency, LocalDate date);
}