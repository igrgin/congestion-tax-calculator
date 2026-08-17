# Use PostgreSQL for Runtime Tax Rules

The completed application reads City and Tax Rule data from PostgreSQL at runtime. It does not silently use an in-memory fallback when PostgreSQL is unavailable.

The pure calculator receives immutable calculation values and does not depend on stored data. `CityLocalTimeService` owns City lookup and City Local Time conversion. `TaxRuleService` owns Vehicle Type and Applicable Tax Rule Set loading. Their JPA implementations contain the repository communication.

PostgreSQL fits the stable relational shape. Database constraints protect keys, relationships, required values, positive amounts, and other single-row rules. Repository-facing services protect cross-row completeness, such as required and non-overlapping Tax Time Bands.

Tax Rule Sets are effective-dated snapshots so that a later snapshot does not change an earlier calculation. Tests can use pure calculation values or mock the service and repository interfaces at the applicable boundary.
