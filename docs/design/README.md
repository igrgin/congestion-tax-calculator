# Design Guide

These documents describe how the Congestion Tax Calculator works and why the important design choices were made. They are written for a developer who has not read the planning conversation.

## Recommended Reading Order

1. [Calculation](calculation.md) explains the tax behavior, time handling, and calculation model.
2. [API](api.md) defines the HTTP request, response, validation, and errors.
3. [Architecture](architecture.md) defines the components and dependency direction.
4. [Persistence](persistence.md) defines PostgreSQL storage, JPA loading, and stored rule versions.
5. [Testing](testing.md) defines the test levels and case-selection strategy.
6. [Operations](operations.md) defines local setup, configuration, health, metrics, CI, and the caching decision.

The root `README.md` is the guide for building, running, and calling the application. The root `questions.md` records assignment ambiguities and the plain-language assumptions used by the solution. Architecture decision records in `docs/adr/` explain choices that are costly to reverse.

## Assignment Coverage

| Assignment requirement | Planned implementation |
|---|---|
| Spring Boot with Java 17 or later | Spring Boot project compiled and tested with Java 17 |
| Call the calculation with different inputs | Versioned HTTP `POST` operation documented in the API design and OpenAPI |
| Scope can be limited to 2013 | City-local year validation rejects a complete request that contains a date outside 2013 |
| Gothenburg hours and amounts | Positive-charge bands stored in PostgreSQL and inserted by Flyway |
| Daily maximum of 60 SEK | Stored daily maximum applied after the single-charge rule |
| Weekends, public holidays, preceding dates, and July are tax-free | Stored weekday, month, holiday, and holiday-offset content |
| Single charge within 60 minutes | Inclusive, non-sliding Charge Window that keeps the highest amount |
| Tax-exempt vehicles | Database-defined Vehicle Types and rule-specific exemptions |
| Rules outside the application | PostgreSQL runtime content behind a Tax Rule Provider |
| Support content for different cities | City code, time zone, currency, and versioned Tax Rules stored per city |
| Submit questions | Root `questions.md` pairs each unresolved question with the assumption used |
| Six-hour limit and prioritization | Implementation budget and additional-work boundary in the operations design and future README |

## Common Terms

- **Passage**: One recorded occurrence of a vehicle passing a tolling station in either direction. Its timestamp identifies an instant that is converted to City Local Time for calculation.
- **City local time**: The local date and clock time in the selected city's IANA time zone, such as `Europe/Stockholm`.
- **Tax rules**: City content that defines taxable times, amounts, tax-free dates, exempt vehicle types, the single-charge period, and the daily maximum.
- **Tax rule set**: One immutable version of the tax rules for a city, with the date on which the version starts to apply.
- **Charge window**: A group that starts with one passage and contains later passages no more than 60 minutes after the first passage. The group produces one charge: its highest amount.
- **Daily tax**: The result for one vehicle on one city-local calendar date after the single-charge rule and daily maximum are applied.
