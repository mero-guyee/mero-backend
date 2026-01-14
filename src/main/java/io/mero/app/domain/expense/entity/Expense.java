package io.mero.app.domain.expense.entity;

import io.mero.app.domain.footprint.entity.Footprint;
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
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "footprint_id")
    private Footprint footprint;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Currency currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 500)
    private String location;

    @Builder
    public Expense(Long id, Trip trip,  Footprint footprint, BigDecimal amount, Currency currency,
                   ExpenseCategory category, String description, LocalDate date, String location) {
        this.id = id;
        this.trip = trip;
        this.footprint = footprint;
        this.amount = amount;
        this.currency = currency;
        this.category = category;
        this.description = description;
        this.date = date;
        this.location = location;
    }

    public void update(BigDecimal amount, Currency currency, ExpenseCategory category,
                       String description, LocalDate date, String location) {
        validateAmount(amount);
        validateCurrency(currency);
        validateDate(date);

        this.amount = amount;
        this.currency = currency;
        this.category = category;
        this.description = description;
        this.date = date;
        this.location = location;
    }

    public void linkToFootprint(Footprint footprint) {
        if (footprint != null && !footprint.getTrip().equals(this.trip)) {
            throw new BadRequestException("같은 여행의 발자취만 연결할 수 있습니다");
        }
        this.footprint = footprint;
    }

    public void unlinkFromFootprint() {
        this.footprint = null;
    }


    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("금액은 0보다 커야 합니다");
        }
    }

    private void validateCurrency(Currency currency) {
        if (currency == null) {
            throw new BadRequestException("통화는 필수입니다");
        }
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new BadRequestException("날짜는 필수입니다");
        }
    }
}