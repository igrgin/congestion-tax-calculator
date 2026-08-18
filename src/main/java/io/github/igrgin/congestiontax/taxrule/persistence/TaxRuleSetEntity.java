package io.github.igrgin.congestiontax.taxrule.persistence;

import io.github.igrgin.congestiontax.domain.rule.TaxExemptions;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleOptions;
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
public class TaxRuleSetEntity {

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
    @Getter
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    TaxRuleSetEntity(Long cityId, LocalDate effectiveFrom, String currencyCode) {
        this.cityId = cityId;
        this.effectiveFrom = effectiveFrom;
        this.currencyCode = currencyCode;
    }

    public static TaxRuleSet toTaxRuleSet(
            TaxRuleSetEntity taxRuleSetEntity,
            String cityCode,
            List<TaxTimeBandEntity> taxTimeBandEntities,
            TaxExemptions taxExemptions,
            TaxRuleOptions taxRuleOptions) {
        var currency = Currency.getInstance(taxRuleSetEntity.currencyCode);

        var taxTimeBands = taxTimeBandEntities.stream()
                .map(entity -> entity.toTaxTimeBand(currency))
                .toList();

        return new TaxRuleSet(
                cityCode, taxRuleSetEntity.effectiveFrom, currency, taxTimeBands, taxExemptions, taxRuleOptions);
    }
}
