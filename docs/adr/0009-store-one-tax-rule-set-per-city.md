# Store one Tax Rule Set per City

Store one complete Tax Rule Set for each City and load it without effective-date selection. The database enforces one Tax Rule Set for each City and does not store `effective_from`.

This decision supersedes the effective-dated snapshot design in ADR-0001. Multiple Tax Rule Sets for one City could support rule changes and historical calculations, but the assignment and its six-hour limit do not require that feature.
