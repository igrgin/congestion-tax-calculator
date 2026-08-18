# Store Tax Time Band Start and End Times

PostgreSQL stores each Tax Time Band as a start `TIME`, an end `TIME`, and a non-negative `NUMERIC(12,2)` amount.

The start is included and the end is excluded. An end after the start defines a same-date band. An end before the start defines one band that crosses midnight. Equal start and end times define a full-day band that ends at the same local time on the next date.

A full-day band overlaps every other Tax Time Band. It is valid only when it is the only Tax Time Band in its Tax Rule Set.

This representation is direct for content editors and maps to Java `LocalTime`. It was selected instead of integer minute numbers, a next-day flag, or a start time with a duration.
