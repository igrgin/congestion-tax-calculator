# Issue 8: Verify supplied and alternative City data end to end

## Purpose

Issue 8 completes the stored assignment content and verifies the application with the supplied Passage list. It also verifies that a second City can use different stored Tax Rules without a Java application change.

The source issue is [GitHub issue 8](https://github.com/igrgin/congestion-tax-calculator/issues/8).

Read `CONTEXT.md`, `CONTRIBUTING.md`, and `docs/design/README.md` before implementation.

## Reassessment

Earlier issues implemented the calculation behavior and most focused tests. Issue 8 does not repeat those tests.

The existing suite already verifies:

- Tax Time Band boundaries and second precision;
- Charge Window grouping, ordering, inclusive duration, and repeated Passages;
- present and absent Charge Window behavior;
- present and absent Daily Maximum behavior;
- weekday, month, public-holiday, preceding-date, and Vehicle Type Tax Exemptions;
- City isolation and typed Tax Rule loading;
- missing or inconsistent stored Tax Rules;
- HTTP `500` responses for stored-content failures;
- HTTP `503` responses for an unavailable database.

Issue 8 adds no duplicate tests for this behavior. It changes production Java only if a new test finds a defect.

## Confirmed scope

Issue 8 adds:

- the complete Gothenburg Tax Rule Set as production Flyway content;
- the complete set of initial Vehicle Types;
- the supplied 16-Passage HTTP calculation for `OTHER`;
- an exact result of six Daily Taxes and `89.00 SEK` total;
- Prometheus checks after the supplied calculation;
- a test-only London City installed by Flyway V5;
- a London HTTP calculation that proves different stored Tax Rules;
- current persistence and testing design documentation.

The request does not contain a time zone. Gothenburg stores `Europe/Stockholm`, and London stores `Europe/London`.

Issue 9 owns the root README and final delivery documentation. Issue 8 does not create the README.

## Complete Gothenburg content

Production migration `V4__complete_gothenburg_tax_rules.sql` completes the existing seed.

It stores these Vehicle Types:

```text
OTHER
EMERGENCY
BUS
DIPLOMAT
MOTORCYCLE
MILITARY
FOREIGN
```

It stores these Gothenburg Tax Time Bands:

```text
06:00-06:30  8.00 SEK
06:30-07:00 13.00 SEK
07:00-08:00 18.00 SEK
08:00-08:30 13.00 SEK
08:30-15:00  8.00 SEK
15:00-15:30 13.00 SEK
15:30-17:00 18.00 SEK
17:00-18:00 13.00 SEK
18:00-18:30  8.00 SEK
18:30-06:00  0.00 SEK
```

The last row is one explicit zero-amount Tax Time Band that crosses midnight. A gap does not mean a zero Tax Amount.

It stores these Tax Rule Options:

```text
CHARGE_WINDOW      60 minutes
DAILY_MAXIMUM      60.00 SEK
HOLIDAY_PRECEDING   1 calendar date
```

It stores Tax Exemptions for Saturday, Sunday, July, and these Vehicle Types:

```text
EMERGENCY
BUS
DIPLOMAT
MOTORCYCLE
MILITARY
FOREIGN
```

It stores the Swedish public holidays in 2013:

```text
2013-01-01
2013-01-06
2013-03-29
2013-03-31
2013-04-01
2013-05-01
2013-05-09
2013-05-19
2013-06-06
2013-06-22
2013-11-02
2013-12-25
2013-12-26
```

The existing `2014-01-01` supporting date remains stored so that the application can evaluate the preceding-date Tax Exemption for `2013-12-31`.

## Supplied calculation

`CalculationITest` sends all 16 assignment timestamps without a change:

```text
2013-01-14 21:00:00
2013-01-15 21:00:00
2013-02-07 06:23:27
2013-02-07 15:27:00
2013-02-08 06:27:00
2013-02-08 06:20:27
2013-02-08 14:35:00
2013-02-08 15:29:00
2013-02-08 15:47:00
2013-02-08 16:01:00
2013-02-08 16:48:00
2013-02-08 17:49:00
2013-02-08 18:29:00
2013-02-08 18:35:00
2013-03-26 14:25:00
2013-03-28 14:07:27
```

The test expects:

```text
2013-01-14   0.00 SEK
2013-01-15   0.00 SEK
2013-02-07  21.00 SEK
2013-02-08  60.00 SEK
2013-03-26   8.00 SEK
2013-03-28   0.00 SEK  DATE_BEFORE_PUBLIC_HOLIDAY
Total       89.00 SEK
```

The test verifies the complete response, including empty Tax Exemption Reason sets for zero amounts that come from the explicit zero Tax Time Band.

## Prometheus checks

After the supplied calculation, `CalculationITest` reads `/actuator/prometheus`.

It checks two standard metric families:

- the successful calculation HTTP request;
- Hikari database-pool connections.

It checks two custom metric families:

- the calculation timer with the bounded `success` outcome;
- the Passage-count histogram with boundaries `1`, `10`, `100`, `1000`, and `10000`.

The test does not compare numeric durations, accumulated counts, generated buckets, bucket order, pool names, JVM identifiers, or log output.

## Test-only London content

Test migration `src/test/resources/db/migration/V5__seed_london_test_tax_rules.sql` stores:

```text
City code: london-test
City time zone: Europe/London
Currency: GBP

Tax Time Bands:
00:00-12:00 4.00 GBP
12:00-00:00 7.00 GBP

Tax Exemption:
Monday

Tax Rule Option:
DAILY_MAXIMUM 10.00 GBP

Absent Tax Rule Options:
CHARGE_WINDOW
HOLIDAY_PRECEDING
```

The London request contains:

```text
2013-02-04 12:15:00
2013-02-05 11:45:00
2013-02-05 12:15:00
```

The test expects:

```text
2013-02-04  0.00 GBP  WEEKDAY
2013-02-05 10.00 GBP
Total      10.00 GBP
```

The Tuesday Passages are 30 minutes apart. Because London has no Charge Window, both Tax Amounts contribute. Their `11.00 GBP` sum is limited to `10.00 GBP` by the stored Daily Maximum.

## Approved test points

Change `CalculationITest` to verify:

- the complete supplied Gothenburg calculation;
- two standard and two custom Prometheus metric families;
- the London calculation.

Change `TaxRuleServiceITest` to verify:

- all seeded Gothenburg Vehicle Types;
- the complete seeded Gothenburg Tax Rule Set;
- the seeded London time zone and Tax Rule Set;
- the absent London Charge Window and Public Holiday Preceding-Date Option.

Remove the existing Gothenburg `06:40` missing-band calculation test. The complete Gothenburg content makes that time taxable. Existing focused tests retain the missing-band coverage.

Do not test private methods, generated SQL, JPA implementation details, log output, metric values, or metric line order.

## Implementation groups

### Group 1: Complete and verify Gothenburg

Red:

1. Add the supplied calculation and Prometheus checks to `CalculationITest`.
2. Add the complete Gothenburg content checks to `TaxRuleServiceITest`.
3. Remove the obsolete Gothenburg missing-band calculation test.
4. Run the focused integration tests and confirm that the new acceptance checks fail.
5. Commit and push the red checkpoint.

Green:

1. Add production Flyway migration V4.
2. Make only the additional changes that the failing tests require.
3. Update the persistence and testing design documents.
4. Run focused verification and `./mvnw verify`.
5. Commit and push the green checkpoint.

### Group 2: Verify London stored content

Red:

1. Add the London calculation to `CalculationITest`.
2. Add the London content checks to `TaxRuleServiceITest`.
3. Run the focused integration tests and confirm that the London checks fail because the City is absent.
4. Commit and push the red checkpoint.

Green:

1. Add the test-only Flyway V5 migration.
2. Make only the additional changes that the failing tests require.
3. Update the testing design document.
4. Run focused verification and `./mvnw verify`.
5. Commit and push the green checkpoint.

## Branch, pull request, and review

All work uses branch `8-verify-supplied-and-alternative-city-data-end-to-end` and one pull request to `main`.

After preparation approval:

1. Commit this plan.
2. Push the branch.
3. Open one draft pull request.
4. Push every later checkpoint commit to the same branch and pull request.

After both groups are green, the primary agent performs one two-axis review in the current task:

- Standards against `CONTRIBUTING.md`, applicable design records, and the code-smell baseline;
- Spec against GitHub issue 8 and this approved plan.

The review does not use a side agent. Fix accepted findings, rerun verification, and update the same pull request. The user gives final approval before the pull request becomes ready for review.

## Verification

Run focused tests during each group. Run the full build after each green checkpoint and after review fixes:

```text
./mvnw verify
git diff --check
```

Implementation is complete when:

- Flyway installs all required Gothenburg content;
- the supplied Passage list returns the agreed six Daily Taxes and `89.00 SEK` total;
- the Prometheus scrape contains the agreed standard and custom metric evidence;
- Flyway V5 installs the test-only London content;
- the London calculation returns the agreed GBP result;
- no production Java change exists unless a test found a defect;
- all affected documentation is current;
- the final two-axis review has no unresolved finding;
- the full Maven verification passes.
