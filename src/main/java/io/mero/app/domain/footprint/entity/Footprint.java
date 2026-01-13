package io.mero.app.domain.footprint.entity;

import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.user.entity.User;
import io.mero.app.global.entity.BaseEntity;
import io.mero.app.global.exception.ForbiddenException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Footprint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "title", length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "weather_info", length = 100)
    private String weatherInfo;

    @OneToMany(mappedBy = "footprint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Photo> photos = new ArrayList<>();

    @OneToMany(mappedBy = "footprint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("order ASC")
    private List<FootprintLocation> locations = new ArrayList<>();


    @Column(name = "is_synced", nullable = false)
    private Boolean isSynced = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    private Long version;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @Builder
    public Footprint(Long id, Trip trip, String title, String content,
                     LocalDate date, String weatherInfo, List<String> photoUrls) {
        this.id = id;
        this.trip = trip;
        this.title = title;
        this.content = content;
        this.date = date;
        this.weatherInfo = weatherInfo;
        this.isSynced = false;
        this.lastModifiedAt = LocalDateTime.now();
        this.photos = new ArrayList<>();
        this.locations = new ArrayList<>();
    }

    public void update(String title, String content, LocalDate date) {
        this.title = title;
        this.content = content;
        this.date = date;
    }

    // === 사진 관리 ===
    public void updatePhotos(List<Photo> newPhotos) {
        this.photos.clear();

        if (newPhotos != null && !newPhotos.isEmpty()) {
            for (Photo photo : newPhotos) {
                photo.setFootprint(this);
                this.photos.add(photo);
            }
        }
    }

    public List<String> getPhotoUrls() {
        return photos.stream()
                .sorted(Comparator.comparing(Photo::getOrderIndex))
                .map(Photo::getImageUrl)
                .collect(Collectors.toList());
    }

    // === 위치 관리 ===
    public void updateLocations(List<FootprintLocation> newLocations) {
        this.locations.clear();

        if (newLocations != null && !newLocations.isEmpty()) {
            for (FootprintLocation location : newLocations) {
                location.setFootprint(this);
                this.locations.add(location);
            }
        }
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
