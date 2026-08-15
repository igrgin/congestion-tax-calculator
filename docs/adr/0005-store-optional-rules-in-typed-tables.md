---
status: superseded by ADR-0006
---

# Store optional rules in typed tables

Store the Daily Maximum Rule and Charge Window Rule in separate optional one-to-one tables owned by a Tax Rule Set. Row presence enables the rule, and row absence disables it. This avoids nullable rule values, keeps each value strongly typed, and lets different cities omit either rule without an application change.
