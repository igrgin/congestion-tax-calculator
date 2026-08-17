package io.github.igrgin.congestiontax.taxrule.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import io.github.igrgin.congestiontax.taxrule.TaxRuleService;
import io.github.igrgin.congestiontax.taxrule.TaxRuleServiceImpl;
import io.github.igrgin.congestiontax.taxrule.exception.MissingApplicableTaxRuleSetException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.OverlappingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxRuleServiceImplTest {

    @Mock
    private VehicleTypeRepository vehicleTypeRepository;

    @Mock
    private TaxRuleSetRepository taxRuleSetRepository;

    @Mock
    private TaxTimeBandRepository taxTimeBandRepository;

    private TaxRuleService taxRuleService;

    @BeforeEach
    void setUp() {
        taxRuleService = new TaxRuleServiceImpl(vehicleTypeRepository, taxRuleSetRepository, taxTimeBandRepository);
    }

    @Test
    void loadsVehicleType() {
        var vehicleTypeEntity = new VehicleTypeEntity("OTHER", "Other vehicle");

        given(vehicleTypeRepository.findByCode("OTHER")).willReturn(Optional.of(vehicleTypeEntity));

        var result = taxRuleService.getVehicleType("OTHER");

        assertThat(result).isEqualTo(new VehicleType("OTHER", "Other vehicle"));
    }

    @Test
    void rejectsUnknownVehicleType() {
        var vehicleTypeCode = "UNKNOWN";

        given(vehicleTypeRepository.findByCode(vehicleTypeCode)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taxRuleService.getVehicleType(vehicleTypeCode))
                .isInstanceOf(UnknownVehicleTypeException.class);
    }

    @Test
    void loadsApplicableTaxRuleSet() {
        var cityCode = "gothenburg";
        var calculationDate = LocalDate.of(2013, 2, 8);
        var effectiveFrom = LocalDate.of(2013, 1, 1);
        var ruleSetId = 1L;
        var currency = Currency.getInstance("SEK");
        var taxRuleSetEntity = storedTaxRuleSet(ruleSetId, effectiveFrom, currency.getCurrencyCode());
        var firstTaxTimeBandEntity =
                new TaxTimeBandEntity(ruleSetId, LocalTime.of(6, 0), LocalTime.of(6, 30), new BigDecimal("8.00"));
        var secondTaxTimeBandEntity =
                new TaxTimeBandEntity(ruleSetId, LocalTime.of(6, 30), LocalTime.of(7, 0), new BigDecimal("13.00"));

        given(taxRuleSetRepository.cityExists(cityCode)).willReturn(true);
        given(taxRuleSetRepository.findApplicableCandidates(cityCode, calculationDate))
                .willReturn(List.of(taxRuleSetEntity));
        given(taxTimeBandRepository.findByRuleSetIdIn(Set.of(ruleSetId)))
                .willReturn(List.of(firstTaxTimeBandEntity, secondTaxTimeBandEntity));

        var result = taxRuleService.getApplicableTaxRuleSets(cityCode, Set.of(calculationDate));

        var expectedTaxRuleSet = new TaxRuleSet(
                cityCode,
                effectiveFrom,
                currency,
                List.of(
                        new TaxTimeBand(
                                LocalTime.of(6, 0),
                                LocalTime.of(6, 30),
                                new TaxAmount(new BigDecimal("8.00"), currency)),
                        new TaxTimeBand(
                                LocalTime.of(6, 30),
                                LocalTime.of(7, 0),
                                new TaxAmount(new BigDecimal("13.00"), currency))));

        assertThat(result).isEqualTo(Map.of(calculationDate, expectedTaxRuleSet));
    }

    @Test
    void rejectsUnknownCity() {
        var cityCode = "unknown";
        var calculationDates = Set.of(LocalDate.of(2013, 2, 8));

        given(taxRuleSetRepository.cityExists(cityCode)).willReturn(false);

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .isInstanceOf(UnknownCityException.class);
    }

    @Test
    void rejectsDateWithoutApplicableTaxRuleSet() {
        var cityCode = "gothenburg";
        var calculationDate = LocalDate.of(2013, 2, 8);
        var calculationDates = Set.of(calculationDate);

        given(taxRuleSetRepository.cityExists(cityCode)).willReturn(true);
        given(taxRuleSetRepository.findApplicableCandidates(cityCode, calculationDate))
                .willReturn(List.of());

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .isInstanceOf(MissingApplicableTaxRuleSetException.class);
    }

    @Test
    void rejectsTaxRuleSetWithoutTaxTimeBands() {
        var cityCode = "gothenburg";
        var calculationDate = LocalDate.of(2013, 2, 8);
        var effectiveFrom = LocalDate.of(2013, 1, 1);
        var calculationDates = Set.of(calculationDate);
        var ruleSetId = 1L;
        var taxRuleSetEntity = storedTaxRuleSet(ruleSetId, effectiveFrom, "SEK");

        given(taxRuleSetRepository.cityExists(cityCode)).willReturn(true);
        given(taxRuleSetRepository.findApplicableCandidates(cityCode, calculationDate))
                .willReturn(List.of(taxRuleSetEntity));
        given(taxTimeBandRepository.findByRuleSetIdIn(Set.of(ruleSetId))).willReturn(List.of());

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .isInstanceOf(MissingTaxTimeBandsException.class);
    }

    @Test
    void rejectsOverlappingTaxTimeBands() {
        var cityCode = "gothenburg";
        var calculationDate = LocalDate.of(2013, 2, 8);
        var effectiveFrom = LocalDate.of(2013, 1, 1);
        var calculationDates = Set.of(calculationDate);
        var ruleSetId = 1L;
        var taxRuleSetEntity = storedTaxRuleSet(ruleSetId, effectiveFrom, "SEK");
        var laterTaxTimeBand =
                new TaxTimeBandEntity(ruleSetId, LocalTime.of(6, 30), LocalTime.of(7, 30), new BigDecimal("13.00"));
        var earlierTaxTimeBand =
                new TaxTimeBandEntity(ruleSetId, LocalTime.of(6, 0), LocalTime.of(7, 0), new BigDecimal("8.00"));

        given(taxRuleSetRepository.cityExists(cityCode)).willReturn(true);
        given(taxRuleSetRepository.findApplicableCandidates(cityCode, calculationDate))
                .willReturn(List.of(taxRuleSetEntity));
        given(taxTimeBandRepository.findByRuleSetIdIn(Set.of(ruleSetId)))
                .willReturn(List.of(laterTaxTimeBand, earlierTaxTimeBand));

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .isInstanceOf(OverlappingTaxTimeBandsException.class);
    }

    private static TaxRuleSetEntity storedTaxRuleSet(Long id, LocalDate effectiveFrom, String currencyCode) {
        var taxRuleSetEntity = spy(new TaxRuleSetEntity(1L, effectiveFrom, currencyCode));

        given(taxRuleSetEntity.getId()).willReturn(id);

        return taxRuleSetEntity;
    }
}
