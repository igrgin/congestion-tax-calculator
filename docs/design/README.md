# Design Guide

These documents describe how the Congestion Tax Calculator works and why the main design choices were made. They are written for a developer who has not read the planning conversation.

## Recommended reading order

1. [Calculation](calculation.md) explains the Tax behavior, Passage time handling, and calculation model.
2. [API](api.md) defines the HTTP request, response, validation, and errors.
3. [Architecture](architecture.md) defines the components and dependency directions.
4. [Persistence](persistence.md) defines PostgreSQL storage and JPA loading.
5. [Testing](testing.md) defines the test levels and case-selection strategy.
6. [Operations](operations.md) defines local setup, configuration, logging, health, metrics, CI, and the caching decision.

The planned root `README.md` will be the guide for building, running, and calling the application. The root `questions.md` records assignment ambiguities and the assumptions used by the solution. Architecture decision records in `docs/adr/` explain choices that are costly to reverse.

## Assignment coverage

| Assignment requirement | Design |
|---|---|
| Spring Boot with Java 17 or later | Spring Boot project compiled for Java 17 |
| Call the calculation with different inputs | Versioned HTTP `POST` operation with a collection-based Passage request |
| Scope can be limited to 2013 | Final City Local Time validation rejects a request that contains a date outside 2013 |
| Gothenburg hours and amounts | Tax Time Bands stored in PostgreSQL and installed by Flyway |
| Daily maximum of 60 SEK | Stored `DAILY_MAXIMUM` Tax Rule Option |
| Weekends, public holidays, preceding dates, and July are tax-free | Stored Tax Exemptions and a stored public-holiday preceding-date count |
| Single charge within 60 minutes | Stored `CHARGE_WINDOW` Tax Rule Option |
| Tax-exempt vehicles | Database-defined Vehicle Types and Tax Rule Set-specific exemptions |
| Tax Rules outside the application | PostgreSQL runtime content loaded through the Tax Rule Service |
| Support content for different Cities | City code, IANA time zone, currency, and one Tax Rule Set stored per City |
| Submit questions | Root `questions.md` pairs each unresolved question with the assumption used |

The first calculation issue implements the initial vertical path through this design. Later issues add the remaining assignment Tax Rules without changing the HTTP collection shape or the service boundaries.

## Common terms

- **City**: The place whose Tax Rules and IANA time zone control a Congestion Tax Calculation. A city code selects it.
- **Passage**: One recorded occurrence of a vehicle passing a tolling station in either direction. Its timestamp gives City Local Time, and the selected City supplies its IANA time zone.
- **City Local Time**: The local date and clock time supplied in a Passage timestamp for the selected City.
- **Tax Rule**: A rule that determines if a Passage is taxable and which charge applies.
- **Tax Exemption**: A Tax Rule that makes a Passage tax-free when its City Local Time or Vehicle Type matches stored content.
- **Tax Rule Option**: An optional Tax Rule with one scalar value that changes calculation behavior or extends a Tax Exemption.
- **Tax Rule Set**: The complete set of Tax Rules stored for one City. Each City has one Tax Rule Set.
- **Tax Time Band**: A period of City Local Time with one Tax Amount. Its start is included, its end is excluded, and it can cross midnight.
- **Tax Amount**: A non-negative Congestion Tax value in one currency.
- **Charge Window**: A period that starts with its first Passage and contains each Passage no later than the configured duration after that first Passage. It can cross City Local Time date boundaries.
- **Daily Tax**: The Congestion Tax for one vehicle on one City Local Time date after the Charge Window and Daily Maximum apply.
