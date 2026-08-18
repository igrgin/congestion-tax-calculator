package io.github.igrgin.congestiontax.taxrule.persistence;

import io.github.igrgin.congestiontax.domain.VehicleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vehicle_type")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class VehicleTypeEntity {

    @Id
    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String description;

    public static VehicleType toVehicleType(VehicleTypeEntity entity) {
        return new VehicleType(entity.code, entity.description);
    }
}
