# HTTP API Design

This document defines how a caller requests one Congestion Tax Calculation and receives its result.

## Operation

```text
POST /api/v1/cities/{cityCode}/congestion-tax/calculations
```

One request contains one database-defined Vehicle Type and one or more Passages for one vehicle.

The calculator does not store registration plates, owners, Passages, or results.

```json
{
  "vehicleType": "OTHER",
  "passages": [
    "2013-02-08 06:10:00",
    "2013-02-08 06:20:00"
  ]
}
```

```json
{
  "cityCode": "gothenburg",
  "vehicleType": "OTHER",
  "currency": "SEK",
  "totalAmount": 16.00,
  "dailyTaxes": [
    {
      "date": "2013-02-08",
      "taxExemptionReasons": [],
      "amount": 16.00
    }
  ]
}
```

Daily results are ordered by date and include zero amounts. Each Daily Tax contains the Tax Exemption Reasons that caused its amount to be zero. An empty set means that no Tax Exemption applied.

One response has one currency from the selected City's Tax Rule Set.

The city code in the path selects the stored rules without changing the API contract. OpenAPI JSON and Swagger UI are planned for the later API documentation work.

## Request validation

For a calculation request:

- `vehicleType` is required and must not be blank.
- `passages` is required and must not be empty.
- Each Passage value is required.
- The Passage must use `uuuu-MM-dd HH:mm:ss` format.
- Each Passage City Local Time date must be in 2013.
- Unknown JSON properties are invalid. A removed `timeZone` property is not accepted or ignored.
- Each Passage is a JSON string token. A property-specific Jackson content deserializer strictly converts it to `LocalDateTime` before the controller method runs.
- The deserializer accepts only the exact `uuuu-MM-dd HH:mm:ss` format. It does not trim the value and rejects other Jackson `LocalDateTime` shapes.
- Deserialization stops at the first invalid Passage. Jackson adds its collection index to the error path.
- `CalculationServiceImpl` collects every zero-based Passage index whose City Local Time date is outside 2013. It rejects the complete command and reports all affected indexes in request order.
- The Calculation module derives each Passage instant with the stored City time zone.
- Missing and repeated local times during daylight-saving changes are outside the supported input contract.

Bean Validation checks the reusable transport-shape invariants. Jackson completes timestamp deserialization before the controller method runs. Supported-year validation belongs to the Calculation Service and runs before Tax Rule lookup, calculation logs, and custom metrics. Complete validation aggregation for other request failures belongs to the later API validation work.

`CalculationRequest` stays inside the HTTP adapter and contains `List<LocalDateTime>`. The controller has no parsing or validation logic. It forwards the City Local Time list in `CalculationCommand` and maps the service result to the HTTP response.

## Error responses

The API status contract is:

- HTTP `400` for an invalid request shape, unknown property, Passage timestamp, unsupported Passage year, or unknown Vehicle Type;
- HTTP `404` for an unknown City;
- HTTP `500` for invalid stored content or an unexpected application failure;
- HTTP `503` when PostgreSQL is unavailable.

Every handled error from the calculation operation returns `application/problem+json`. The shared response contains `title`, `status`, `detail`, and a stable top-level `code`. It contains `errors` only when one or more specific errors exist. The JSON omits `errors` when the collection is empty. Each included error has a field, a stable code, and a human-readable message. The responses omit `type`.

`CalculationServiceImpl` owns supported-year input failures. It also translates lower lookup and stored-content failures into calculation-owned exceptions and preserves their causes. Missing Tax Time Bands become `MissingStoredTaxTimeBandsException`, an invalid City time zone becomes `InvalidStoredCityTimeZoneException`, and an overlap becomes `InvalidStoredTaxTimeBandsException` at this boundary. `CalculationExceptionHandler` maps calculation, deserialization, Bean Validation, unreadable-request, and unexpected exceptions to Problem Details. The domain and Tax Rule areas do not depend on Spring Web.

The HTTP exception boundary logs each handled `4xx` response at `WARN` with a stable failure category, safe context, and no stack trace. It logs each handled `5xx` response once at `ERROR` with an internal stack trace. Invalid stored Charge Window or Daily Maximum content uses the `invalid-tax-rule-option` category. Missing Tax Time Bands use `missing-tax-time-bands`. An invalid stored City time zone uses `invalid-city-time-zone`. Overlapping Tax Time Bands use `overlapping-tax-time-bands`. This rule applies to the calculation API exception handler, not to unrelated framework or servlet responses.

A timestamp deserialization failure returns Problem Details JSON and uses the `invalid-passage-timestamp` category at `WARN` without a stack trace. The response reports the first invalid Passage because deserialization stops at the first invalid value.

Malformed JSON returns Problem Details JSON and uses the `invalid-json` category at `WARN` without a stack trace. It omits `errors` because Jackson cannot reliably identify a request field.

Other unreadable request bodies, such as a request with an unknown property, use the `invalid-request` category at `WARN` without a stack trace. They return Problem Details and omit `errors` when no specific field errors are available.

Missing Tax Rules, invalid stored Tax Rule Options, invalid stored Tax Time Bands, invalid stored City time zones, and unexpected failures return the public code `CALCULATION_FAILED`. The response contains no internal failure data. A data-access resource failure or a failure to create the PostgreSQL transaction returns `SERVICE_UNAVAILABLE`. Exception class names, stored boundaries, parser details, SQL, credentials, and stack traces stay out of every response.

The invalid Passage timestamp error shape is:

```json
{
  "title": "Invalid calculation request",
  "status": 400,
  "detail": "The request contains invalid Passages.",
  "code": "INVALID_REQUEST",
  "errors": [
    {
      "field": "passages[1]",
      "code": "INVALID_PASSAGE_TIMESTAMP",
      "message": "A Passage must use the City Local Time format uuuu-MM-dd HH:mm:ss."
    }
  ]
}
```

The malformed JSON error shape is:

```json
{
  "title": "Invalid JSON",
  "status": 400,
  "detail": "The request body is not valid JSON.",
  "code": "INVALID_JSON"
}
```

The supported-year error shape is:

```json
{
  "title": "Invalid calculation request",
  "status": 400,
  "detail": "The request contains invalid Passages.",
  "code": "INVALID_REQUEST",
  "errors": [
    {
      "field": "passages[0]",
      "code": "UNSUPPORTED_PASSAGE_YEAR",
      "message": "A Passage City Local Time date must be in 2013."
    },
    {
      "field": "passages[2]",
      "code": "UNSUPPORTED_PASSAGE_YEAR",
      "message": "A Passage City Local Time date must be in 2013."
    },
    {
      "field": "passages[3]",
      "code": "UNSUPPORTED_PASSAGE_YEAR",
      "message": "A Passage City Local Time date must be in 2013."
    }
  ]
}
```

The supported-year response uses the top-level code `INVALID_REQUEST`. Each field error uses the code `UNSUPPORTED_PASSAGE_YEAR`. Several unsupported Passages produce one error entry for each affected zero-based index, in request order. The supported-year response does not combine unsupported-year errors with other validation failure types. Complete field-error aggregation for the other validation types belongs to the later API issue.

An internal failure response is:

```json
{
  "title": "Calculation failed",
  "status": 500,
  "detail": "The calculation could not be completed.",
  "code": "CALCULATION_FAILED"
}
```

| Condition | HTTP status |
|---|---:|
| Successful calculation | `200` |
| Invalid JSON, request data, Vehicle Type, or local year | `400` |
| Unknown city code | `404` |
| Missing or inconsistent stored rules or City time zone | `500` |
| PostgreSQL unavailable | `503` |
