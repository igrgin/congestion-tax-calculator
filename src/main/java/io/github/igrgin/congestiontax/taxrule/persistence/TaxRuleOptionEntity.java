package io.github.igrgin.congestiontax.taxrule.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tax_rule_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxRuleOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "rule_set_id", nullable = false)
    private Long ruleSetId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "type_code", nullable = false, length = 32)
    private TaxRuleOptionType type;

    @Getter
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Getter
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Getter
    @Column(name = "preceding_days")
    private Short precedingDays;

    @Column(length = 255)
    private String description;

    TaxRuleOptionEntity(
            Long ruleSetId, TaxRuleOptionType type, BigDecimal amount, Integer durationMinutes, Short precedingDays) {
        this.ruleSetId = ruleSetId;
        this.type = type;
        this.amount = amount;
        this.durationMinutes = durationMinutes;
        this.precedingDays = precedingDays;
    }
}
