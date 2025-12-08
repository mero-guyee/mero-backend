package io.mero.app.domain.diary.entity;

import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.user.entity.User;
import io.mero.app.global.embedded.Location;
import io.mero.app.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDate date;

    @Embedded
    private Location location;

    @Column(name = "weather_info", length = 100)
    private String weatherInfo;

    @ElementCollection
    @CollectionTable(name = "diary_photos", joinColumns = @JoinColumn(name = "diary_id"))
    @Column(name = "photo_url", length = 500)
    private List<String> photoUrls = new ArrayList<>();


    @Column(name = "is_synced", nullable = false)
    private Boolean isSynced = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    private Long version;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @Builder
    public Diary(Long id, Trip trip, String content,
                 LocalDate date, Location location, String weatherInfo, List<String> photoUrls) {
        this.id = id;
        this.trip = trip;
        this.content = content;
        this.date = date;
        this.location = location;
        this.weatherInfo = weatherInfo;
        this.isSynced = false;
        this.lastModifiedAt = LocalDateTime.now();
        this.photoUrls = photoUrls != null ? photoUrls : new ArrayList<>();
    }

    public void update(String content, LocalDate date,
                       Location location, List<String> photoUrls) {
        this.content = content;
        this.date = date;
        this.location = location;
        this.photoUrls = photoUrls != null ? photoUrls : new ArrayList<>();
    }

    // === 동기화 관리 ===
    public void markAsSynced() {
        this.isSynced = true;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void markAsUnsynced() {
        this.isSynced = false;
        this.lastModifiedAt = LocalDateTime.now();
    }

    // === Soft Delete ===
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

}
