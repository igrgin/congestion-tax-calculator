# Testing Design

The test plan uses a small number of carefully selected tests. It covers each distinct successful path, common path, boundary, and error path. It does not repeat the same behavior only to increase a coverage number. There is no required coverage percentage.

## Pure Calculation Tests

Use JUnit 5 parameterized tests when many inputs prove the same rule. Each case name states the input condition and expected result. Tests call public operations and do not test private methods.

Tests use parameterized SLF4J messages when logs make execution easier to follow. Integration tests log meaningful scenario boundaries and useful failure context. Focused calculation tests log only when the parameterized case name and assertion output are not sufficient. Tests do not add a routine log for every assertion.

Cases cover:

- Each time-band start and exclusive end, including seconds
- Same-date, midnight-crossing, and full-day bands
- A passage exactly at and just after the charge-window boundary
- The non-sliding window rule
- A zero-amount passage that starts a window
- Unsorted and repeated passages
- City-local date grouping at midnight
- Each tax-free weekday, month, holiday, and preceding-holiday date
- Each initial exempt Vehicle Type
- The daily maximum
- Unlimited Daily Tax when `DAILY_MAXIMUM` is absent
- Separate Passage charges when `CHARGE_WINDOW` is absent
- Zero preceding dates when `HOLIDAY_PRECEDING` is absent
- Several dates with successive Tax Rule Set snapshots
- Rejection of mixed currencies in one result

## Test Execution

Maven Surefire runs regular test classes with the suffix `Test` and excludes classes with the suffix `ITest`. Maven Failsafe runs integration test classes with the suffix `ITest`. The `test` phase runs regular tests. The `verify` phase runs both groups and all build checks.

## HTTP and Database Tests

A foundation integration test starts Spring Boot with PostgreSQL 18.4 through Testcontainers. It verifies that `/actuator/health` reports the application and database as `UP`, shows component statuses without details, and that `/actuator/prometheus` supplies standard metrics. The Actuator allowlist exposes only these two endpoints.

One full-path test starts Spring Boot with a temporary PostgreSQL database from Testcontainers. Flyway creates the schema and the initial assignment data. The test sends the complete assignment list through HTTP and verifies these Daily Taxes for `OTHER`:

| Date | Amount in SEK |
|---|---:|
| 2013-01-14 | 0.00 |
| 2013-01-15 | 0.00 |
| 2013-02-07 | 21.00 |
| 2013-02-08 | 60.00 |
| 2013-03-26 | 8.00 |
| 2013-03-28 | 0.00 |
| **Total** | **89.00** |

This test proves that HTTP parsing, Flyway data, JPA loading, time conversion, calculation, and JSON output work together. After the calculation, it scrapes `/actuator/prometheus` and verifies the standard and custom meter families. Focused assertions check only the required meter names, bounded tags, and configured boundaries. They do not compare the complete scrape or volatile metric values. Add a separate repository test only when an important query is not covered through this path. Tests do not require Hibernate to produce an exact SQL string.

A second integration fixture defines a test-only city with a different time zone, currency, time bands, and Tax Exemptions. It omits `DAILY_MAXIMUM`, `CHARGE_WINDOW`, and `HOLIDAY_PRECEDING`. It proves that different database content changes the calculation without a Java code change. Invented rules do not enter the initial seed data. This proof can be completed after the primary assignment path is verified, but the application design must support it from the start.

Error tests cover an unknown city, unknown Vehicle Type, empty or oversized passage list, timestamp without an offset, local date outside 2013, several invalid passages, missing or inconsistent rules, and an unavailable database.
