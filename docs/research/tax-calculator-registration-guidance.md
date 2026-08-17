# Tax Calculator Registration Guidance

## Question

Should `TaxCalculator` be:

1. a pure Java class that Spring registers with `@Bean`;
2. a pure Java class that `CalculationServiceImpl` creates directly; or
3. a Spring service with an interface and an implementation?

This research does not use the current issue plan or existing architecture decisions as constraints.

## Spring findings

Spring recommends plain objects for application code. A unit test can create these objects with `new` and does not need Spring. Spring can still manage a plain object when a configuration class exposes it through an `@Bean` method.

`@Bean`, `@Component`, and `@Service` all register objects in the Spring container. `@Service` is a specialization of `@Component`. It identifies a service role, but it does not add transactions, metrics, or other behavior by itself.

Spring can inject a concrete class. An interface is not required for constructor injection. An interface is also not required for a test double because Mockito can mock both classes and interfaces.

A Spring-managed bean can use proxy-based features if a later requirement needs them. The current `TaxCalculator` does not need a transaction or another proxy-based feature.

Sources:

- [Unit Testing](https://docs.spring.io/spring-framework/reference/testing/unit.html)
- [Using the `@Bean` Annotation](https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html)
- [Classpath Scanning and Managed Components](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html)
- [Dependencies and Configuration in Detail](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html)
- [Proxying Mechanisms](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)
- [Testing Spring Boot Applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [Bean Overriding in Tests](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/bean-overriding.html)
- [Understanding the Spring Framework Transaction Abstraction](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-decl-explained.html)

## Option 1: Pure class registered with `@Bean`

```java
public final class TaxCalculator {
    public CalculationResult calculate(
            VehicleType vehicleType,
            List<Passage> passages,
            Map<LocalDate, TaxRuleSet> applicableRules) {
        // Apply Tax Rules.
    }
}
```

```java
@Configuration(proxyBeanMethods = false)
public class CalculationConfiguration {
    @Bean
    TaxCalculator taxCalculator() {
        return new TaxCalculator();
    }
}
```

`CalculationServiceImpl` receives the concrete `TaxCalculator` through its constructor. The calculator stays independent from Spring. Its construction stays outside the service that uses it.

## Option 2: Pure class constructed directly

```java
public final class CalculationServiceImpl implements CalculationService {
    private final TaxCalculator taxCalculator = new TaxCalculator();
}
```

This is the smallest option. It makes `TaxCalculator` an internal implementation detail of `CalculationServiceImpl`. It also makes the dependency less visible and gives `CalculationServiceImpl` responsibility for construction.

## Option 3: Spring service interface and implementation

```java
public interface TaxCalculatorService {
    CalculationResult calculate(
            VehicleType vehicleType,
            List<Passage> passages,
            Map<LocalDate, TaxRuleSet> applicableRules);
}
```

```java
@Service
public final class TaxCalculatorServiceImpl implements TaxCalculatorService {
    // Apply Tax Rules.
}
```

This option follows the project rule for Spring services. However, it creates a Java interface with one production implementation and no current adapter need.

## Codebase design analysis

| Option | Module depth | Seam | Test effect | Deletion test |
|---|---|---|---|---|
| Pure class with `@Bean` | The `calculate` method hides the Tax Rule algorithm behind a small interface. | Spring owns construction only. | Unit tests use `new`; Spring tests can replace the bean. | No extra Java interface exists. |
| Direct construction | The calculator remains deep, but its construction is inside the caller. | No container seam exists. | Unit tests use `new`; caller tests use the real calculator unless construction changes. | This has the least code. |
| Service interface and implementation | The algorithm is deep, but the service interface repeats the implementation shape. | The seam has one in-process adapter. | Tests can replace it, but that does not require an interface. | Deleting the interface loses no current adapter boundary. |

`TaxCalculator` is an in-process dependency. Its public `calculate` method is already its architecture interface. A separate Java interface would be a hypothetical seam until a second implementation or a real adapter need exists.

## Recommendation

Use a pure `TaxCalculator` class and register it through an explicit `@Bean` method.

This option keeps the Tax Rule calculation independent from Spring. It also keeps the dependency visible in `CalculationServiceImpl` and keeps construction outside that service. Do not add `TaxCalculatorService` and `TaxCalculatorServiceImpl` unless a later requirement creates a real service boundary.
