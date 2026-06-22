package io.mero.app.domain.trip.entity;

import io.mero.app.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "trip_memos")
@SQLRestriction("deleted_at IS NULL")
public class TripMemo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "client_id", length = 36, unique = true)
    private String clientId;

    @Builder
    public TripMemo(Trip trip, String title,
                    String content, String clientId) {
        this.trip = trip;
        this.title = title;
        this.content = content;
        this.clientId = clientId;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

}
