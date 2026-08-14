# Convert Passage Instants to City Local Time

The API requires `Z` or an explicit UTC offset so each Passage identifies an unambiguous instant. The application stores an IANA time zone for each city and converts each instant to City Local Time before it applies clock-time and calendar rules. A fixed city offset was rejected because daylight-saving and historical offset changes depend on the date.
