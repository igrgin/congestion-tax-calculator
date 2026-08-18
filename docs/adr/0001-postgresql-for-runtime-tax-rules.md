# Use PostgreSQL for Runtime Tax Rules

The completed application reads City and Tax Rule data from PostgreSQL at runtime. It does not silently use an in-memory fallback when PostgreSQL is unavailable.

The pure calculator receives immutable calculation values and does not depend on stored data. `TaxRuleService` owns City existence, Vehicle Type, and Tax Rule Set loading. Its JPA implementation contains the repository communication. ADR-0008 supersedes the former request-owned time-zone statement in this record.

PostgreSQL fits the stable relational shape. Database constraints protect keys, relationships, required values, non-negative amounts, and Tax Time Band overlap. Repository-facing services repeat important stored-content checks when they assemble calculation values.

ADR-0009 supersedes the effective-dated Tax Rule Set decision in this record. Tests can use pure calculation values or mock the service and repository interfaces at the applicable boundary.
