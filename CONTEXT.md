# Congestion Tax Calculator

This glossary defines the terms that have a specific meaning in this project.

## Language

**Passage**:
A recorded time when one vehicle passes a tolling station.
_Avoid_: Entry, exit, transaction

**Vehicle Type**:
A vehicle category used to decide if a vehicle is tax-exempt.
_Avoid_: Vehicle class, exemption type

**Tax Rule Set**:
The complete collection of tax rules for one city.
_Avoid_: Rule configuration

**Tax Time Band**:
A period of local time with one congestion tax amount.
_Avoid_: Rate slot, tariff interval

**Tax Exemption**:
A rule that makes a passage tax-free because of its date or vehicle type.
_Avoid_: Rule exception

**Tax Rule Option**:
An optional rule with one value, such as a duration or maximum amount.
_Avoid_: Parameter, setting

**Charge Window**:
A fixed-length period that groups passages for the single charge rule. Only the highest applicable amount in the period is charged.
_Avoid_: Sliding window, chained group

**Daily Maximum**:
The highest congestion tax amount that one vehicle can pay for one date.
_Avoid_: Daily tax limit, daily cap

**Daily Tax**:
The congestion tax for one vehicle on one date.
_Avoid_: Daily fee, daily charge
