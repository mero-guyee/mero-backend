package io.mero.app.domain.exchange.entity;

import io.mero.app.global.entity.BaseEntity;
import io.mero.app.global.enums.Currency;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"from_currency", "to_currency", "date"}
        ),
        indexes = {
                @Index(name = "idx_exchange_date", columnList = "date"),
                @Index(name = "idx_exchange_currencies", columnList = "from_currency, to_currency")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_currency", nullable = false, length = 20)
    private Currency fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 20)
    private Currency toCurrency;

    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal rate;

    @Column(nullable = false)
    private LocalDate date;

    @Builder
    public ExchangeRate(Long id, Currency fromCurrency, Currency toCurrency,
                        BigDecimal rate, LocalDate date) {
        this.id = id;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.date = date;
    }

}
