package io.github.igrgin.congestiontax.taxrule.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.rule.MonthTaxExemption;
import io.github.igrgin.congestiontax.domain.rule.PublicHolidayPrecedingDateOption;
import io.github.igrgin.congestiontax.domain.rule.PublicHolidayTaxExemption;
import io.github.igrgin.congestiontax.domain.rule.TaxExemptions;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import io.github.igrgin.congestiontax.domain.rule.VehicleTypeTaxExemption;
import io.github.igrgin.congestiontax.domain.rule.WeekdayTaxExemption;
import io.github.igrgin.congestiontax.taxrule.TaxRuleService;
import io.github.igrgin.congestiontax.taxrule.TaxRuleServiceImpl;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidCityTimeZoneException;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidTaxExemptionException;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidTaxRuleOptionException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxRuleSetException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.OverlappingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import io.github.igrgin.congestiontax.taxrule.model.CityTaxRuleSet;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    void loadsTaxTimeBandsWithAGap() {
        givenCompleteTaxRuleSet(List.of(
                taxTimeBand(LocalTime.of(6, 0), LocalTime.of(7, 0), "8.00"),
                taxTimeBand(LocalTime.of(8, 0), LocalTime.of(9, 0), "13.00")));

        assertThat(taxRuleService.getCityTaxRuleSet(CITY_CODE).taxRuleSet().taxTimeBands())
                .hasSize(2);
    }

    @Test
    void loadsEachStoredTaxExemptionType() {
        var holidayDate = LocalDate.of(2013, Month.DECEMBER, 25);
        var taxExemptions = List.of(
                new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.WEEKDAY, (short) 6, null, null, null),
                new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.MONTH, null, (short) 7, null, null),
                new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.PUBLIC_HOLIDAY, null, null, holidayDate, null),
                new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.VEHICLE_TYPE, null, null, null, "BUS"));
        givenCompleteTaxRuleSet(
                List.of(taxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), "8.00")), List.of(), taxExemptions);

        var result = taxRuleService.getCityTaxRuleSet(CITY_CODE);

        assertThat(result.taxRuleSet().taxExemptions())
                .isEqualTo(new TaxExemptions(List.of(
                        new WeekdayTaxExemption(DayOfWeek.SATURDAY),
                        new MonthTaxExemption(Month.JULY),
                        new PublicHolidayTaxExemption(holidayDate),
                        new VehicleTypeTaxExemption("BUS"))));
    }

    @Test
    void loadsPublicHolidayPrecedingDateOptionWithoutPublicHolidayTaxExemption() {
        var option = new TaxRuleOptionEntity(RULE_SET_ID, TaxRuleOptionType.HOLIDAY_PRECEDING, null, null, (short) 1);
        givenCompleteTaxRuleSet(
                List.of(taxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), "8.00")), List.of(option), List.of());

        var taxRuleSet = taxRuleService.getCityTaxRuleSet(CITY_CODE).taxRuleSet();

        assertThat(taxRuleSet.taxRuleOptions().publicHolidayPrecedingDateOption())
                .contains(new PublicHolidayPrecedingDateOption(1));
        assertThat(taxRuleSet.taxExemptions()).isEqualTo(TaxExemptions.empty());
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("overlappingTaxTimeBands")
    void rejectsEveryTaxTimeBandOverlapShape(
            String scenario, LocalTime firstStart, LocalTime firstEnd, LocalTime secondStart, LocalTime secondEnd) {
        var firstBand = taxTimeBand(firstStart, firstEnd, "8.00");
        var secondBand = taxTimeBand(secondStart, secondEnd, "13.00");
        givenCompleteTaxRuleSet(List.of(firstBand, secondBand));

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet(CITY_CODE))
                .isInstanceOf(OverlappingTaxTimeBandsException.class);
    }

    @Test
    void stopsValidationAtFirstConflictingPair() {
        var firstBand = taxTimeBand(LocalTime.of(6, 0), LocalTime.of(8, 0), "8.00");
        var secondBand = taxTimeBand(LocalTime.of(7, 0), LocalTime.of(9, 0), "13.00");
        var laterInvalidBand = taxTimeBand(null, null, "18.00");
        givenCompleteTaxRuleSet(List.of(firstBand, secondBand, laterInvalidBand));

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
    void rejectsInvalidStoredPublicHolidayPrecedingDateOption() {
        assertInvalidStoredTaxRuleOptions(
                List.of(new TaxRuleOptionEntity(RULE_SET_ID, TaxRuleOptionType.HOLIDAY_PRECEDING, null, null, null)),
                "HOLIDAY_PRECEDING");
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
    void rejectsMissingStoredTaxExemptionType() {
        assertInvalidStoredTaxExemptions(
                List.of(new TaxExemptionEntity(RULE_SET_ID, null, (short) 6, null, null, null)), "UNKNOWN");
    }

    @Test
    void rejectsDuplicateStoredTaxExemptions() {
        var first = new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.WEEKDAY, (short) 6, null, null, null);
        var second = new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.WEEKDAY, (short) 6, null, null, null);

        assertInvalidStoredTaxExemptions(List.of(first, second), "WEEKDAY");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidStoredTaxExemptions")
    void rejectsInvalidStoredTaxExemption(
            String scenario, TaxExemptionEntity taxExemption, String taxExemptionTypeCode) {
        assertInvalidStoredTaxExemptions(List.of(taxExemption), taxExemptionTypeCode);
    }

    @Test
    void rejectsInvalidStoredCityTimeZone() {
        given(cityRepository.findByCode(CITY_CODE))
                .willReturn(Optional.of(new CityEntity(CITY_CODE, "Gothenburg", "Mars/Olympus")));

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet(CITY_CODE))
                .isInstanceOf(InvalidCityTimeZoneException.class);
    }

    private void givenCompleteTaxRuleSet(List<TaxTimeBandEntity> taxTimeBands) {
        givenCompleteTaxRuleSet(taxTimeBands, List.of(), List.of());
    }

    private void givenCompleteTaxRuleSet(
            List<TaxTimeBandEntity> taxTimeBands,
            List<TaxRuleOptionEntity> taxRuleOptions,
            List<TaxExemptionEntity> taxExemptions) {
        var taxRuleSet = storedTaxRuleSet("SEK");
        givenCity();
        given(taxRuleSetRepository.findByCityCode(CITY_CODE)).willReturn(Optional.of(taxRuleSet));
        given(taxTimeBandRepository.findByRuleSetId(RULE_SET_ID)).willReturn(taxTimeBands);
        given(taxRuleOptionRepository.findByRuleSetId(RULE_SET_ID)).willReturn(taxRuleOptions);
        given(taxExemptionRepository.findByRuleSetId(RULE_SET_ID)).willReturn(taxExemptions);
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

    private static Stream<Arguments> overlappingTaxTimeBands() {
        return Stream.of(
                Arguments.of(
                        "same-date partial overlap",
                        LocalTime.of(7, 0),
                        LocalTime.of(9, 0),
                        LocalTime.of(6, 0),
                        LocalTime.of(8, 0)),
                Arguments.of(
                        "nested band", LocalTime.of(6, 0), LocalTime.of(9, 0), LocalTime.of(7, 0), LocalTime.of(8, 0)),
                Arguments.of(
                        "cross-midnight overlap",
                        LocalTime.of(18, 30),
                        LocalTime.of(6, 0),
                        LocalTime.of(5, 0),
                        LocalTime.of(7, 0)),
                Arguments.of(
                        "full-day band with another band",
                        LocalTime.of(6, 0),
                        LocalTime.of(6, 0),
                        LocalTime.of(7, 0),
                        LocalTime.of(8, 0)),
                Arguments.of(
                        "two full-day bands",
                        LocalTime.of(6, 0),
                        LocalTime.of(6, 0),
                        LocalTime.of(12, 0),
                        LocalTime.of(12, 0)));
    }

    private static Stream<Arguments> invalidStoredTaxExemptions() {
        return Stream.of(
                Arguments.of(
                        "weekday is absent",
                        new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.WEEKDAY, null, null, null, null),
                        "WEEKDAY"),
                Arguments.of(
                        "weekday is below its accepted range",
                        new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.WEEKDAY, (short) 0, null, null, null),
                        "WEEKDAY"),
                Arguments.of(
                        "weekday is above its accepted range",
                        new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.WEEKDAY, (short) 8, null, null, null),
                        "WEEKDAY"),
                Arguments.of(
                        "weekday row has another typed value",
                        new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.WEEKDAY, (short) 6, (short) 7, null, null),
                        "WEEKDAY"),
                Arguments.of(
                        "month is absent",
                        new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.MONTH, null, null, null, null),
                        "MONTH"),
                Arguments.of(
                        "month is outside its accepted range",
                        new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.MONTH, null, (short) 13, null, null),
                        "MONTH"),
                Arguments.of(
                        "month row has another typed value",
                        new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.MONTH, (short) 6, (short) 7, null, null),
                        "MONTH"),
                Arguments.of(
                        "public-holiday date is absent",
                        new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.PUBLIC_HOLIDAY, null, null, null, null),
                        "PUBLIC_HOLIDAY"),
                Arguments.of(
                        "public-holiday row has another typed value",
                        new TaxExemptionEntity(
                                RULE_SET_ID,
                                TaxExemptionType.PUBLIC_HOLIDAY,
                                null,
                                (short) 7,
                                LocalDate.of(2013, Month.DECEMBER, 25),
                                null),
                        "PUBLIC_HOLIDAY"),
                Arguments.of(
                        "Vehicle Type code is absent",
                        new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.VEHICLE_TYPE, null, null, null, null),
                        "VEHICLE_TYPE"),
                Arguments.of(
                        "Vehicle Type code is blank",
                        new TaxExemptionEntity(RULE_SET_ID, TaxExemptionType.VEHICLE_TYPE, null, null, null, " "),
                        "VEHICLE_TYPE"),
                Arguments.of(
                        "Vehicle Type row has another typed value",
                        new TaxExemptionEntity(
                                RULE_SET_ID, TaxExemptionType.VEHICLE_TYPE, (short) 6, null, null, "BUS"),
                        "VEHICLE_TYPE"));
    }

    private void assertInvalidStoredTaxRuleOptions(List<TaxRuleOptionEntity> options, String optionTypeCode) {
        givenCompleteTaxRuleSet(
                List.of(taxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), "8.00")), options, List.of());

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet(CITY_CODE))
                .isInstanceOfSatisfying(
                        InvalidTaxRuleOptionException.class,
                        exception -> assertThat(exception.optionTypeCode()).isEqualTo(optionTypeCode));
    }

    private void assertInvalidStoredTaxExemptions(List<TaxExemptionEntity> taxExemptions, String taxExemptionTypeCode) {
        givenCompleteTaxRuleSet(
                List.of(taxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), "8.00")), List.of(), taxExemptions);

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet(CITY_CODE))
                .isInstanceOfSatisfying(
                        InvalidTaxExemptionException.class, exception -> assertThat(exception.taxExemptionTypeCode())
                                .isEqualTo(taxExemptionTypeCode));
    }
}
