package io.github.igrgin.congestiontax.citylocaltime.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "city")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    CityEntity(String code, String name, String timeZone) {
        this.code = code;
        this.name = name;
        this.timeZone = timeZone;
    }

    ZoneId toZoneId() {
        return ZoneId.of(timeZone);
    }
}
