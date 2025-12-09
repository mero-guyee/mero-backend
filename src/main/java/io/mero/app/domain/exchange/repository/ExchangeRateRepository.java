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

    // 가장 최근 USD 환율 조회 (LIMIT 1)
    @Query(value = "SELECT er FROM ExchangeRate er " +
            "WHERE er.fromCurrency = 'USD' " +  // ← USD 고정
            "AND er.toCurrency = :toCurrency " +
            "AND er.date <= :date " +
            "ORDER BY date DESC " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<ExchangeRate> findLatestRateBeforeDate(
            @Param("toCurrency") Currency toCurrency,  // fromCurrency 파라미터 제거
            @Param("date") LocalDate date);
}