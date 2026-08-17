# Organize Java by Owned Business Responsibility

Organize Java types first by the durable business responsibility that owns the behavior:

```text
calculation
domain
metrics
taxrule
```

Keep HTTP transport under `calculation.http`. Keep JPA entities, repository interfaces, and JPA service implementations under the persistence package of the business responsibility that owns them.

`calculation.http` owns request time-zone validation and Passage time mapping. `taxrule.persistence` owns City existence, Vehicle Type, and Tax Rule storage. One service can use one repository or several repositories. The service boundary follows the business responsibility, not the number of database tables.

The `domain` package contains the pure calculation model. It depends on the JDK and compile-time Lombok annotations. It has no Lombok runtime dependency. `calculation` coordinates the complete use case. The top-level `metrics` package owns calculation instrumentation.

Each Spring service has a matching interface and implementation. Consumers depend on the interface. Persistence entities do not leave their owning persistence package.

This structure keeps related changes together, makes ownership and dependency directions visible, and keeps replaceable technology details outside the pure domain.
