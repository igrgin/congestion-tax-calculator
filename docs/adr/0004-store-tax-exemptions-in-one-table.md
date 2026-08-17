# Store Tax Exemptions in one table

Store weekday, month, public-holiday, and Vehicle Type Tax Exemptions as type-specific rows in one `TAX_EXEMPTION` table. A code-owned `TAX_EXEMPTION_TYPE` table defines the closed set of supported types, and PostgreSQL constraints require the matching typed value. This replaces four type-specific tables and a separate public-holiday table, lets the Tax Rule Service bulk-load all Tax Exemptions, and keeps Tax Time Bands in their direct relational form.
