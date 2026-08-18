package io.github.igrgin.congestiontax.taxrule.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tax_exemption")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxExemptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "rule_set_id", nullable = false)
    private Long ruleSetId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "type_code", nullable = false, length = 32)
    private TaxExemptionType type;

    @Getter
    @Column(name = "day_of_week")
    private Short dayOfWeek;

    @Getter
    @Column(name = "month_number")
    private Short monthNumber;

    @Getter
    @Column(name = "holiday_date")
    private LocalDate holidayDate;

    @Getter
    @Column(name = "vehicle_type_code", length = 32)
    private String vehicleTypeCode;

    @Column(length = 255)
    private String description;

    TaxExemptionEntity(
            Long ruleSetId,
            TaxExemptionType type,
            Short dayOfWeek,
            Short monthNumber,
            LocalDate holidayDate,
            String vehicleTypeCode) {
        this.ruleSetId = ruleSetId;
        this.type = type;
        this.dayOfWeek = dayOfWeek;
        this.monthNumber = monthNumber;
        this.holidayDate = holidayDate;
        this.vehicleTypeCode = vehicleTypeCode;
    }
}
