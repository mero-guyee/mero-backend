package io.mero.app.global.embedded;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Location {

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;   // 위도

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;  // 경도

    @Column(name = "location_name", length = 200)
    private String locationName;   // 장소명

}
