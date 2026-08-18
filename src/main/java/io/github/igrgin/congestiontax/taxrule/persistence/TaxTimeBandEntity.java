package io.github.igrgin.congestiontax.taxrule.persistence;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Currency;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tax_time_band")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxTimeBandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "rule_set_id", nullable = false)
    private Long ruleSetId;

    @Getter
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Getter
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, precision = 12, scale = 2)
    @Getter
    private BigDecimal amount;

    TaxTimeBandEntity(Long ruleSetId, LocalTime startTime, LocalTime endTime, BigDecimal amount) {
        this.ruleSetId = ruleSetId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.amount = amount;
    }

    static TaxTimeBand toTaxTimeBand(TaxTimeBandEntity entity, Currency currency) {
        return new TaxTimeBand(entity.getStartTime(), entity.getEndTime(), new TaxAmount(entity.getAmount(), currency));
    }
}
