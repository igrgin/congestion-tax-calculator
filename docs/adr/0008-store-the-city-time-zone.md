# Store the City time zone

Store one required IANA time-zone identifier for each City. The City time zone controls how the Calculation module derives Passage instants from City Local Time, and the calculation request cannot override it. This design prevents a caller from selecting a time zone that conflicts with the selected City and removes time-zone knowledge from the HTTP interface.

This record supersedes ADR-0002 and the request-owned time-zone statements in ADR-0001 and ADR-0007.
