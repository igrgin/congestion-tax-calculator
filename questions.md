# Questions and Assumptions

The assignment does not answer the questions below. Because answers were not available during implementation, each question includes the assumption used by the submitted solution and the result of that assumption.

## Calculation Rules

### How is the 60-minute single-charge period measured?

**Assumption used:** The period starts with the first Passage in a Charge Window. It includes a Passage exactly 60 minutes later. A later Passage does not restart or extend the window.

**Result:** For Passages at 06:00, 06:50, and 07:40, the first two Passages form one Charge Window. The Passage at 07:40 starts a new Charge Window.

### Can one single-charge period include passages from two calendar dates?

**Assumption used:** Yes. The assignment limits a Charge Window by elapsed time and does not end it at midnight. The first Passage anchors the window even when later Passages have another City Local Time date.

**Result:** Passages before and after midnight can belong to one Charge Window. The window contributes its highest Tax Amount to the City Local Time date of the earliest Passage that has that amount. If several Passages have the same highest amount, the earliest Passage wins. Each distinct input date remains in the response.

### Does a passage outside the hours with a positive amount take part in the single-charge rule?

**Assumption used:** Yes. Every passage at a tolling station takes part, even when its time has an amount of zero.

**Result:** A zero-amount Passage can start a Charge Window and can affect which later Passages belong to that window.

### How do seconds fit into time ranges that are written only with hours and minutes?

**Assumption used:** Each time range includes its start and excludes the start of the next range.

**Result:** The range written as 06:00–06:29 includes values from 06:00:00 up to, but not including, 06:30:00. A passage at 06:29:59 has an amount of 8 SEK. A passage at 06:30:00 has an amount of 13 SEK.

### Can one Tax Time Band cross midnight or cover a full day?

**Assumption used:** Yes. PostgreSQL keeps the existing start time, end time, and amount fields. An end time before the start time means that the band continues through midnight. Equal start and end times define a full-day band that ends at the same local time on the next date.

**Result:** The supplied `18:30–05:59` period is one stored Tax Time Band. Under the existing exclusive-end convention, PostgreSQL stores `18:30:00` as its start and `06:00:00` as its end. The application permits a zero Tax Amount and matches the band on either side of midnight. A full-day Tax Time Band matches every City Local Time and cannot coexist with another Tax Time Band in the same Tax Rule Set.

### How many days before a public holiday are tax-free?

**Assumption used:** Only the calendar day immediately before a public holiday is tax-free.

**Result:** The application checks whether the next calendar date is a public holiday. It does not exempt two or more preceding dates unless another rule also exempts them.

### Which dates are public holidays for the supported period?

**Assumption used:** The application uses the Swedish public holiday calendar for 2013 as calendar data. It also stores 1 January 2014 so that it can evaluate 31 December 2013. The assignment remains the only source of congestion-tax behavior.

**Result:** The application stores the verified dates in PostgreSQL. The README identifies the calendar source. It does not import other congestion-tax rules from an external source.

## Supplied Data and API

### Which time zone applies to the supplied values that have no UTC offset?

**Assumption used:** Each supplied value is City Local Time in Gothenburg. The stored Gothenburg City row supplies `Europe/Stockholm` as its IANA time zone.

**Result:** The supplied list can be sent without modification and is compared directly with the Gothenburg Tax Time Bands. The Calculation module derives an instant from each local value and the stored City time zone when it needs ordering or elapsed time. A caller cannot override the City time zone. Missing and repeated local times during daylight-saving changes are outside the supported input contract.

### Does the supplied list belong to one vehicle?

**Assumption used:** Yes. The complete list is test data for one vehicle.

**Result:** One full-path test calculates the complete list for a taxable vehicle. Smaller tests can use selected values to prove individual rules.

### Does the direction of travel affect the calculation?

**Assumption used:** No. The assignment states that tax applies when vehicles drive into and out of Gothenburg, but it does not provide the direction or station for any passage.

**Result:** Each input value records when one vehicle passes a tolling station. The calculation does not classify the passage as an entry or exit and does not use the time that the vehicle remains inside an area.

### What does a repeated date and time mean?

**Assumption used:** Each value is an independent record of a vehicle passage. The application accepts repeated values and applies the single-charge rule normally.

**Result:** A repeated value does not increase the tax by itself. The application cannot connect it to a different direction or station because the input does not contain that information.

### How does a caller identify the vehicle, owner, and tax category?

**Assumption used:** Vehicle identification, ownership, billing, and classification are responsibilities of the calling system. Each request contains passages for one vehicle and one known vehicle-category code. The calculator validates that the code exists in the database.

**Result:** The API does not accept or store registration plates, owner data, travel direction, or station identifiers. An unknown vehicle-category code makes the request invalid.

### Should the response contain one total or separate results for each date?

**Assumption used:** The response contains both. It gives one result for each local calendar date in the selected city and the total for the complete request.

**Result:** A caller can see how the daily maximum and tax-free dates affected the result without making a separate request for each date.

### Is the 2013 scope only a documented limit, or must the API enforce it?

**Assumption used:** The API reads each Passage timestamp as City Local Time. At the start of the Calculation Service operation, the service enforces the limit before it accesses the Tax Rule Service, writes calculation log entries, or records custom metrics. If one or more Passages are outside 2013, the service rejects the complete request and reports all affected Passage indexes.

**Result:** The API does not return a partial calculation that could be mistaken for the complete tax. The limit applies to Passage dates. Stored supporting dates can be outside 2013, including the public holiday on 1 January 2014 that supports the 31 December 2013 preceding-date Tax Exemption.

## Stored Rules and Application Scope

### Can content editors define new kinds of calculation behavior?

**Assumption used:** No. The application supports a closed set of Tax Rule types. Content editors can change city-specific values and select supported Tax Rules in PostgreSQL. A new kind of calculation behavior requires an application change.

**Result:** Different cities can use different stored weekdays, months, public-holiday dates, Vehicle Type exemptions, Tax Time Bands, daily maximums, and Charge Window durations. The application does not interpret an editor-defined rule language.

### Must every city use a daily maximum and a Charge Window?

**Assumption used:** No. `DAILY_MAXIMUM` and `CHARGE_WINDOW` are optional Tax Rule Options in a complete Tax Rule Set. When present, each option has a positive stored value.

**Result:** A Tax Rule Set without `DAILY_MAXIMUM` has uncapped Daily Tax. A Tax Rule Set without `CHARGE_WINDOW` charges each taxable Passage separately. Different cities can select either option and store different values without an application change.

### Which data must the external data store contain?

**Assumption used:** PostgreSQL contains the city data and congestion-tax rules used by the calculation. The application remains a stateless calculator and does not store passages, vehicles, owners, or calculation results.

**Result:** The calling system keeps vehicle and passage records. The calculator reads the selected City's Tax Rule Set from PostgreSQL at runtime.

### Can one City have several effective-dated Tax Rule Sets?

**Assumption used:** No. Each City has one Tax Rule Set for the supported 2013 calculation scope. The assignment requires different Tax Rules for different cities, but it does not require Tax Rule history or changes during the year.

**Result:** The application loads one Tax Rule Set for the selected City. It has no effective-date selection, history, inheritance, or future publication logic. Multiple effective-dated Tax Rule Sets remain an optional feature that was considered and excluded because it is outside the assignment and its six-hour limit.

### How do content editors update congestion-tax rules?

**Assumption used:** An administration API and editor interface are outside the assignment. Flyway installs the initial content. Later changes use a controlled database migration or an external database administration process. The calculation application only reads the stored content.

**Result:** The submitted API does not include authentication or write operations for congestion-tax rules. PostgreSQL remains the external runtime source of the content.
