# Testing Design

The test suite uses a small number of tests that prove distinct behavior, boundaries, and failures. It does not repeat the same behavior only to increase a coverage number. The project has no required coverage percentage.

## Test levels

The project uses these test levels:

- pure domain tests;
- service tests with mocked repositories or collaborators;
- controller tests with a mocked Calculation Service;
- schema integration tests with PostgreSQL;
- full-path Spring Boot integration tests.

Tests call public operations. They do not test private methods.

Application logging remains active when a test uses a Spring profile. Test code does not write log messages, and tests do not assert log output.

## Current domain test responsibilities

`TaxAmountTest` proves:

- addition;
- minimum selection;
- maximum selection;
- greater-than comparison;
- zero creation;
- rejection of null values;
- rejection of negative amounts;
- rejection of arithmetic between different currencies.

`TaxTimeBandTest` proves:

- inclusive start;
- exclusive end;
- exclusion outside the band;
- rejection of null values;
- matching every City Local Time when the start and end are equal;
- matching on both sides of midnight when the end is before the start;
- acceptance of a zero Tax Amount.

`TaxRuleSetTest` proves:

- rejection of null fields;
- rejection of an empty Tax Time Band list;
- each stored Tax Exemption match;
- all matching Tax Exemption Reasons in stable order;
- an unmodifiable reason set;
- present and absent Public Holiday Preceding-Date Option behavior;
- inert Public Holiday Preceding-Date Option behavior when no public-holiday Tax Exemption exists;
- the first and last preceding-date boundaries;
- overlapping public-holiday reasons for consecutive holidays.

`TaxExemptionsTest` proves rejection of null and duplicate Tax Exemption values. It also proves that a Vehicle Type Tax Exemption rejects a blank Vehicle Type code.

`PublicHolidayPrecedingDateOptionTest` proves rejection of a zero or negative calendar-date count.

`ChargeWindowTest` proves rejection of a null, zero, or negative duration.

`DailyMaximumTest` proves rejection of a null or zero Tax Amount.

`TaxRuleOptionsTest` proves typed Public Holiday Preceding-Date Option lookup and rejection of duplicate option types.

`DailyTaxTest` proves that the convenience constructor uses an empty Tax Exemption Reason set.

`TaxCalculatorTest` proves:

- one taxed Passage;
- each initial exempt Vehicle Type;
- a known Vehicle Type without a matching Tax Exemption remains taxable;
- each calendar Tax Exemption type;
- present and absent Public Holiday Preceding-Date Option behavior;
- all matching Tax Exemption Reasons;
- zero Daily Tax and total Tax Amount for an exempt date or Vehicle Type;
- Tax Exemption evaluation before Tax Time Band selection, including an exempt Passage outside all Tax Time Bands;
- addition of all Passage Tax Amounts when the Charge Window is absent;
- date grouping and ascending Daily Tax order;
- one Charge Window across a City Local Time midnight;
- assignment of a cross-date window charge to the winning Passage's date;
- an earliest-Passage tie break for equal highest Tax Amounts;
- Passage instant ordering and highest Tax Amount selection in a Charge Window;
- the inclusive configured Charge Window boundary;
- non-sliding Charge Window behavior;
- participation of Passages in explicit zero-amount Tax Time Bands, exempt Passages, and repeated Passages in Charge Windows;
- Daily Maximum application after Charge Window calculation;
- Tax Time Band selection at second precision, including adjacent boundaries;
- rejection of a non-exempt Passage before the first Tax Time Band and at an exclusive Tax Time Band end when no adjacent band contains it;
- rejection of an empty Passage list;
- rejection of null calculation inputs.

## Current service and controller test responsibilities

`TaxRuleServiceImplTest` proves:

- rejection of an unknown City;
- loading and validation of the stored City time zone;
- rejection of an invalid stored City time zone;
- Vehicle Type loading;
- rejection of an unknown Vehicle Type;
- Tax Rule Set loading for the selected City;
- loading of adjacent Tax Time Bands;
- loading of a cross-midnight Tax Time Band;
- rejection of a missing Tax Rule Set;
- rejection of a Tax Rule Set without Tax Time Bands;
- rejection of overlapping Tax Time Bands independent of repository order;
- rejection of a nested Tax Time Band;
- rejection of a full-day Tax Time Band combined with another band;
- rejection of two full-day Tax Time Bands;
- use of `OverlappingTaxTimeBandsException` for every overlap shape;
- failure on the first conflicting pair;
- safe rejection of invalid stored Charge Window and Daily Maximum content;
- safe rejection of invalid stored Public Holiday Preceding-Date Option content;
- safe rejection of duplicate stored Tax Rule Option types;
- typed mapping of each stored Tax Exemption Type;
- safe rejection of invalid shapes, ranges, missing types, and duplicate stored Tax Exemptions;
- loading a Public Holiday Preceding-Date Option when no public-holiday Tax Exemption exists.

This test stays in the `taxrule.persistence` test package because it uses package-access entity constructors to prepare repository results. It calls the implementation through the `TaxRuleService` interface.

`CalculationServiceImplTest` proves:

- coordination of Passages, stored Tax Rules, and the pure calculator;
- acceptance of City Local Times `2013-01-01 00:00:00` and `2013-12-31 23:59:59` as the supported-year boundaries;
- collection of every unsupported-year Passage index before Tax Rule lookup, calculation logs, and custom metrics;
- derivation of winter and summer instants with the stored City time zone;
- translation of unknown City and Vehicle Type failures into calculation-owned exceptions while preserving their causes;
- translation of an invalid stored Tax Rule Option into a calculation-owned failure with its safe type code;
- translation of `MissingTaxTimeBandsException` and `NoMatchingTaxTimeBandException` to `MissingStoredTaxTimeBandsException`, with the original cause preserved;
- translation of `InvalidCityTimeZoneException` to `InvalidStoredCityTimeZoneException`;
- translation of `OverlappingTaxTimeBandsException` to `InvalidStoredTaxTimeBandsException`;
- translation of invalid stored Tax Exemption content into a calculation-owned failure with its safe type code.

`CalculationControllerTest` proves:

- the one-Passage HTTP request and response mapping;
- the several-Passage HTTP request and response mapping;
- rejection of a null or blank Vehicle Type;
- rejection of a null or empty Passage list;
- rejection of a null Passage value;
- strict deserialization of exact `uuuu-MM-dd HH:mm:ss` Passage string tokens before the controller method runs;
- rejection of values with leading or trailing whitespace and other Jackson `LocalDateTime` shapes;
- rejection of an invalid second Passage before the Calculation Service runs;
- mapping of a timestamp deserialization failure to an HTTP `400` Problem Details response for the first invalid Passage;
- rejection of unknown JSON properties, including a Problem Details response for the removed `timeZone` property;
- mapping of malformed JSON to an HTTP `400` Problem Details response without `errors`;
- forwarding of `List<LocalDateTime>` to `CalculationCommand`;
- mapping of an unsupported-year service exception to one HTTP `400` Problem Details response that reports all affected zero-based Passage indexes in request order;
- the top-level `INVALID_REQUEST` code and the `UNSUPPORTED_PASSAGE_YEAR` code for each affected Passage;
- inclusion of `errors` only when at least one specific error exists;
- a Problem Details response for each handled `400`, `404`, `500`, and `503` failure;
- the safe `CALCULATION_FAILED` response for invalid stored content and unexpected failures;
- all matching Tax Exemption Reasons in stable order with zero Daily and total Tax Amounts;
- a safe HTTP `500` response for invalid stored Tax Exemption content.

The `503` cases include a data-access resource failure and a PostgreSQL transaction-start failure.

The controller test uses the `test` profile. It does not connect to PostgreSQL.

## Schema integration test

`TaxRuleSchemaITest` starts Spring Boot with PostgreSQL through Testcontainers. It inserts synthetic schema-test data. It does not test the Gothenburg assignment values.

The test proves:

- valid rows can be inserted;
- City codes are unique;
- a City time zone is required and must not be blank;
- a Tax Rule Set must reference a known City;
- a City cannot have two Tax Rule Sets;
- a Tax Time Band must reference a known Tax Rule Set;
- a Tax Time Band amount must not be negative;
- a Tax Time Band can have equal start and end times for a full day;
- a Tax Time Band can have an end before its start;
- an exact Tax Time Band duplicate is rejected;
- a same-date partial overlap is rejected;
- a nested Tax Time Band is rejected;
- a cross-midnight overlap is rejected;
- a full-day Tax Time Band combined with another band is rejected;
- two full-day Tax Time Bands are rejected;
- a Tax Rule Option must reference a known Tax Rule Set;
- a Tax Rule Set cannot select the same option type twice;
- each Tax Rule Option has the correct positive value shape;
- the closed Tax Exemption Type vocabulary;
- valid typed Tax Exemption rows;
- exact Tax Exemption value shapes and weekday and month ranges;
- Tax Exemption foreign keys;
- duplicate rejection for each Tax Exemption Type.

PostgreSQL uses a GiST exclusion constraint to reject every Tax Time Band overlap shape in one Tax Rule Set. The schema test proves this constraint directly.

## Full-path integration test

`CalculationITest` starts the complete Spring Boot application with a temporary PostgreSQL database. Flyway creates the schema and inserts the current Gothenburg seed data.

The test sends:

```json
{
  "vehicleType": "OTHER",
  "passages": [
    "2013-02-08 06:20:27"
  ]
}
```

The Passage supplies City Local Time `2013-02-08T06:20:27`. The stored `06:00–06:30` Tax Time Band produces `8.00 SEK`. The stored City time zone `Europe/Stockholm` derives the instant `2013-02-08T05:20:27Z` for ordering and elapsed-time calculations.

The test verifies this response:

```json
{
  "cityCode": "gothenburg",
  "vehicleType": "OTHER",
  "currency": "SEK",
  "totalAmount": 8.00,
  "dailyTaxes": [
    {
      "date": "2013-02-08",
      "taxExemptionReasons": [],
      "amount": 8.00
    }
  ]
}
```

This test proves that HTTP parsing, stored City time-zone handling, Flyway data, JPA loading, Tax calculation, and JSON output work together.

The test also verifies:

- several Passages are calculated from stored Tax Rules;
- a non-exempt Passage outside the stored Tax Time Bands returns a safe HTTP `500` Problem Details response with `CALCULATION_FAILED`;
- invalid request bodies return HTTP `400`;
- an unknown City returns HTTP `404`;
- an unknown Vehicle Type returns HTTP `400`.

## Tax Rule Service integration test

`TaxRuleServiceITest` starts the application with a temporary PostgreSQL database and calls the real Tax Rule Service. It uses synthetic rows to verify:

- stored City time-zone loading;
- stored Vehicle Type lookup;
- Tax Rule Set loading for the selected City;
- isolation between Cities;
- stored Charge Window loading as a typed Domain value;
- stored Daily Maximum loading as a typed Domain value in the Tax Rule Set currency;
- stored Public Holiday Preceding-Date Option loading as a typed Domain value;
- stored weekday, month, public-holiday, and Vehicle Type Tax Exemption loading as typed Domain values;
- an empty Tax Exemption collection when no rows exist;
- rejection of a selected Tax Rule Set with no Tax Time Bands;
- loading of valid non-overlapping Tax Time Bands.

An `@AfterEach` method removes the synthetic rows. The test does not use a test transaction or mock repositories.

## Actuator integration test

`ActuatorITest` verifies that:

- application health is `UP`;
- database health is `UP`;
- health component details are hidden.

## Test execution

Maven Surefire runs regular test classes with the suffix `Test`. It excludes classes with the suffix `ITest`.

Maven Failsafe runs integration test classes with the suffix `ITest`.

Use:

```text
./mvnw test
```

to run regular tests.

Use:

```text
./mvnw verify
```

to run regular tests, integration tests, and build checks.

Tests that start Spring without PostgreSQL use the `test` profile. Full Spring Boot and PostgreSQL integration tests use the `itest` profile.

## Later test coverage

Later calculation issues will add focused tests for:

- remaining validation-detail aggregation and API documentation;
- a second City with different stored Tax Rules.

Issue 5 owns the one-Tax-Rule-Set tests, Tax Time Band boundary and overlap tests, cross-date Charge Window tests, and the shared calculation Problem Details structure.

The complete assignment full-path test will be added when the related calculation behavior and seed data exist.
