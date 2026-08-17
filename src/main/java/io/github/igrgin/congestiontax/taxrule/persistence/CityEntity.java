package io.github.igrgin.congestiontax.taxrule.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "city")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Getter
    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    CityEntity(String code, String name, String timeZone) {
        this.code = code;
        this.name = name;
        this.timeZone = timeZone;
    }
}
