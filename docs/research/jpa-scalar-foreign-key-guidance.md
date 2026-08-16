# JPA Scalar Foreign-Key Guidance

## Scope

This guide applies to the issue 3 persistence read path. The project uses Spring Data JPA 4.1.0, Hibernate ORM 7.4.1, and PostgreSQL. The persistence adapter loads stored rows and maps them to immutable application and calculation values.

## Finding

The proposed design is correct for this read path. A JPA entity can map `city_id` or `rule_set_id` as a basic `Long` field. It does not have to map the referenced row as a `@ManyToOne` association. Jakarta Persistence defines wrapper types such as `Long` as basic types. It requires a relationship annotation only when the Java model contains an association to another entity. [Jakarta Persistence 3.2, sections 2.2 and 2.6](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2)

PostgreSQL, not JPA, enforces the relationship. A foreign key requires each non-null referencing value to match a referenced primary key or unique value. Thus, the Flyway migration must define each foreign key and must make each required foreign-key column `NOT NULL`. [PostgreSQL constraint documentation](https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-FK)

## Benefits

Scalar foreign-key fields keep all loading decisions in `JpaTaxRuleProvider`. The provider can load Tax Rule Set candidates first, then load Tax Rule Options and Tax Time Bands with two explicit bulk queries. No entity association can cause an implicit fetch, and no association cascade or orphan-removal operation exists. JPA supplies these operations only through association mappings. [Jakarta Persistence `ManyToOne` API](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/manytoone) [Jakarta Persistence `OneToMany` API](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/onetomany)

Spring Data JPA can derive all proposed queries from scalar entity fields. It checks entity property names, supports `LessThanEqual`, `In`, and static `OrderBy`, and accepts a `Collection` for an `In` parameter. [Spring Data JPA query-method documentation](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)

The proposed unique constraints also provide useful PostgreSQL B-tree indexes because each query filters on the leading column:

- `tax_rule_set (city_id, effective_from)` supports the City and effective-date query.
- `tax_rule_option (rule_set_id, type_code)` supports the Tax Rule Option bulk query.
- `tax_time_band (rule_set_id, start_time, end_time)` supports the Tax Time Band bulk query.

PostgreSQL uses a multicolumn B-tree index most efficiently when a query constrains its leading columns. A foreign-key declaration alone does not create an index on its referencing columns. [PostgreSQL multicolumn-index documentation](https://www.postgresql.org/docs/current/indexes-multicolumn.html) [PostgreSQL foreign-key index guidance](https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-FK)

## Limits and risks

### Schema validation

`spring.jpa.hibernate.ddl-auto=validate` is useful, but it does not prove the complete Flyway schema. Hibernate 7.4.1 validates mapped tables, columns, column types, and sequences. It can validate mapped indexes and unique keys only when their separate validation settings enable that work; the default is no index or unique-key validation. Its validator does not validate foreign keys or check constraints. [Hibernate 7.4.1 schema-validator source](https://github.com/hibernate/hibernate-orm/blob/7.4.1/hibernate-core/src/main/java/org/hibernate/tool/schema/internal/AbstractSchemaValidator.java) [Hibernate 7.4.1 constraint-validation source](https://github.com/hibernate/hibernate-orm/blob/7.4.1/hibernate-core/src/main/java/org/hibernate/tool/schema/internal/ConstraintValidationType.java)

The scalar fields also give Hibernate no association metadata from which it could infer a foreign key. Flyway remains the source for foreign keys, unique constraints, check constraints, and `NOT NULL` constraints. A PostgreSQL integration test must prove that the migration runs and that the complete read path works.

The `tax_rule_option_type` table has no JPA entity. Hibernate does not validate an unmapped table. This is acceptable because Flyway owns that table and its foreign key from `tax_rule_option.type_code`.

### Read-only use

Entities with no setters are not inherently read-only. With field access, JPA reads and writes entity fields directly. A managed entity can still be dirty-checked if code changes its state. [Jakarta Persistence 3.2, section 2.3](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2)

Put `@Transactional(readOnly = true)` on the concrete provider load operation. Spring states that read-only mode is a performance hint and is not a check that prevents a modifying query. With Hibernate, Spring sets manual flush mode and skips dirty checks. [Spring Data JPA transaction documentation](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html)

Use repository interfaces that extend `Repository<T, ID>` and expose only the approved read methods. This keeps save and delete operations outside the adapter interface. Spring Data supports selective repository methods. [Spring Data repository-definition documentation](https://docs.spring.io/spring-data/jpa/reference/repositories/definition.html)

Hibernate `@Immutable` is optional. It stops dirty checking and does not synchronize in-memory changes, but it ignores such changes instead of reporting them. It also does not replace narrow repository interfaces or database permissions. [Hibernate `@Immutable` API](https://docs.hibernate.org/orm/7.4/javadocs/org/hibernate/annotations/Immutable.html)

### Multi-query consistency

Keep the candidate query and both child queries in one provider transaction. PostgreSQL `READ COMMITTED` can give two successive queries different snapshots. This is safe here only if a Tax Rule Set and all its child rows are inserted in one transaction and an existing Tax Rule Set is never changed. These rules make the first candidate query a valid point-in-time selection. If the application later permits concurrent changes to an existing Tax Rule Set, use a stable snapshot such as `REPEATABLE READ` or replace the three reads with one consistent query. [PostgreSQL transaction-isolation documentation](https://www.postgresql.org/docs/current/transaction-iso.html)

### Enum storage

Mapping `TaxRuleOptionEntity.typeCode` with `@Enumerated(EnumType.STRING)` is valid for a basic enum field. The stored value is the Java enum constant name. Therefore, the enum constants and the database codes must be identical and stable. The database foreign key remains the integrity check for the option-type row. [Jakarta Persistence `Enumerated` API](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/enumerated) [Jakarta Persistence `EnumType` API](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/enumtype)

## Project rule

Use scalar foreign-key fields and explicit bulk queries for the Tax Rule Provider. Apply these conditions:

- Flyway defines restrictive, non-null foreign keys.
- Repository interfaces expose read operations only.
- The provider load operation owns one read-only transaction.
- The provider groups rows by scalar ID and maps complete immutable values before return.
- Tax Rule Set rows and their child rows form an immutable snapshot after commit.
- Integration tests, not Hibernate schema validation alone, verify the Flyway schema and the complete read path.

Add a JPA association only when application code needs object navigation, association fetch plans, or JPA cascade behavior. Database foreign keys express and enforce the current relationships.
