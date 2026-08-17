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

Application logging remains active when a test uses a Spring profile. Test code does not write log messages, and tests do not assert log output. Metric tests use the highest practical public seam and assert only the required meter, tags, and boundaries.

## Current domain tests

`TaxAmountTest` proves:

- addition;
- minimum selection;
- zero creation;
- rejection of null values;
- rejection of negative amounts;
- rejection of arithmetic between different currencies.

`TaxTimeBandTest` proves:

- inclusive start;
- exclusive end;
- exclusion outside the band;
- rejection of null values;
- rejection of an end equal to the start;
- rejection of an end before the start;
- rejection of a non-positive Tax Amount.

`TaxRuleSetTest` proves:

- rejection of null fields;
- rejection of an empty Tax Time Band list.

`DailyTaxTest` proves that the convenience constructor uses an empty Tax Exemption Reason set.

`TaxCalculatorTest` proves:

- one taxed Passage;
- zero Tax outside the Tax Time Bands;
- rejection of an empty Passage list;
- rejection of an empty Applicable Tax Rule Set map;
- rejection of a missing Applicable Tax Rule Set for the Passage date;
- rejection of null calculation inputs.

## Current service and controller tests

`TaxRuleServiceImplTest` proves:

- rejection of an unknown City;
- Vehicle Type loading;
- rejection of an unknown Vehicle Type;
- Applicable Tax Rule Set selection;
- loading of adjacent Tax Time Bands;
- rejection of a missing Applicable Tax Rule Set;
- rejection of a Tax Rule Set without Tax Time Bands;
- rejection of overlapping Tax Time Bands independent of repository order.

`CalculationServiceImplTest` proves:

- coordination of Passages, stored Tax Rules, and the pure calculator;
- translation of unknown City and Vehicle Type failures into calculation-owned exceptions while preserving their causes;
- the `rejected` metric outcome for known lookup failures;
- the `failed` metric outcome for an unexpected failure.

`CalculationControllerTest` proves:

- the one-Passage HTTP request and response mapping;
- rejection of a null or blank Vehicle Type;
- rejection of a null or empty Passage list;
- rejection of a null Passage value;
- rejection of multiple Passages;
- derivation of winter and summer instants with the request IANA time zone;
- rejection of an invalid IANA time zone;
- rejection of a Passage timestamp that does not use `uuuu-MM-dd HH:mm:ss`.

The controller test uses the `test` profile. It does not connect to PostgreSQL.

## Schema integration test

`TaxRuleSchemaITest` starts Spring Boot with PostgreSQL through Testcontainers. It inserts synthetic schema-test data. It does not test the Gothenburg assignment values.

The test proves:

- valid rows can be inserted;
- City codes are unique;
- a Tax Rule Set must reference a known City;
- a City cannot have two Tax Rule Sets with the same effective date;
- a Tax Time Band must reference a known Tax Rule Set;
- a Tax Time Band amount must be positive;
- a Tax Time Band end must be after its start;
- an exact Tax Time Band duplicate is rejected;
- a Tax Rule Option must reference a known Tax Rule Set;
- a Tax Rule Set cannot select the same option type twice;
- each Tax Rule Option has the correct positive value shape.

Cross-row Tax Time Band overlap is a Tax Rule Service check. It is not a database constraint.

## Full-path integration test

`CalculationITest` starts the complete Spring Boot application with a temporary PostgreSQL database. Flyway creates the schema and inserts the current Gothenburg seed data.

The test sends:

```json
{
  "vehicleType": "OTHER",
  "timeZone": "Europe/Stockholm",
  "passages": [
    "2013-02-08 06:20:27"
  ]
}
```

The Passage supplies City Local Time `2013-02-08T06:20:27`. The stored `06:00–06:30` Tax Time Band produces `8.00 SEK`. The request time zone derives the instant `2013-02-08T05:20:27Z` for ordering and elapsed-time calculations.

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

This test proves that HTTP parsing, request time-zone handling, Flyway data, JPA loading, Tax calculation, and JSON output work together.

It also verifies that Prometheus publishes the calculation timer with the bounded `success` outcome.

The test also verifies:

- a Passage outside the stored Tax Time Bands returns zero Tax;
- invalid request bodies return HTTP `400`;
- an unknown City returns HTTP `404`;
- an unknown Vehicle Type returns HTTP `400`.

## Tax Rule Service integration test

`TaxRuleServiceITest` starts the application with a temporary PostgreSQL database and calls the real Tax Rule Service. It uses synthetic rows to verify:

- stored Vehicle Type lookup;
- Applicable Tax Rule Set selection by City and calculation date;
- isolation between Cities;
- rejection of a selected Tax Rule Set with no Tax Time Bands;
- rejection of overlapping Tax Time Bands.

An `@AfterEach` method removes the synthetic rows. The test does not use a test transaction or mock repositories.

## Actuator integration test

`ActuatorITest` verifies that:

- application health is `UP`;
- database health is `UP`;
- health component details are hidden;
- Prometheus publishes standard JVM, process, HTTP, and connection-pool metrics.

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

- multiple Passage ordering and date grouping;
- Charge Window boundaries and non-sliding behavior;
- zero-amount Passages in a Charge Window;
- repeated Passages;
- weekday, month, public-holiday, and preceding-date Tax Exemptions;
- Vehicle Type Tax Exemptions;
- Daily Tax limits;
- missing optional Tax Rules;
- successive Applicable Tax Rule Sets;
- mixed currencies in one calculation;
- complete transport validation and Problem Details;
- a second City with different stored Tax Rules.

The complete assignment full-path test will be added when the related calculation behavior and seed data exist.
