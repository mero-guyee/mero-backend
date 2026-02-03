package io.mero.app.domain.budget.entity;

import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.global.entity.BaseEntity;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "budgets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Currency currency;

    @Column(name = "client_id", length = 36, unique = true)
    private String clientId;

    @Column(name = "exchange_rate", precision = 15, scale = 6)
    private BigDecimal exchangeRate;

    @Builder
    public Budget(Long id, Trip trip, BigDecimal amount, Currency currency, String clientId, BigDecimal exchangeRate) {
        validateAmount(amount);
        validateCurrency(currency);
        validateExchangeRate(exchangeRate);

        this.id = id;
        this.trip = trip;
        this.amount = amount;
        this.currency = currency;
        this.clientId = clientId;
        this.exchangeRate = exchangeRate;
    }

    public void update(BigDecimal amount, Currency currency, BigDecimal exchangeRate) {
        validateAmount(amount);
        validateCurrency(currency);
        validateExchangeRate(exchangeRate);

        this.amount = amount;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Budget amount must be greater than 0");
        }
    }

    private void validateCurrency(Currency currency) {
        if (currency == null) {
            throw new BadRequestException("Currency is required");
        }
    }

    private void validateExchangeRate(BigDecimal exchangeRate) {
        if (exchangeRate != null && exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Exchange rate must be greater than 0");
        }
    }
}
