# Lombok guidance

## Scope

This guide applies to Java 17 code that uses Spring Boot, Spring Framework, Spring Data JPA, and Hibernate. Use Lombok when it removes mechanical code and keeps the class contract clear. Prefer Java language features when they give the same result.

## Recommended use

### Spring services and components

Use `@RequiredArgsConstructor` when a Spring-managed class has required dependencies in uninitialized `final` fields. Lombok generates one constructor parameter for each such field. It also generates parameters for uninitialized fields that use Lombok `@NonNull`. [Lombok constructor documentation](https://projectlombok.org/features/constructor)

Spring uses the only constructor of a bean even when that constructor has no `@Autowired` annotation. Thus, a class with one Lombok-generated constructor does not need `@Autowired`. If a class has more than one constructor, use `@Autowired` only when Spring needs an explicit constructor choice. [Spring Framework constructor injection documentation](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)

Use an explicit constructor when it validates values, transforms input, or makes a non-obvious choice. In these cases, the constructor code is part of the class contract and must stay visible.

### Loggers

Use `@Slf4j` on a class that writes log messages. It generates the usual private static final SLF4J logger for that class. [Lombok logging documentation](https://projectlombok.org/features/log)

Do not add `@Slf4j` to a class that does not write log messages.

### JPA entities

Use `@NoArgsConstructor(access = AccessLevel.PROTECTED)` when an entity needs the Jakarta Persistence constructor. The Jakarta Persistence specification requires a public or protected constructor with no parameters. It also states that an entity must be non-final. [Jakarta Persistence 3.2, section 2.1](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2)

Use `@Getter` only for state that callers must read. Use a narrow `@Setter` only when the entity design needs direct mutation. Lombok getters and setters are public by default, but they can have a narrower access level. [Lombok getter and setter documentation](https://projectlombok.org/features/GetterSetter)

Prefer named entity methods for state changes that have a domain rule. For example, a named method can validate a change and preserve an invariant. A generated setter cannot show that rule.

### Immutable values and records

Use a Java record for a small immutable request, response, configuration, or calculation value when record semantics are correct. A record supplies final component fields, a canonical constructor, accessors, `equals`, `hashCode`, and `toString`. A record is shallowly immutable. Mutable components still need defensive copies when isolation is required. [Java 17 `Record` documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Record.html)

Do not use Lombok `@Value` when a record gives the required contract. `@Value` generates a final class, final fields, getters, a constructor, equality, hashing, and text output. This duplicates Java 17 record features and uses JavaBean getter names instead of record component accessors. [Lombok `@Value` documentation](https://projectlombok.org/features/Value)

A record cannot be a JPA entity. Jakarta Persistence 3.2 prohibits a record as an entity and requires an entity to be non-final. [Jakarta Persistence 3.2, section 2.1](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2)

### Builders

Use `@Builder` only when a value has enough independent optional inputs to make direct construction hard to read. Put it on the intended constructor or factory method when the class has explicit construction rules. Lombok states that type-level `@Builder` acts like a package-private all-arguments constructor and can conflict with other generated or explicit constructors. [Lombok builder documentation](https://projectlombok.org/features/Builder)

Use `@Builder.Default` for every field initializer that must also apply when the builder does not receive that value. An unset builder field otherwise becomes `null`, zero, or `false`. An explicit constructor does not apply `@Builder.Default` unless it calls a Lombok-generated constructor that applies the default. [Lombok builder default documentation](https://projectlombok.org/features/Builder#builderdefault)

Do not use a builder to bypass an entity factory, invariant, or required domain operation. Prefer a constructor or factory that makes required values and validation clear.

### Null checks

Use Lombok `@NonNull` only when an immediate generated null check is the full required behavior. On a method parameter or record component, Lombok inserts a null check. Its default failure is `NullPointerException`. [Lombok `@NonNull` documentation](https://projectlombok.org/features/NonNull)

Use explicit validation when the application needs a domain-specific exception, more than one validation rule, or a detailed message.

## Restricted use

### `@Data`

Do not use `@Data` on JPA entities. `@Data` combines public getters, public setters for non-final fields, `toString`, `equals`, `hashCode`, and a required-arguments constructor. Its combined behavior makes the entity API too broad and applies equality and text-output rules without an entity-specific decision. [Lombok `@Data` documentation](https://projectlombok.org/features/Data)

Use the narrow Lombok annotations that match the exact need.

### `@Value` on entities

Do not use `@Value` on JPA entities. It makes the class and its fields final by default. Jakarta Persistence requires non-final entities and persistent members. Final classes also limit Hibernate proxy-based lazy loading. [Lombok `@Value` documentation](https://projectlombok.org/features/Value) [Hibernate ORM entity requirements](https://docs.hibernate.org/orm/7.0/userguide/html_single/#entity-pojo)

### `@EqualsAndHashCode` on entities

Do not use a default `@EqualsAndHashCode` on a JPA entity. Lombok uses all non-static and non-transient fields by default. Entity equality needs a deliberate identity policy. Hibernate states that entity equality is difficult and that many entities can keep object identity. It also shows that equality based on a generated identifier can break a `Set` because the identifier and hash value change after persistence. A stable natural key is the preferred basis when value equality is required. [Lombok equality documentation](https://projectlombok.org/features/EqualsAndHashCode) [Hibernate ORM equality guidance](https://docs.hibernate.org/orm/7.0/userguide/html_single/#entity-pojo-equalshashcode)

Write entity `equals` and `hashCode` explicitly when the entity needs value equality. Base them on the approved stable identity rule. Test transient, managed, detached, and proxy cases. Do not cache an entity hash code with Lombok. Lombok warns that cached hashing is unsafe when any value used by the hash can change. [Lombok equality documentation](https://projectlombok.org/features/EqualsAndHashCode)

### `@ToString` on entities

Do not use default `@ToString` on a JPA entity. Lombok includes all non-static fields by default and calls a getter when one exists. This behavior can include an association, access lazy state, recurse through a bidirectional relation, or expose a sensitive value. [Lombok text output documentation](https://projectlombok.org/features/ToString)

If entity text output is useful, write it explicitly or use `@ToString(onlyExplicitlyIncluded = true)`. Include only safe scalar values. Keep associations and sensitive values out.

### Forced no-argument constructors

Do not use `@NoArgsConstructor(force = true)` as a routine solution. Lombok initializes final fields to `null`, zero, or `false`, and it does not run `@NonNull` checks for those fields. This can create an invalid object. [Lombok constructor documentation](https://projectlombok.org/features/constructor)

Use the protected JPA no-argument constructor on an entity. Keep entity fields non-final as required by the persistence model. Use a normal constructor or factory for valid application creation.

### `@AllArgsConstructor`

Use `@AllArgsConstructor` only when every field is intentionally part of the public construction contract. Entity fields such as a generated database identifier, version, or audit value usually make a generated all-arguments constructor unsuitable.

### `@SneakyThrows`

Do not use `@SneakyThrows` in application code. It can throw a checked exception without declaring it in the method signature. This hides part of the method contract from callers. Lombok also describes this feature as contentious and advises careful use. [Lombok `@SneakyThrows` documentation](https://projectlombok.org/features/SneakyThrows)

Handle, translate, or declare the checked exception.

### Experimental annotations

Do not make an experimental Lombok annotation a project default. Lombok states that experimental features can change, move to the stable package, or be removed. Review a specific experimental annotation and its compiler limits before you approve it. [Lombok experimental feature documentation](https://projectlombok.org/features/experimental/)

## Build boundary

Use a maintained Lombok version that supports Java 17. Lombok added initial JDK 17 support in version 1.18.22. Configure Lombok as a compile-time annotation processor and a provided dependency. It is not an application runtime dependency. [Lombok change log](https://projectlombok.org/changelog) [Lombok Maven setup](https://projectlombok.org/setup/maven)

## Project rule

For each class, select the smallest Lombok annotation set that preserves a clear API and valid object state.

- Spring service or component with simple required dependencies: use `final` fields and consider `@RequiredArgsConstructor`.
- Class that logs: consider `@Slf4j`.
- JPA entity: consider protected `@NoArgsConstructor` and narrow getters. Decide setters, equality, hashing, and text output for that entity.
- Immutable transport or calculation value: prefer a Java record.
- Complex optional construction: consider `@Builder` on the approved constructor or factory and define required defaults.
- Domain validation or non-obvious construction: write explicit code.

The review is complete when every Lombok annotation has a clear reason, generated behavior does not weaken an invariant, and entity identity and association behavior remain explicit.
