package io.mero.app.domain.footprint.entity;

import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Footprint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false,
            foreignKey = @ForeignKey(
                    foreignKeyDefinition = "FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE"
            ))
    private Trip trip;

    @Column(name = "title", length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "weather_info", length = 100)
    private String weatherInfo;

    @Column(name = "client_id", length = 36, unique = true)
    private String clientId;

    @OneToMany(mappedBy = "footprint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Photo> photos = new ArrayList<>();

    @OneToMany(mappedBy = "footprint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<FootprintLocation> locations = new ArrayList<>();


//    @Version // 충돌 해결용
//    private Long version;

    @Builder
    public Footprint(Long id, Trip trip, String title, String content,
                     LocalDate date, String weatherInfo, List<Photo> photoUrls, String clientId) {
        this.id = id;
        this.trip = trip;
        this.title = title;
        this.content = content;
        this.date = date;
        this.weatherInfo = weatherInfo;
        this.clientId = clientId;
        this.photos = photoUrls;
        this.locations = new ArrayList<>();
    }

    public void update(String title, String content, LocalDate date, String weatherInfo) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.weatherInfo = weatherInfo;
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
        if (photos == null) return new ArrayList<>();
        return photos.stream()
                .sorted(Comparator.comparing(Photo::getOrderIndex))
                .map(Photo::getS3Url)
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

}
