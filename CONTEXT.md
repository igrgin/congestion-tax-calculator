# Congestion Tax Calculation

This context defines the language for calculating congestion tax from vehicle passages with the rules for a selected city.

## Language

**Congestion Tax Calculation**:
A calculation of congestion tax for one vehicle from a set of passages.
_Avoid_: Tax query, fee lookup

**City**:
The place whose Tax Rules and IANA time zone control a Congestion Tax Calculation. A city code selects it.
_Avoid_: Location, jurisdiction

**Passage**:
One recorded occurrence of a vehicle passing a tolling station in either direction. Its timestamp gives the City Local Time, and the selected City supplies its IANA time zone.
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

**Tax Exemption Reason**:
An explanation that identifies one Tax Exemption that applies to a Daily Tax. One Daily Tax can have more than one Tax Exemption Reason.
_Avoid_: Exemption flag, zero reason

**Tax Rule Option**:
An optional Tax Rule with one scalar value that changes how the application calculates tax or extends a Tax Exemption. A Tax Rule Set can select each supported option at most once.
_Avoid_: Parameter, setting

**Public Holiday Preceding-Date Option**:
A Tax Rule Option with a positive count of calendar dates before each stored public holiday that are tax-free.
_Avoid_: Holiday exception, pre-holiday rule

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
A period that starts with its first Passage and includes each Passage no later than its configured duration after that first Passage.
_Avoid_: Sliding window, chained group

**Daily Maximum**:
A Tax Rule Option with a positive Tax Amount that limits the Daily Tax after the application calculates the Passage charges.
_Avoid_: Daily Tax limit, daily cap

**Daily Tax**:
The congestion tax for one vehicle on one local calendar date, after the Charge Window and Daily Maximum apply.
_Avoid_: Daily fee, daily charge

**City Local Time**:
The local civil date and time supplied in a Passage timestamp for the selected city.
_Avoid_: Server time, system time
