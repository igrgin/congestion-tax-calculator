package io.github.igrgin.congestiontax.taxrule.persistence;

import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tax_rule_set")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TaxRuleSetEntity {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Getter
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    TaxRuleSetEntity(Long cityId, LocalDate effectiveFrom, String currencyCode) {
        this.cityId = cityId;
        this.effectiveFrom = effectiveFrom;
        this.currencyCode = currencyCode;
    }

    TaxRuleSet toTaxRuleSet(String cityCode, List<TaxTimeBandEntity> taxTimeBandEntities) {
        var currency = Currency.getInstance(currencyCode);

        var taxTimeBands = taxTimeBandEntities.stream()
                .map(entity -> entity.toTaxTimeBand(currency))
                .toList();

        return new TaxRuleSet(cityCode, effectiveFrom, currency, taxTimeBands);
    }
}
