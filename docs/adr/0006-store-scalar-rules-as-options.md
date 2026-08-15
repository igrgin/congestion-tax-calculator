# Store scalar Tax Rules as options

Store the daily maximum, Charge Window, and public-holiday preceding-date behavior as typed rows in one `TAX_RULE_OPTION` table. A code-owned `TAX_RULE_OPTION_TYPE` table defines the closed set of supported options, and PostgreSQL constraints require one matching scalar value. This gives optional scalar Tax Rules one bulk-loaded structure while Tax Exemptions and Tax Time Bands keep their distinct relational forms.
