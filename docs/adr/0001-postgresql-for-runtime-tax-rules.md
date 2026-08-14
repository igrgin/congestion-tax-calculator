# Use PostgreSQL for Runtime Tax Rules

Use a provider boundary so the calculation does not depend on rule storage. The completed application reads city rules from PostgreSQL at runtime and does not silently use an in-memory fallback. PostgreSQL fits the stable relational shape and can enforce links, valid values, and non-overlapping time bands more directly than a document store. Stored rule sets are immutable and effective-dated so a later update does not change an earlier calculation. Unit tests can use an in-memory test provider.
