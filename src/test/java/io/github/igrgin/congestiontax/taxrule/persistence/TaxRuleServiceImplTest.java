package io.github.igrgin.congestiontax.taxrule.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.rule.TaxExemptions;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import io.github.igrgin.congestiontax.domain.rule.WeekdayTaxExemption;
import io.github.igrgin.congestiontax.taxrule.TaxRuleService;
import io.github.igrgin.congestiontax.taxrule.TaxRuleServiceImpl;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidCityTimeZoneException;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidTaxRuleOptionException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingApplicableTaxRuleSetException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.OverlappingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import io.github.igrgin.congestiontax.taxrule.model.ApplicableTaxRuleSets;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
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

    private static final String CITY_CODE = "gothenburg";
    private static final LocalDate CALCULATION_DATE = LocalDate.of(2013, Month.FEBRUARY, 8);
    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2013, Month.JANUARY, 1);
    private static final long RULE_SET_ID = 1L;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private VehicleTypeRepository vehicleTypeRepository;

    @Mock
    private TaxRuleSetRepository taxRuleSetRepository;

    @Mock
    private TaxTimeBandRepository taxTimeBandRepository;

    @Mock
    private TaxRuleOptionRepository taxRuleOptionRepository;

    @Mock
    private TaxExemptionRepository taxExemptionRepository;

    private TaxRuleService taxRuleService;

    @BeforeEach
    void setUp() {
        taxRuleService = new TaxRuleServiceImpl(
                cityRepository,
                vehicleTypeRepository,
                taxRuleSetRepository,
                taxTimeBandRepository,
                taxRuleOptionRepository,
                taxExemptionRepository);
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
        var calculationDate = LocalDate.of(2013, Month.FEBRUARY, 8);
        var effectiveFrom = LocalDate.of(2013, Month.JANUARY, 1);
        var ruleSetId = 1L;
        var currency = Currency.getInstance("SEK");
        var taxRuleSetEntity = storedTaxRuleSet(ruleSetId, effectiveFrom, currency.getCurrencyCode());
        var firstTaxTimeBandEntity =
                new TaxTimeBandEntity(ruleSetId, LocalTime.of(6, 0), LocalTime.of(6, 30), new BigDecimal("8.00"));
        var secondTaxTimeBandEntity =
                new TaxTimeBandEntity(ruleSetId, LocalTime.of(6, 30), LocalTime.of(7, 0), new BigDecimal("13.00"));

        given(cityRepository.findByCode(cityCode))
                .willReturn(Optional.of(new CityEntity(cityCode, "Gothenburg", "Europe/Stockholm")));
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

        assertThat(result)
                .isEqualTo(new ApplicableTaxRuleSets(
                        ZoneId.of("Europe/Stockholm"), Map.of(calculationDate, expectedTaxRuleSet)));
    }

    @Test
    void loadsStoredWeekdayTaxExemption() {
        var taxRuleSet = storedTaxRuleSet(RULE_SET_ID, EFFECTIVE_FROM, "SEK");
        var taxTimeBand =
                new TaxTimeBandEntity(RULE_SET_ID, LocalTime.of(6, 0), LocalTime.of(6, 30), new BigDecimal("8.00"));
        var taxExemption = new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.WEEKDAY, (short) 6, null, null, null);

        given(cityRepository.findByCode(CITY_CODE))
                .willReturn(Optional.of(new CityEntity(CITY_CODE, "Gothenburg", "Europe/Stockholm")));
        given(taxRuleSetRepository.findApplicableCandidates(CITY_CODE, CALCULATION_DATE))
                .willReturn(List.of(taxRuleSet));
        given(taxTimeBandRepository.findByRuleSetIdIn(Set.of(RULE_SET_ID))).willReturn(List.of(taxTimeBand));
        given(taxExemptionRepository.findByRuleSetIdIn(Set.of(RULE_SET_ID))).willReturn(List.of(taxExemption));

        var result = taxRuleService.getApplicableTaxRuleSets(CITY_CODE, Set.of(CALCULATION_DATE));

        assertThat(result.taxRuleSetsByCalculationDate().get(CALCULATION_DATE).taxExemptions())
                .isEqualTo(new TaxExemptions(List.of(new WeekdayTaxExemption(DayOfWeek.SATURDAY))));
    }

    @Test
    void rejectsUnknownCity() {
        var cityCode = "unknown";
        var calculationDates = Set.of(LocalDate.of(2013, Month.FEBRUARY, 8));

        given(cityRepository.findByCode(cityCode)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .isInstanceOf(UnknownCityException.class);
    }

    @Test
    void rejectsDateWithoutApplicableTaxRuleSet() {
        var cityCode = "gothenburg";
        var calculationDate = LocalDate.of(2013, Month.FEBRUARY, 8);
        var calculationDates = Set.of(calculationDate);

        given(cityRepository.findByCode(cityCode))
                .willReturn(Optional.of(new CityEntity(cityCode, "Gothenburg", "Europe/Stockholm")));
        given(taxRuleSetRepository.findApplicableCandidates(cityCode, calculationDate))
                .willReturn(List.of());

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .isInstanceOf(MissingApplicableTaxRuleSetException.class);
    }

    @Test
    void rejectsTaxRuleSetWithoutTaxTimeBands() {
        var cityCode = "gothenburg";
        var calculationDate = LocalDate.of(2013, Month.FEBRUARY, 8);
        var effectiveFrom = LocalDate.of(2013, Month.JANUARY, 1);
        var calculationDates = Set.of(calculationDate);
        var ruleSetId = 1L;
        var taxRuleSetEntity = storedTaxRuleSet(ruleSetId, effectiveFrom, "SEK");

        given(cityRepository.findByCode(cityCode))
                .willReturn(Optional.of(new CityEntity(cityCode, "Gothenburg", "Europe/Stockholm")));
        given(taxRuleSetRepository.findApplicableCandidates(cityCode, calculationDate))
                .willReturn(List.of(taxRuleSetEntity));
        given(taxTimeBandRepository.findByRuleSetIdIn(Set.of(ruleSetId))).willReturn(List.of());

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .isInstanceOf(MissingTaxTimeBandsException.class);
    }

    @Test
    void rejectsOverlappingTaxTimeBands() {
        var cityCode = "gothenburg";
        var calculationDate = LocalDate.of(2013, Month.FEBRUARY, 8);
        var effectiveFrom = LocalDate.of(2013, Month.JANUARY, 1);
        var calculationDates = Set.of(calculationDate);
        var ruleSetId = 1L;
        var taxRuleSetEntity = storedTaxRuleSet(ruleSetId, effectiveFrom, "SEK");
        var laterTaxTimeBand =
                new TaxTimeBandEntity(ruleSetId, LocalTime.of(6, 30), LocalTime.of(7, 30), new BigDecimal("13.00"));
        var earlierTaxTimeBand =
                new TaxTimeBandEntity(ruleSetId, LocalTime.of(6, 0), LocalTime.of(7, 0), new BigDecimal("8.00"));

        given(cityRepository.findByCode(cityCode))
                .willReturn(Optional.of(new CityEntity(cityCode, "Gothenburg", "Europe/Stockholm")));
        given(taxRuleSetRepository.findApplicableCandidates(cityCode, calculationDate))
                .willReturn(List.of(taxRuleSetEntity));
        given(taxTimeBandRepository.findByRuleSetIdIn(Set.of(ruleSetId)))
                .willReturn(List.of(laterTaxTimeBand, earlierTaxTimeBand));

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .isInstanceOf(OverlappingTaxTimeBandsException.class);
    }

    @Test
    void rejectsInvalidStoredChargeWindow() {
        var taxRuleOption = new TaxRuleOptionEntity(RULE_SET_ID, TaxRuleOptionType.CHARGE_WINDOW, null, null, null);

        assertInvalidStoredTaxRuleOptions(List.of(taxRuleOption), "CHARGE_WINDOW");
    }

    @Test
    void rejectsInvalidStoredDailyMaximum() {
        var taxRuleOption =
                new TaxRuleOptionEntity(RULE_SET_ID, TaxRuleOptionType.DAILY_MAXIMUM, BigDecimal.ZERO, null, null);

        assertInvalidStoredTaxRuleOptions(List.of(taxRuleOption), "DAILY_MAXIMUM");
    }

    @Test
    void rejectsDuplicateStoredTaxRuleOptionTypes() {
        var firstTaxRuleOption = new TaxRuleOptionEntity(RULE_SET_ID, TaxRuleOptionType.CHARGE_WINDOW, null, 60, null);
        var secondTaxRuleOption = new TaxRuleOptionEntity(RULE_SET_ID, TaxRuleOptionType.CHARGE_WINDOW, null, 30, null);

        assertInvalidStoredTaxRuleOptions(List.of(firstTaxRuleOption, secondTaxRuleOption), "CHARGE_WINDOW");
    }

    @Test
    void rejectsMissingStoredTaxRuleOptionType() {
        var taxRuleOption = new TaxRuleOptionEntity(RULE_SET_ID, null, null, null, null);

        assertInvalidStoredTaxRuleOptions(List.of(taxRuleOption), "UNKNOWN");
    }

    @Test
    void rejectsInvalidStoredCityTimeZone() {
        var cityCode = "gothenburg";
        var calculationDates = Set.of(LocalDate.of(2013, Month.FEBRUARY, 8));

        given(cityRepository.findByCode(cityCode))
                .willReturn(Optional.of(new CityEntity(cityCode, "Gothenburg", "Mars/Olympus")));

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .isInstanceOf(InvalidCityTimeZoneException.class);
    }

    private static TaxRuleSetEntity storedTaxRuleSet(Long id, LocalDate effectiveFrom, String currencyCode) {
        var taxRuleSetEntity = spy(new TaxRuleSetEntity(1L, effectiveFrom, currencyCode));

        given(taxRuleSetEntity.getId()).willReturn(id);

        return taxRuleSetEntity;
    }

    private void assertInvalidStoredTaxRuleOptions(List<TaxRuleOptionEntity> taxRuleOptions, String optionTypeCode) {
        var taxRuleSet = storedTaxRuleSet(RULE_SET_ID, EFFECTIVE_FROM, "SEK");
        var taxTimeBand =
                new TaxTimeBandEntity(RULE_SET_ID, LocalTime.of(6, 0), LocalTime.of(6, 30), new BigDecimal("8.00"));

        given(cityRepository.findByCode(CITY_CODE))
                .willReturn(Optional.of(new CityEntity(CITY_CODE, "Gothenburg", "Europe/Stockholm")));
        given(taxRuleSetRepository.findApplicableCandidates(CITY_CODE, CALCULATION_DATE))
                .willReturn(List.of(taxRuleSet));
        given(taxTimeBandRepository.findByRuleSetIdIn(Set.of(RULE_SET_ID))).willReturn(List.of(taxTimeBand));
        given(taxRuleOptionRepository.findByRuleSetIdIn(Set.of(RULE_SET_ID))).willReturn(taxRuleOptions);

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(CITY_CODE, Set.of(CALCULATION_DATE)))
                .isInstanceOfSatisfying(
                        InvalidTaxRuleOptionException.class,
                        exception -> assertThat(exception.optionTypeCode()).isEqualTo(optionTypeCode));
    }
}
