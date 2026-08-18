# Issue 5: Use City Local Time and one City Tax Rule Set

## Purpose

Issue 5 completes City Local Time calculation across dates and simplifies stored Tax Rules to match the assignment. Each City has one Tax Rule Set. A Tax Time Band and a Charge Window can cross midnight.

The source issue is [GitHub issue 5](https://github.com/igrgin/congestion-tax-calculator/issues/5).

Read `CONTEXT.md`, `CONTRIBUTING.md`, and `docs/design/README.md` before implementation.

## Confirmed scope

This issue keeps the completed request behavior:

- strict Passage deserialization from exact `uuuu-MM-dd HH:mm:ss` JSON strings;
- typed City Local Time values through `CalculationCommand`;
- Passage instant derivation with the selected City's stored IANA time zone;
- complete rejection of Passage dates outside 2013;
- Problem Details responses for invalid Passage timestamps, malformed JSON, and unsupported Passage years.

This issue changes stored Tax Rules and multi-date calculation:

- one Tax Rule Set for each City;
- no effective date, history, snapshot selection, inheritance, or future publication behavior;
- one Tax Time Band can cross midnight without a new database column;
- a Tax Time Band can have a zero Tax Amount;
- equal Tax Time Band start and end times are invalid;
- Tax Time Band overlap validation covers the complete 24-hour clock;
- one Charge Window can contain Passages from different City Local Time dates;
- the highest Passage Tax Amount in a Charge Window is charged once;
- the earliest Passage wins when several Passages have the same highest amount;
- the charge belongs to the winning Passage's City Local Time date;
- every distinct input date produces one ordered Daily Tax;
- the Daily Maximum applies after window charges are assigned and added by date;
- safe failure handling for a missing City Tax Rule Set;
- pre-release Flyway migrations that match the simplified schema.

Issue 6 owns Vehicle Type and calendar Tax Exemptions. It will rebase after issue 5 merges. The calculator design applies Tax Exemptions before Charge Window selection. An exempt Passage has a zero Tax Amount but still participates in the window.

Multiple effective-dated Tax Rule Sets remain an optional feature. The assignment and its six-hour limit do not require it.

## Project language

Use these terms from `CONTEXT.md`:

- City Local Time;
- Passage;
- Tax Rule Set;
- Tax Time Band;
- Charge Window;
- Daily Tax;
- Tax Amount.

Do not use `Applicable Tax Rule Set`, `snapshot`, or `effective date` for the implemented model. The selected City supplies the time zone and its one Tax Rule Set.

## Tax Rule storage

`TAX_RULE_SET` keeps its identifier, City foreign key, and currency code. Remove `effective_from`. A unique `city_id` permits at most one Tax Rule Set for each City.

`TAX_TIME_BAND` keeps the existing fields:

```text
start_time TIME WITHOUT TIME ZONE
end_time TIME WITHOUT TIME ZONE
amount NUMERIC(12,2)
```

The boundary convention does not change. The start is included and the end is excluded.

Under this existing convention, the supplied `18:30–05:59` display range uses stored boundaries `18:30:00` and `06:00:00`.

- An end after the start defines a same-date band.
- An end before the start defines one band that crosses midnight.
- An end equal to the start is invalid.
- The amount can be zero or positive.

For a cross-midnight band, `TaxTimeBand.includes` matches a local time when it is on or after the start or before the end.

The Tax Rule Service rejects overlaps around the complete clock. Adjacent bands and gaps remain valid. A gap produces a zero Tax Amount in the Tax Rule Set currency.

Issue 5 rewrites the initial Flyway migrations because the project has no release. A developer must run `docker compose down -v` before using the rewritten schema with an existing development database.

## Tax Rule loading

The Tax Rule Service loads the selected City, validates its stored IANA time zone, and loads the one Tax Rule Set that references the City. It then loads the Tax Time Bands and Tax Rule Options for that set.

Remove:

- calculation-date input from the Tax Rule Service operation;
- candidate and latest-effective-date selection;
- the map of Tax Rule Sets by date;
- `effectiveFrom` from persistence and Domain values;
- missing Applicable Tax Rule Set exceptions;
- the selected Applicable Tax Rule Set `DEBUG` event;
- mixed-snapshot currency handling.

Keep `TaxAmount` currency checks as a Domain invariant. Translate a missing City Tax Rule Set to the calculation boundary and log `missing-tax-rule-set` at `ERROR` with its cause. The HTTP response stays safe and contains no internal failure data.

This issue adds no custom metric.

## Cross-date Charge Windows

The calculator orders all Passages by instant before it creates Charge Windows. It does not group Passages by date first.

For each Charge Window:

1. The first Passage anchors the inclusive duration boundary.
2. Every Passage inside the boundary participates.
3. A later Passage does not extend the boundary.
4. The highest effective Passage Tax Amount wins.
5. If several Passages have that amount, the earliest Passage wins.
6. The winning charge belongs to the winner's City Local Time date.
7. The first Passage after the boundary starts the next window.

Zero Tax Amount and repeated Passages participate. Issue 6 will make exempt Passages participate with a zero Tax Amount.

The calculator creates one Daily Tax for every distinct input date. A date can have zero Tax because its Passage lost a cross-date Charge Window. This case does not add a Tax Exemption Reason. After charge assignment, the calculator adds charges by date and applies the Daily Maximum to each date.

## Approved test seams

Use these public boundaries:

- `TaxTimeBand` for same-date and cross-midnight matching boundaries;
- `TaxCalculator.calculate` for cross-date Charge Windows, winner-date assignment, tie behavior, ordered Daily Taxes, and Daily Maximum order;
- `TaxRuleService` for one-set loading, missing-set handling, child loading, and circular overlap rejection;
- PostgreSQL schema integration for one set per City, zero amounts, cross-midnight boundaries, and equal-boundary rejection;
- the HTTP-to-PostgreSQL operation for the selected City's one Tax Rule Set.

Keep the existing Calculation Service winter and summer time-zone tests. Keep the strict timestamp and supported-year HTTP tests. Remove or replace tests that prove effective-date selection, historical snapshots, future snapshots, mixed snapshot currencies, or midnight Charge Window separation.

Do not test private methods, exact generated SQL, JPA internals, or log output.

## Implementation order

1. Add failing tests at the approved seams.
2. Simplify the Tax Rule Set Domain and Tax Rule Service interfaces.
3. Rewrite the pre-release schema and seed migrations.
4. Add cross-midnight Tax Time Band validation and matching.
5. Change the calculator to create Charge Windows across all ordered Passages.
6. Assign each window charge to the winner's date and apply the Daily Maximum.
7. Translate the missing Tax Rule Set failure and remove obsolete logs.
8. Update affected tests and documents.

## Verification

Run focused tests after each change. Then run:

```text
./mvnw test
./mvnw verify
git diff --check
```

The implementation is complete when:

- each City has at most one Tax Rule Set;
- calculation uses the selected City's one Tax Rule Set for all Passages;
- Tax Time Bands can cross midnight and can contain zero Tax;
- equal Tax Time Band boundaries and circular overlaps are rejected;
- Charge Windows can cross City Local Time dates;
- the winning charge is assigned to the correct date;
- every input date has one ordered Daily Tax;
- the Daily Maximum applies after charge assignment;
- missing Tax Rule Set failures use safe handling;
- all affected tests and documents agree with the design;
- the full Maven verification passes.
