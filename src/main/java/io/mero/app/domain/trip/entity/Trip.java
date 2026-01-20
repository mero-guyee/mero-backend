package io.mero.app.domain.trip.entity;

import io.mero.app.domain.budget.entity.Budget;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.user.entity.User;
import io.mero.app.global.entity.BaseEntity;
import io.mero.app.global.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "countries", length = 1000)
    private String countriesStr;

    @OneToOne(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TripCoverImage coverImage;

    @Column(name = "is_synced", nullable = false)
    private Boolean isSynced = false;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Budget> budgets = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Footprint> footprints = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripDocument> documents = new ArrayList<>();

    @Builder
    public Trip(Long id, User user, String title,
                LocalDate startDate, LocalDate endDate, List<String> countries) {
        this.id = id;
        this.user = user;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        setCountries(countries);
    }

    public boolean isOwner(Long userId) {
        return this.user.getId().equals(userId);
    }

    public void update(String title, LocalDate startDate, LocalDate endDate,
                       List<String> countries) {
        validateTitle(title);
        validatePeriod(startDate, endDate);

        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        setCountries(countries);
    }

    public void setCoverImage(TripCoverImage coverImage) {
        this.coverImage = coverImage;
    }

    public void removeCoverImage() {
        this.coverImage = null;
    }

    public String getCoverImageUrl() {
        return coverImage != null ? coverImage.getS3Url() : null;
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
            throw new BadRequestException("여행 제목은 필수입니다");
        }
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("날짜는 필수입니다");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("시작일은 종료일보다 이전이어야 합니다");
        }
    }
}
