# Congestion Tax Calculation

This context defines the language for calculating congestion tax from vehicle passages with the rules for a selected city.

## Language

**Congestion Tax Calculation**:
A calculation of congestion tax for one vehicle from a set of passages.
_Avoid_: Tax query, fee lookup

**Passage**:
One recorded occurrence of a vehicle passing a tolling station in either direction. Its timestamp identifies an instant. The application converts that instant to City Local Time for calculation.
_Avoid_: Entry, exit, transaction

**Vehicle Type**:
A database-defined category with one consistent meaning across all cities. A Tax Rule Set can make the category taxable or exempt. Initial codes are Other, Emergency, Bus, Diplomat, Motorcycle, Military, and Foreign; new content can add other codes.
_Avoid_: Vehicle class, exemption type

**Tax Rule**:
A rule that determines if a passage is taxable and which charge applies.
_Avoid_: Parameter, setting

**Charge Window**:
A period that starts with its first passage and includes applicable passages no later than 60 minutes after that first passage.
_Avoid_: Sliding window, chained group

**Daily Tax**:
The congestion tax for one vehicle on one local calendar date, after the single charge rule and daily maximum apply.
_Avoid_: Daily fee, daily charge

**City Local Time**:
The local civil date and time in the IANA time zone of the city for which congestion tax is calculated.
_Avoid_: Server time, system time
