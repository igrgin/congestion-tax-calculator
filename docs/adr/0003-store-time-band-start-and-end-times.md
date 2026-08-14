# Store Time-Band Start and End Times

PostgreSQL stores each positive Tax Time Band as start `TIME`, end `TIME`, and `NUMERIC(12,2)` amount. An end before the start crosses midnight, and equal times mean a full day. This was selected over minute numbers, a separate next-day flag, and start plus duration because it is direct for content editors, maps to Java `LocalTime`, and uses no consistency flag or calculated end.
