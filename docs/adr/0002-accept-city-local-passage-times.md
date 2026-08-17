# Accept City Local Passage Times from the Request

The API requires one IANA time zone and Passage timestamps in `uuuu-MM-dd HH:mm:ss` format. Each timestamp is City Local Time.

The application accepts the request time zone as correct. The HTTP adapter keeps the supplied City Local Time and derives an instant for ordering and elapsed-time calculations.

The City table does not store a time zone. The application does not compare the request time zone with stored City content.

Missing and repeated local times during daylight-saving changes are outside the supported input contract.
