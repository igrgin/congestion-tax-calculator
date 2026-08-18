package io.github.igrgin.congestiontax.taxrule.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import io.github.igrgin.congestiontax.taxrule.TaxRuleService;
import io.github.igrgin.congestiontax.taxrule.TaxRuleServiceImpl;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidCityTimeZoneException;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidTaxRuleOptionException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxRuleSetException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.OverlappingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import io.github.igrgin.congestiontax.taxrule.model.CityTaxRuleSet;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxRuleServiceImplTest {

    private static final String CITY_CODE = "gothenburg";
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

    private TaxRuleService taxRuleService;

    @BeforeEach
    void setUp() {
        taxRuleService = new TaxRuleServiceImpl(
                cityRepository,
                vehicleTypeRepository,
                taxRuleSetRepository,
                taxTimeBandRepository,
                taxRuleOptionRepository);
    }

    @Test
    void loadsVehicleType() {
        given(vehicleTypeRepository.findByCode("OTHER"))
                .willReturn(Optional.of(new VehicleTypeEntity("OTHER", "Other vehicle")));

        assertThat(taxRuleService.getVehicleType("OTHER")).isEqualTo(new VehicleType("OTHER", "Other vehicle"));
    }

    @Test
    void rejectsUnknownVehicleType() {
        given(vehicleTypeRepository.findByCode("UNKNOWN")).willReturn(Optional.empty());

        assertThatThrownBy(() -> taxRuleService.getVehicleType("UNKNOWN"))
                .isInstanceOf(UnknownVehicleTypeException.class);
    }

    @Test
    void loadsCityTaxRuleSetWithAdjacentTaxTimeBands() {
        var currency = Currency.getInstance("SEK");
        var firstBand = taxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), "8.00");
        var secondBand = taxTimeBand(LocalTime.of(6, 30), LocalTime.of(7, 0), "13.00");
        givenCompleteTaxRuleSet(List.of(firstBand, secondBand));

        var result = taxRuleService.getCityTaxRuleSet(CITY_CODE);

        var expected = new TaxRuleSet(
                CITY_CODE,
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

        assertThat(result).isEqualTo(new CityTaxRuleSet(ZoneId.of("Europe/Stockholm"), expected));
    }

    @Test
    void loadsCrossMidnightTaxTimeBand() {
        givenCompleteTaxRuleSet(List.of(taxTimeBand(LocalTime.of(18, 30), LocalTime.of(6, 0), "8.00")));

        assertThat(taxRuleService.getCityTaxRuleSet(CITY_CODE).taxRuleSet().taxTimeBands())
                .extracting(TaxTimeBand::startTime, TaxTimeBand::endTime)
                .containsExactly(tuple(LocalTime.of(18, 30), LocalTime.of(6, 0)));
    }

    @Test
    void rejectsUnknownCity() {
        given(cityRepository.findByCode("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet("unknown")).isInstanceOf(UnknownCityException.class);
    }

    @Test
    void rejectsMissingCityTaxRuleSet() {
        givenCity();
        given(taxRuleSetRepository.findByCityCode(CITY_CODE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet(CITY_CODE))
                .isInstanceOf(MissingTaxRuleSetException.class);
    }

    @Test
    void rejectsTaxRuleSetWithoutTaxTimeBands() {
        givenCompleteTaxRuleSet(List.of());

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet(CITY_CODE))
                .isInstanceOf(MissingTaxTimeBandsException.class);
    }

    @Test
    void rejectsOverlappingTaxTimeBandsIndependentOfOrder() {
        var laterBand = taxTimeBand(LocalTime.of(6, 30), LocalTime.of(7, 30), "13.00");
        var earlierBand = taxTimeBand(LocalTime.of(6, 0), LocalTime.of(7, 0), "8.00");
        givenCompleteTaxRuleSet(List.of(laterBand, earlierBand));

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet(CITY_CODE))
                .isInstanceOf(OverlappingTaxTimeBandsException.class);
    }

    @Test
    void rejectsOverlapAcrossMidnight() {
        var crossMidnightBand = taxTimeBand(LocalTime.of(18, 30), LocalTime.of(6, 0), "8.00");
        var earlyBand = taxTimeBand(LocalTime.of(5, 30), LocalTime.of(6, 30), "13.00");
        givenCompleteTaxRuleSet(List.of(earlyBand, crossMidnightBand));

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet(CITY_CODE))
                .isInstanceOf(OverlappingTaxTimeBandsException.class);
    }

    @Test
    void rejectsInvalidStoredChargeWindow() {
        assertInvalidStoredTaxRuleOptions(
                List.of(new TaxRuleOptionEntity(RULE_SET_ID, TaxRuleOptionType.CHARGE_WINDOW, null, null, null)),
                "CHARGE_WINDOW");
    }

    @Test
    void rejectsInvalidStoredDailyMaximum() {
        assertInvalidStoredTaxRuleOptions(
                List.of(new TaxRuleOptionEntity(
                        RULE_SET_ID, TaxRuleOptionType.DAILY_MAXIMUM, BigDecimal.ZERO, null, null)),
                "DAILY_MAXIMUM");
    }

    @Test
    void rejectsDuplicateStoredTaxRuleOptionTypes() {
        var first = new TaxRuleOptionEntity(RULE_SET_ID, TaxRuleOptionType.CHARGE_WINDOW, null, 60, null);
        var second = new TaxRuleOptionEntity(RULE_SET_ID, TaxRuleOptionType.CHARGE_WINDOW, null, 30, null);

        assertInvalidStoredTaxRuleOptions(List.of(first, second), "CHARGE_WINDOW");
    }

    @Test
    void rejectsMissingStoredTaxRuleOptionType() {
        assertInvalidStoredTaxRuleOptions(
                List.of(new TaxRuleOptionEntity(RULE_SET_ID, null, null, null, null)), "UNKNOWN");
    }

    @Test
    void rejectsInvalidStoredCityTimeZone() {
        given(cityRepository.findByCode(CITY_CODE))
                .willReturn(Optional.of(new CityEntity(CITY_CODE, "Gothenburg", "Mars/Olympus")));

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet(CITY_CODE))
                .isInstanceOf(InvalidCityTimeZoneException.class);
    }

    private void givenCompleteTaxRuleSet(List<TaxTimeBandEntity> taxTimeBands) {
        var taxRuleSet = storedTaxRuleSet("SEK");
        givenCity();
        given(taxRuleSetRepository.findByCityCode(CITY_CODE)).willReturn(Optional.of(taxRuleSet));
        given(taxTimeBandRepository.findByRuleSetId(RULE_SET_ID)).willReturn(taxTimeBands);
    }

    private void givenCity() {
        given(cityRepository.findByCode(CITY_CODE))
                .willReturn(Optional.of(new CityEntity(CITY_CODE, "Gothenburg", "Europe/Stockholm")));
    }

    private static TaxRuleSetEntity storedTaxRuleSet(String currencyCode) {
        var taxRuleSetEntity = spy(new TaxRuleSetEntity(1L, currencyCode));
        given(taxRuleSetEntity.getId()).willReturn(RULE_SET_ID);
        return taxRuleSetEntity;
    }

    private static TaxTimeBandEntity taxTimeBand(LocalTime start, LocalTime end, String amount) {
        return new TaxTimeBandEntity(RULE_SET_ID, start, end, new BigDecimal(amount));
    }

    private void assertInvalidStoredTaxRuleOptions(List<TaxRuleOptionEntity> options, String optionTypeCode) {
        givenCompleteTaxRuleSet(List.of(taxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), "8.00")));
        given(taxRuleOptionRepository.findByRuleSetId(RULE_SET_ID)).willReturn(options);

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet(CITY_CODE))
                .isInstanceOfSatisfying(
                        InvalidTaxRuleOptionException.class,
                        exception -> assertThat(exception.optionTypeCode()).isEqualTo(optionTypeCode));
    }
}
