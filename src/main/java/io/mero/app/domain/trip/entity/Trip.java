package io.mero.app.domain.trip.entity;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "trips")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "countries", length = 1000)
    private String countriesStr;

    @Column(name = "total_budget", precision = 15, scale = 2)
    private BigDecimal totalBudget;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_currency", length = 20)
    private Currency budgetCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_currency", length = 20)
    private Currency defaultCurrency;

    @Builder
    public Trip(Long id, User user, String title, String description,
                LocalDate startDate, LocalDate endDate, List<String> countries,
                BigDecimal totalBudget, Currency budgetCurrency, Currency defaultCurrency) {
        this.id = id;
        this.user = user;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        setCountries(countries);
        this.totalBudget = totalBudget;
        this.budgetCurrency = budgetCurrency;
        this.defaultCurrency = defaultCurrency != null ? defaultCurrency : Currency.KRW;
    }

    public boolean isOwner(Long userId) {
        return this.user.getId().equals(userId);
    }

    public void update(String title, String description, LocalDate startDate, LocalDate endDate,
                       List<String> countries, BigDecimal totalBudget,
                       Currency budgetCurrency, Currency defaultCurrency) {
        validateTitle(title);
        validatePeriod(startDate, endDate);
        validateBudget(totalBudget);
        validateDefaultCurrency(defaultCurrency);

        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        setCountries(countries);
        this.totalBudget = totalBudget;
        this.budgetCurrency = budgetCurrency;
        this.defaultCurrency = defaultCurrency;
    }

    public List<String> getCountries() {
        if (countriesStr == null || countriesStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(countriesStr.split(",")));
    }

    public void setCountries(List<String> countries) {
        if (countries == null || countries.isEmpty()) {
            this.countriesStr = "";
        } else {
            this.countriesStr = String.join(",", countries);
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("여행 제목은 필수입니다");
        }
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일과 종료일은 필수입니다");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다");
        }
    }

    private void validateBudget(BigDecimal totalBudget) {
        if (totalBudget != null && totalBudget.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("예산은 0 이상이어야 합니다");
        }
    }

    private void validateDefaultCurrency(Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException("기본 통화는 필수입니다");
        }
    }
}