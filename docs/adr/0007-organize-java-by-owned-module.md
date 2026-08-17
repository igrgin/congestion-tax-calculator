# Organize Java by Owned Business Responsibility

Organize Java types first by the durable business responsibility that owns the behavior:

```text
calculation
domain
metrics
taxrule
```

Keep HTTP transport under `calculation.http`. Keep Calculation Service input and output values under `calculation.model`. Keep the Tax Rule Service interface and implementation under `taxrule`. Keep JPA entities and repository interfaces under `taxrule.persistence`.

`calculation.http` owns request timestamp parsing. `calculation` owns Passage time mapping. `taxrule` owns City existence, City time-zone validation, Vehicle Type, and Tax Rule loading. `taxrule.persistence` owns the stored row mappings and database reads. One service can use one repository or several repositories. The service seam follows the business responsibility, not the number of database tables. ADR-0008 supersedes the former HTTP time-zone ownership statement in this record.

The `domain` package contains the pure calculation model. It depends on the JDK and compile-time Lombok annotations. It has no Lombok runtime dependency. `calculation` coordinates the complete use case. The top-level `metrics` package owns calculation instrumentation.

Each Spring service has a matching interface and implementation. Consumers depend on the interface. Persistence types can use public Java access when the implementation in the owning application area needs them across a package. They remain internal to the Tax Rule module and do not cross the `TaxRuleService` interface.

This structure keeps related changes together, makes ownership and dependency directions visible, and keeps replaceable technology details outside the pure domain.
