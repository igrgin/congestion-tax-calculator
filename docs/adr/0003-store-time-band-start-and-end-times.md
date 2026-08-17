# Store Tax Time Band Start and End Times

PostgreSQL stores each positive Tax Time Band as a start `TIME`, an end `TIME`, and a `NUMERIC(12,2)` amount.

The end must be after the start. A Tax Time Band cannot cross midnight, and equal start and end times do not represent a full day. The start is included and the end is excluded.

This representation is direct for content editors and maps to Java `LocalTime`. It was selected instead of integer minute numbers, a next-day flag, or a start time with a duration. It needs no calculated end time or consistency flag.
