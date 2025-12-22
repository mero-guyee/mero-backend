package io.mero.app.domain.expense.entity;

import io.mero.app.domain.diary.entity.Diary;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.user.entity.User;
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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id")
    private Diary diary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_currency", length = 20, nullable = false)
    private Currency originalCurrency;

    @Column(name = "converted_amount", precision = 15, scale = 2)
    private BigDecimal convertedAmount;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "is_synced", nullable = false)
    private Boolean isSynced = false;

    @Builder
    public Expense(User user, Trip trip, Diary diary, ExpenseCategory category,
                   LocalDate date, BigDecimal amount, Currency originalCurrency,
                   BigDecimal convertedAmount, String receiptUrl, String memo) {
        this.user = user;
        this.trip = trip;
        this.diary = diary;
        this.category = category;
        this.date = date;
        this.amount = amount;
        this.originalCurrency = originalCurrency;
        this.convertedAmount = convertedAmount;
        this.receiptUrl = receiptUrl;
        this.memo = memo;
        this.isSynced = false;
    }

    public void updateCategory(ExpenseCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("카테고리는 필수입니다");
        }
        this.category = category;
    }

    public void updateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("날짜는 필수입니다");
        }
        this.date = date;
    }

    public void updateAmount(BigDecimal amount, Currency originalCurrency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다");
        }
        if (originalCurrency == null) {
            throw new IllegalArgumentException("통화는 필수입니다");
        }
        this.amount = amount;
        this.originalCurrency = originalCurrency;
    }

    public void updateConversion(BigDecimal convertedAmount) {
        this.convertedAmount = convertedAmount;
    }

    public void updateReceipt(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void linkToDiary(Diary diary) {
        this.diary = diary;
    }

    public void unlinkFromDiary() {
        this.diary = null;
    }

    public void markAsSynced() {
        this.isSynced = true;
    }

    public void markAsUnsynced() {
        this.isSynced = false;
    }
}