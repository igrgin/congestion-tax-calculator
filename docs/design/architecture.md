# Application Architecture

This document defines the main application components and the direction of their dependencies.

The application uses stored city data from its first implementation. A city code selects the content for that city. The calculation code does not contain city-specific amounts, time bands, calendar dates, currency, time zone, exemptions, daily maximum, or charge-window duration. The assignment supplies the initial rule data for Gothenburg.

```mermaid
flowchart LR
    API["HTTP Controller"] --> APP["Calculation Service"]
    APP --> CALC["Pure Tax Calculator"]
    APP --> PORT["Tax Rule Provider"]
    PORT -. "implemented by" .-> JPA["JPA Tax Rule Provider"]
    JPA --> REPO["Spring Data Repositories"]
    REPO --> DB[("PostgreSQL")]
```

- The HTTP controller validates transport data and creates the HTTP response.
- `CalculationService` defines the operation that coordinates one complete request.
- `CalculationServiceImpl` is the Spring `@Service` implementation. It converts time values and calls the other components.
- The pure calculator applies the rules without Spring or database behavior.
- The Tax Rule Provider is the application boundary for city and rule data.
- The JPA provider loads stored rows and maps them to immutable calculation values.
- Spring Data repositories contain explicit database read operations.

The application uses one Maven module with three owned package areas. Transport and persistence are adapters inside the area that owns their interface.

```text
io.github.igrgin.congestiontax
├── calculation
│   ├── exception
│   └── http
│       └── dto
├── domain
│   ├── calculation
│   └── rule
└── ruleprovider
    └── persistence
        ├── city
        ├── taxruleoption
        ├── taxruleset
        ├── taxtimeband
        └── vehicletype
```

The Maven group is `io.github.igrgin`, the artifact ID and application name are `congestion-tax-calculator`, and the base Java package is `io.github.igrgin.congestiontax`. The main class is `CongestionTaxCalculatorApplication`.

The `domain` area owns the pure calculation language and behavior. Given a known Vehicle Type, localized Passages, and an Applicable Tax Rule Set for each date, it returns Daily Taxes and a total. It does not find cities or rules, parse transport data, convert time zones, store rows, log, or record metrics.

Dependencies follow these directions:

```text
calculation.http -> calculation
calculation -> domain
calculation -> ruleprovider
ruleprovider -> domain
ruleprovider.persistence -> ruleprovider + domain
domain -> JDK only
```

`calculation` depends on the provider interface, not its persistence adapter. `ruleprovider.persistence` maps stored rows to immutable values before return. This gives the pure calculator and the coordination service small public test seams without PostgreSQL.

The provider offers three application operations:

- `findCity(cityCode)` returns an optional City with its database ID, public code, name, and `ZoneId`.
- `findVehicleType(vehicleTypeCode)` returns an optional database-defined Vehicle Type with its code and description.
- `loadRules(cityId, requiredDates)` returns immutable Applicable Tax Rule Sets indexed by date.

The calculation service first finds the city and Vehicle Type. It then converts passage instants to city-local dates and requests only the rules and calendar data required by those dates. The separate Vehicle Type lookup distinguishes a known taxable type from an unknown code. A taxable type does not appear in the exemption rows, but it must still exist in the Vehicle Type table.

Micrometer instrumentation stays at the Calculation Service boundary. It measures application coordination without adding Micrometer, Spring, or monitoring behavior to the pure calculator. The HTTP and database integrations use the standard meters that Spring Boot supplies.

Every class annotated with Spring `@Service` follows the same naming convention: `XService` is its interface and `XServiceImpl` is its implementation. Consumers inject the interface. This is a project convention. It applies to Spring services, not controllers, Spring Data repositories, configuration classes, the pure calculator, or other components that are not services.
