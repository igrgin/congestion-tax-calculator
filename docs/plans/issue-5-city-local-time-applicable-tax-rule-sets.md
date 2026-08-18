# Issue 5: Calculate by City Local Time and Applicable Tax Rule Sets

## Purpose

Issue 5 completes multi-date calculation behavior. It validates the supported Passage year, uses the stored City time zone, selects one Applicable Tax Rule Set for each City Local Time date, and rejects a result that would combine currencies.

The source issue is [GitHub issue 5](https://github.com/igrgin/congestion-tax-calculator/issues/5). The implementation branch is `5-calculate-by-city-local-time-and-applicable-tax-rule-sets`.

Read `CONTEXT.md`, `CONTRIBUTING.md`, and `docs/design/README.md` before implementation.

## Confirmed scope

This issue implements or proves:

- transport-shape validation in the HTTP controller;
- raw Passage timestamp transfer through `CalculationCommand`, which uses `List.copyOf` to take an unmodifiable copy;
- strict Passage parsing as City Local Time in `uuuu-MM-dd HH:mm:ss` format;
- Passage instant derivation with the selected City's stored IANA time zone;
- rejection of each Passage whose City Local Time date is outside 2013;
- one complete request rejection that reports all unsupported-year Passage indexes;
- one narrow Problem Details response for unsupported-year input;
- Daily Tax grouping by City Local Time date;
- a Charge Window that cannot cross a City Local Time date boundary;
- selection of the latest Tax Rule Set whose effective date is not after each calculation date;
- successive complete Tax Rule Set snapshots that keep historical results stable;
- exclusion of a future Tax Rule Set from an earlier calculation;
- rejection of one calculation that would combine Tax Rule Set currencies;
- safe failure translation for a missing Applicable Tax Rule Set and mixed currencies;
- the required `DEBUG`, `WARN`, and `ERROR` failure categories;
- focused tests at the approved public seams;
- updates to the affected design documents.

Issue 6 owns calendar and Vehicle Type Tax Exemptions. The supported Passage year does not limit stored supporting dates. Stored content can include the public holiday on `2014-01-01` so that issue 6 can evaluate the `DATE_BEFORE_PUBLIC_HOLIDAY` Tax Exemption for `2013-12-31`.

Issue 7 owns complete validation, the remaining Problem Details responses, and OpenAPI. This issue does not change issue 7 or aggregate different validation failure types.

## Project language

Use these terms from `CONTEXT.md`:

- City Local Time;
- Passage;
- Tax Rule Set;
- Applicable Tax Rule Set;
- Charge Window;
- Daily Tax;
- Tax Amount.

Do not use "request time zone" or "effective Tax Rules." The selected City supplies the time zone. The selected snapshot is the Applicable Tax Rule Set.

The supported year is an API validation rule. It is not a new project term or Domain value.

## Time and date rules

Each Passage timestamp is City Local Time. The HTTP controller validates the transport shape and passes `request.passages()` directly to `CalculationCommand`. `CalculationCommand` uses `List.copyOf` to take an unmodifiable copy before the command crosses into the Calculation Service. The controller does not parse the timestamps.

`CalculationServiceImpl` strictly parses each timestamp without an offset and in request order. The selected City supplies one stored IANA time zone. The Calculation module uses that time zone to derive the Passage instant.

City Local Time supplies:

- the supported-year check;
- the calculation date;
- the local time for Tax Time Band selection.

The instant supplies:

- chronological Passage order;
- actual elapsed time for Charge Window behavior.

Only Passage City Local Time dates in 2013 are valid. Stored Tax Rule Set effective dates, public-holiday dates, and other supporting dates can be outside 2013.

## Supported-year validation

`CalculationServiceImpl` parses each Passage timestamp in request order. It stops at the first malformed timestamp and keeps the existing malformed-timestamp rejection behavior.

After all timestamps parse, `CalculationServiceImpl`:

1. checks each City Local Time year;
2. collects each affected zero-based Passage index;
3. keeps the indexes in request order;
4. rejects the complete request when the collection is not empty.

This issue does not combine malformed-timestamp errors with unsupported-year errors. The HTTP exception handler maps both Calculation Service exceptions to the agreed HTTP errors.

Malformed-timestamp and unsupported-year requests stop before Tax Rule Service access, PostgreSQL access, and the Calculation Service metrics recorder.

## Unsupported-year response

The calculation HTTP operation returns HTTP `400` with `application/problem+json`:

```json
{
  "title": "Invalid calculation request",
  "status": 400,
  "detail": "The request contains invalid Passages.",
  "code": "INVALID_REQUEST",
  "errors": [
    {
      "field": "passages[0]",
      "code": "UNSUPPORTED_PASSAGE_YEAR",
      "message": "A Passage City Local Time date must be in 2013."
    }
  ]
}
```

The response omits `type`. The top-level code identifies the request-error family. Each error code identifies the failed validation rule. Several unsupported Passages produce several error entries with the same error code and different fields.

The response uses this `WARN` log failure category without a stack trace:

```text
unsupported-passage-year
```

## Applicable Tax Rule Set behavior

For each distinct calculation date, the Tax Rule Service selects the Tax Rule Set with the latest effective date that is not after that date.

A future Tax Rule Set does not apply to an earlier date. A newer Tax Rule Set does not change the result for a date controlled by an older Tax Rule Set.

Each Tax Rule Set is one complete immutable snapshot. The application does not merge it with an earlier snapshot and does not inherit missing child rows.

The existing `DEBUG` event remains:

```text
Selected Applicable Tax Rule Set. cityCode={} calculationDate={} effectiveFrom={} chargeWindowEnabled={} dailyMaximumEnabled={}
```

The event contains no Passage timestamp, Tax Amount, or stored scalar rule value.

## Currency consistency

One calculation response has one currency. The calculator rejects a result when Daily Taxes from selected Tax Rule Sets use different currencies.

The Domain area continues to prevent Tax Amount arithmetic across currencies. The Calculation Service translates that failure to a calculation-owned stored-content failure. The HTTP exception boundary returns a safe, empty HTTP `500` response in this issue.

The handler logs this stable failure category at `ERROR` with the cause:

```text
mixed-tax-rule-set-currencies
```

The log does not contain Tax Amounts or request timestamps.

## Missing Applicable Tax Rule Set

The Tax Rule Service continues to reject a calculation date when no Applicable Tax Rule Set exists. The Calculation Service translates the Tax Rule module failure to a calculation-owned stored-content failure and preserves the cause.

The HTTP exception boundary returns a safe, empty HTTP `500` response and logs this stable category with the cause:

```text
missing-applicable-tax-rule-set
```

Safe context can contain the City code and calculation date. The response contains no internal failure data.

## Metrics

This issue adds no custom metric. Standard HTTP metrics, the calculation outcome timer, the accepted Passage-count distribution, and safe logs answer the issue's operational questions.

`CalculationServiceImpl` completes timestamp parsing and supported-year validation before it calls the calculation metrics recorder. The calculation outcome timer and Passage-count distribution record only requests that pass this input validation. Standard HTTP metrics continue to record rejected HTTP requests.

The existing calculation events keep their owners and levels. A metrics failure cannot change the calculation result.

## Approved test seams

Use these public boundaries:

- the calculation HTTP operation for supported-year validation and its Problem Details response;
- `CalculationService.calculate` for strict timestamp parsing, fail-fast malformed-timestamp rejection, complete unsupported-year index collection, and the pre-Tax-Rule validation boundary;
- `TaxCalculator.calculate` for local-midnight Charge Window behavior and mixed currencies;
- `CalculationService.calculate` for calculation-owned stored-content failure translation;
- the HTTP-to-PostgreSQL operation for successive snapshots, historical results, and future-snapshot exclusion.

Keep the existing Calculation Service winter and summer time-zone test. Keep the existing Tax Rule Service tests for Applicable Tax Rule Set selection. Do not add a repeated case when an existing test already proves the same rule.

Do not test private methods, exact SQL, JPA internals, or log output.

## Implementation groups

### Group 1: Supported City Local Time input

This group adds:

- raw Passage timestamp transfer through `CalculationCommand`, which uses `List.copyOf` to take an unmodifiable copy;
- strict parsing and supported-year validation in `CalculationServiceImpl` before Tax Rule Service and metrics access;
- complete supported-year index collection;
- the narrow Problem Details response;
- top-level and per-error response codes;
- the supported-year `WARN` failure category;
- supported-year boundary tests;
- one explicit local-midnight Charge Window test;
- applicable API, calculation, testing, operations, and question updates.

The approved test seams are the calculation HTTP operation, `CalculationService.calculate`, and `TaxCalculator.calculate`.

### Group 2: Applicable Tax Rule Set consistency

This group adds or proves:

- successive complete snapshots;
- stable historical results;
- future-snapshot exclusion;
- mixed-currency rejection;
- missing Applicable Tax Rule Set translation;
- mixed-currency translation;
- the two stored-content `ERROR` failure categories;
- applicable calculation, API, architecture, persistence, testing, and operations updates.

The approved test seams are `TaxCalculator.calculate`, `CalculationService.calculate`, and the HTTP-to-PostgreSQL operation.

## Writer agents and worktrees

Only one implementation group can be active.

For the active group:

1. create separate temporary Git worktrees from the group's fixed point;
2. assign independent, non-overlapping changes to writer subagents;
3. use `gpt-5.6-sol` with medium reasoning for each writer;
4. let the writers work in parallel only inside that group;
5. integrate each checkpoint into the issue branch;
6. remove or replace temporary worktrees only after their changes are safe.

Do not start Group 2 work before the user approves the Group 1 code-review checkpoint. After approval, create fresh Group 2 worktrees from the approved Group 1 commit.

The root agent controls integration and pushes to the issue branch. This prevents parallel worktrees from racing on the draft pull request.

## Checkpoint flow

The draft pull request is the review location. Each checkpoint gets a brief summary and a pushed commit. Each checkpoint is an approval gate. Do not show complete files in chat unless the user asks.

### Red

1. Add the group tests at the approved seams.
2. Run focused tests.
3. Confirm that the new behavior fails for the expected reason.
4. Commit the red checkpoint with issue number `#5`.
5. Push the checkpoint to the draft pull request.
6. Give the user a brief summary and commit identifier.
7. Stop for user approval.

Do not start green work before the user approves the red checkpoint.

### Green

1. Add the minimum production behavior for the group.
2. Run focused tests and type checking regularly.
3. Apply the project formatter.
4. Update the affected documents.
5. Commit the green checkpoint with issue number `#5`.
6. Push the checkpoint to the draft pull request.
7. Give the user a brief summary and commit identifier.
8. Stop for user approval.

Do not start code review before the user approves the green checkpoint.

### Code review

1. Compare `HEAD` with the group's fixed point by three-dot diff.
2. Run the Standards and Spec axes in parallel with the `code-review` skill.
3. Use `gpt-5.6-sol` with high reasoning for both review agents.
4. Report the two axes separately.
5. If review fixes are necessary, use `gpt-5.6-sol` writer agents with medium reasoning.
6. Rerun affected verification and repeat the review when necessary.
7. Push review fixes when they exist.
8. Give the user a brief review summary and the current commit identifier.
9. Stop for user approval.

Do not start or push work from the next group before this approval.

## Fixed points

The fixed point for Group 1 is branch commit `9e464e8`.

The fixed point for Group 2 is the approved Group 1 commit. Create all Group 2 worktrees from that commit.

## Verification

Use focused commands during each group:

```text
./mvnw -Dtest=TaxCalculatorTest test
./mvnw -Dtest=CalculationServiceImplTest test
./mvnw -Dtest=CalculationControllerTest test
./mvnw -Dit.test=CalculationITest verify -Dskip.regular.tests=true
```

Run the complete verification once at the end of Group 2:

```text
./mvnw verify
```

## Completion checklist

- The request cannot override the stored City time zone.
- Every valid Passage timestamp uses City Local Time.
- A request reports all Passage indexes outside 2013.
- Stored supporting dates can be outside 2013.
- A Charge Window cannot cross a City Local Time date boundary.
- Each calculation date uses its Applicable Tax Rule Set.
- Future snapshots do not change earlier results.
- Complete snapshots do not inherit earlier child content.
- One result cannot combine currencies.
- Missing Applicable Tax Rule Sets and mixed currencies reach the shared `ERROR` boundary with safe categories.
- No new custom metric exists without an operational question.
- Focused checks and the final `./mvnw verify` pass.
- Both review axes pass for each group.
- The user approves Group 1 before Group 2 starts.
- The user approves Group 2 before final pull-request review.
