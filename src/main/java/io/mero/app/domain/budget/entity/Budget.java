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

    @Builder
    public Budget(Long id, Trip trip, BigDecimal amount, Currency currency) {
        validateAmount(amount);
        validateCurrency(currency);

        this.id = id;
        this.trip = trip;
        this.amount = amount;
        this.currency = currency;
    }

    public void update(BigDecimal amount, Currency currency) {
        validateAmount(amount);
        validateCurrency(currency);

        this.amount = amount;
        this.currency = currency;
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
}
