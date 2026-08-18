# Store Tax Time Band Start and End Times

PostgreSQL stores each Tax Time Band as a start `TIME`, an end `TIME`, and a non-negative `NUMERIC(12,2)` amount.

The start is included and the end is excluded. An end after the start defines a same-date band. An end before the start defines one band that crosses midnight. Equal start and end times are invalid and do not represent a full day.

This representation is direct for content editors and maps to Java `LocalTime`. It was selected instead of integer minute numbers, a next-day flag, or a start time with a duration. A cross-midnight band needs no extra database column.
