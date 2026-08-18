package io.github.igrgin.congestiontax.taxrule.persistence;

import static io.github.igrgin.congestiontax.taxrule.persistence.TaxTimeBandEntity.toTaxTimeBand;

import io.github.igrgin.congestiontax.domain.rule.TaxExemptions;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleOptions;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @JdbcTypeCode(SqlTypes.CHAR)
    @Getter
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    TaxRuleSetEntity(Long cityId, String currencyCode) {
        this.cityId = cityId;
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
                .map(entity -> toTaxTimeBand(entity, currency))
                .toList();

        return new TaxRuleSet(cityCode, currency, taxTimeBands, taxExemptions, taxRuleOptions);
    }
}
