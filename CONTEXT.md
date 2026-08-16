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

**Tax Exemption**:
A Tax Rule that makes a Passage tax-free when its City Local Time or Vehicle Type matches stored content.
_Avoid_: Rule exception, exemption type

**Tax Rule Option**:
An optional Tax Rule with one scalar value that changes how the application calculates tax or extends a Tax Exemption. A Tax Rule Set can select each supported option at most once.
_Avoid_: Parameter, setting

**Tax Rule Set**:
One immutable, complete snapshot of the Tax Rules for one city. Its effective date starts its period, and a newer Tax Rule Set ends the preceding period.
_Avoid_: Rule configuration, current rules

**Applicable Tax Rule Set**:
The one Tax Rule Set that controls a calculation date for a city. It has the latest effective date that is not after the calculation date.
_Avoid_: Active rules, current rules

**Tax Time Band**:
A period of City Local Time with one positive tax amount. Its start is included and its end is excluded.
_Avoid_: Rate slot, tariff interval

**Tax Amount**:
A non-negative congestion-tax value expressed in one currency. A Tax Amount cannot be combined with a Tax Amount in another currency.
_Avoid_: Money, Monetary amount

**Charge Window**:
A period that starts with its first passage and includes applicable passages no later than 60 minutes after that first passage.
_Avoid_: Sliding window, chained group

**Daily Tax**:
The congestion tax for one vehicle on one local calendar date, after the single charge rule and daily maximum apply.
_Avoid_: Daily fee, daily charge

**City Local Time**:
The local civil date and time in the IANA time zone of the city for which congestion tax is calculated.
_Avoid_: Server time, system time
