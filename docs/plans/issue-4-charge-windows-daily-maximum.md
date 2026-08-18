# Issue 4: Calculate Charge Windows and apply the Daily Maximum

> This document records the issue 4 plan. Issue 5 supersedes its City Local Time date boundary. A Charge Window can cross midnight, and ADR-0009 replaces effective-dated Tax Rule Sets with one Tax Rule Set for each City.

## Purpose

Issue 4 extends the first calculation path from one Passage to several Passages. It loads optional Tax Rule Options, calculates one Daily Tax for each City Local Time date, and records the accepted Passage count.

The source issue is [GitHub issue 4](https://github.com/igrgin/congestion-tax-calculator/issues/4). The implementation branch is `4-charge-windows-daily-maximum`.

Read `CONTEXT.md`, `CONTRIBUTING.md`, and `docs/design/README.md` before implementation.

## Confirmed scope

This issue implements:

- stored `CHARGE_WINDOW` and `DAILY_MAXIMUM` Tax Rule Options;
- typed immutable Domain values for these options;
- bulk loading of options for the selected Tax Rule Sets;
- several Passages in one calculation request;
- Passage ordering by instant;
- Daily Tax grouping by City Local Time date;
- Tax Time Band selection for each Passage;
- an anchored Charge Window with an inclusive configured boundary;
- a zero Tax Amount Passage that starts or participates in a Charge Window;
- the highest Tax Amount from each Charge Window;
- one charge for each Passage when Charge Window behavior is absent;
- a configured Daily Maximum after Passage charges are added;
- an uncapped Daily Tax when Daily Maximum behavior is absent;
- a stable Daily Tax order by date;
- the `congestion.tax.calculation.passages` distribution;
- safe option-decision and stored-content log events;
- focused and public tests for the issue behavior;
- updates to the affected design documents.

Issue 5 still owns complete supported-year validation, successive Tax Rule Set coverage, and the complete mixed-currency failure contract. Issue 6 owns Tax Exemptions. Issue 8 owns the complete Gothenburg Tax Rule seed and the complete supplied-list result.

The current Flyway seed stays as staged content in this issue. Tests use synthetic stored content when they must enable or disable a Tax Rule Option.

## Project language

`CONTEXT.md` defines these terms:

- A Charge Window starts with its first Passage and includes each Passage no later than its configured duration after that first Passage.
- A Daily Maximum is a Tax Rule Option with a positive Tax Amount that limits the Daily Tax after the application calculates the Passage charges.

Use these terms in code, tests, logs, and documents.

## Module interfaces and test seams

Use these existing module interfaces as the approved test seams:

- `TaxRuleService.getApplicableTaxRuleSets` for stored option loading;
- `TaxCalculator.calculate` for pure Daily Tax behavior;
- the calculation HTTP operation for public behavior.

Do not test private methods, JPA implementation details, exact SQL, or log output.

## Domain model

`TaxRuleSet` owns one non-null `TaxRuleOptions` value.

`TaxRuleOptions` is a deep module for optional scalar Tax Rules. It owns typed option values and exposes named queries:

```text
chargeWindow() -> Optional<ChargeWindow>
dailyMaximum() -> Optional<DailyMaximum>
```

The implementation does not use `Optional` for fields or constructor parameters. It can receive an unmodifiable collection of typed options, reject duplicate types, and return `Optional` results from its query methods.

Use these typed values:

```text
ChargeWindow
  duration: Duration

DailyMaximum
  amount: TaxAmount
```

`ChargeWindow` rejects a null, zero, or negative duration. `DailyMaximum` rejects a null or zero Tax Amount. `TaxAmount` already rejects a negative amount and arithmetic between different currencies.

An empty `TaxRuleOptions` value disables both behaviors.

The pure calculator does not know persistence option codes or nullable persistence columns.

## Persistence and mapping

The existing schema remains the source for option storage:

```text
TAX_RULE_OPTION_TYPE
TAX_RULE_OPTION
```

Add a JPA entity, a matching Java enum for the stored type code, and a narrow repository operation. The repository loads all options for the selected Tax Rule Set IDs in one operation.

`TaxRuleServiceImpl`:

1. selects the Applicable Tax Rule Set for each calculation date;
2. collects the selected Tax Rule Set IDs;
3. bulk-loads Tax Time Bands and Tax Rule Options;
4. validates the complete stored content;
5. maps storage values to immutable Domain values;
6. returns the complete Tax Rule Set map.

Map `CHARGE_WINDOW.duration_minutes` to a positive `Duration`. Map `DAILY_MAXIMUM.amount` to a positive `TaxAmount` in the Tax Rule Set currency. Row absence disables the matching behavior.

The database constraints remain the first check for stored value shape. Service mapping must also fail safely if loaded content is invalid.

## Daily Tax calculation

`TaxCalculator.calculate` performs this sequence:

1. validates the calculation inputs;
2. groups Passages by City Local Time date;
3. orders the groups by date;
4. sorts each date's Passages by instant;
5. selects the Applicable Tax Rule Set for the date;
6. finds the Tax Time Band amount for each Passage;
7. uses zero in the Tax Rule Set currency when no band matches;
8. applies Charge Window behavior when it is present;
9. adds the Passage charges;
10. applies the Daily Maximum when it is present;
11. creates one Daily Tax for the date;
12. adds the ordered Daily Taxes to the result total.

For Charge Window behavior:

- the first Passage anchors the window;
- the boundary uses actual elapsed time between Passage instants;
- a Passage exactly at the configured duration stays in the window;
- a later Passage does not move the boundary;
- the first Passage after the boundary starts the next window;
- every Passage participates, including a repeated Passage or a Passage with zero Tax Amount;
- only the highest Tax Amount in the window contributes to the Daily Tax;
- a Charge Window cannot cross a City Local Time date.

When Charge Window behavior is absent, each Passage supplies one charge. A zero charge does not change the sum.

Apply the Daily Maximum after all Charge Window charges are added. When Daily Maximum behavior is absent, do not cap the result.

## HTTP behavior

Remove the temporary one-Passage restriction. Bean Validation continues to reject a null or empty Passage list and a null Passage value. The controller parses every Passage as City Local Time and sends an unmodifiable list to the Calculation Service.

This issue can receive Passages from several City Local Time dates. The result contains one Daily Tax for each date in ascending order. Complete date validation remains in issue 5.

## Logging and failures

For each Applicable Tax Rule Set, the Tax Rule module writes one `DEBUG` event with this safe context:

```text
cityCode
calculationDate
effectiveFrom
chargeWindowEnabled
dailyMaximumEnabled
```

The event does not contain a Tax Amount or Charge Window duration.

Invalid stored Charge Window or Daily Maximum content throws one specific Tax Rule module exception. The HTTP exception boundary uses this stable failure category:

```text
invalid-tax-rule-option
```

The `ERROR` event can include the safe option type code and the exception stack trace. It does not include a stored amount, duration, SQL value, or exception message in the HTTP response.

The existing calculation start, successful completion, timer outcome, and suppressed metric-failure events keep their agreed levels and owners.

## Passage-count metric

Add one distribution summary:

```text
congestion.tax.calculation.passages
```

Configure these boundaries:

```text
1
10
100
1000
10000
```

Record the Passage count once for every request that passes HTTP validation and reaches `CalculationService`. Record it even when a later City lookup, Vehicle Type lookup, stored-content operation, or calculation fails.

The meter has no request-value or outcome tags. A meter failure cannot change the calculation result. `CalculationMetrics` catches the failure and logs it at `WARN`.

The implementation does not retain automated tests for metrics.

## Implementation groups

### Group 1: Stored Tax Rule Options

This group adds:

- the typed Domain option values;
- `TaxRuleSet` option ownership;
- the option JPA entity, enum, and repository;
- one bulk option load;
- option validation and mapping;
- the option-decision `DEBUG` event;
- the safe stored-option failure category;
- focused Tax Rule Service and PostgreSQL tests;
- applicable persistence and testing document updates.

The approved main test seam is `TaxRuleService.getApplicableTaxRuleSets`.

### Group 2: Complete Daily Tax

This group adds:

- several-Passage HTTP acceptance;
- date grouping and stable ordering;
- instant ordering within each date;
- complete Tax Time Band boundary and seconds cases;
- present and absent Charge Window behavior;
- configured inclusive window boundaries;
- non-sliding windows;
- zero-amount and repeated Passage behavior;
- present and absent Daily Maximum behavior;
- focused calculator, controller, and public calculation tests;
- applicable calculation, API, and testing document updates.

The approved main test seams are `TaxCalculator.calculate` and the calculation HTTP operation.

### Group 3: Passage workload signal

This group adds:

- the Passage-count distribution and boundaries;
- count recording for each accepted Calculation Service call;
- metric-failure suppression;
- final metric, architecture, testing, and operations document updates.

The user chose not to retain automated tests for metrics. Group 3 keeps the production metric and failure suppression.

## Checkpoint flow

Each group moves through these checkpoints as one approval unit.

### Red

1. Add the first failing tracer test at the approved seam.
2. Run its focused test command and prove that it fails for the expected missing behavior.
3. Show the changed code with full file context when practical.
4. Explain the failed behavior and stop for user direction.

After the red checkpoint, continue with one test and one minimum implementation change at a time. Do not add all tests before implementation. Do not refactor during the red-green loop.

### Green

1. Complete all behavior in the group through small red-green cycles.
2. Run focused tests regularly.
3. Compile regularly.
4. Apply the project formatter.
5. Show all group changes with full file context when practical.
6. Report focused verification and stop for user direction.

Group 3 runs `./mvnw verify` as part of its green checkpoint. This is the one complete suite run at the end of issue implementation.

### Code review

1. Create one local coherent commit that includes issue number `#4`.
2. Compare the group commit with the commit at which the group started.
3. Run the Standards and Spec reviews in parallel with the `code-review` skill.
4. Report the two axes separately.
5. Fix accepted findings, amend the group commit, rerun affected verification, and repeat the review when necessary.
6. Stop for user approval.

### Approval and push

Push the branch only after the user approves the reviewed group. The next group starts from the preceding approved group commit.

The fixed point for Group 1 is the local preparation commit. The fixed point for each later group is the preceding approved group commit.

## Verification

Use focused commands during implementation. Select the command from the changed seam, for example:

```text
./mvnw -Dtest=TaxRuleServiceImplTest test
./mvnw -Dtest=TaxCalculatorTest test
./mvnw -Dtest=CalculationControllerTest test
./mvnw -Dit.test=TaxRuleServiceITest verify -Dskip.regular.tests=true
./mvnw -Dit.test=CalculationITest verify -Dskip.regular.tests=true
```

Run the complete verification once at the end:

```text
./mvnw verify
```

## Completion checklist

- Stored Charge Window and Daily Maximum values cross the Tax Rule Service seam as typed Domain values.
- Absence of either option disables only its matching behavior.
- Invalid stored option content fails with safe context at the shared `ERROR` boundary.
- Several unsorted and repeated Passages produce stable ordered Daily Taxes.
- Charge Windows use an anchored inclusive configured boundary and actual elapsed time.
- Zero-amount Passages participate in Charge Window grouping.
- The highest Tax Amount in each Charge Window contributes to the Daily Tax.
- Daily Maximum behavior applies after Passage charges are added.
- The Passage-count distribution has the five required boundaries and no request-value tags.
- Metric failures do not change calculation results.
- Focused tests and the final `./mvnw verify` pass.
- Each affected design document agrees with the implementation.
- Every group passes its Standards and Spec reviews.
- The user approves each group before push.
