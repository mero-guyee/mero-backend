package io.mero.app.domain.footprint.entity;

import io.mero.app.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "footprint_location")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FootprintLocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "footprint_id", nullable = false)
    private Footprint footprint;

    @Column(name = "place_name", nullable = false)
    private String placeName;

    @Column(name = "country")
    private String country;

    @Column(name = "city")
    private String city;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "order")
    private Integer order;

    @Builder
    public FootprintLocation(Footprint footprint, String placeName, String country,
                             String city, BigDecimal latitude, BigDecimal longitude,
                             Integer order) {
        this.footprint = footprint;
        this.placeName = placeName;
        this.country = country;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.order = order;
    }

    public void setFootprint(Footprint footprint) {
        this.footprint = footprint;
    }

}
