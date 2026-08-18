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
- HTTP `404` for an unknown City.

An invalid stored City time zone or missing Tax Rule Set is invalid server content and returns HTTP `500`.

`CalculationServiceImpl` owns supported-year input failures. It also translates lower lookup failures into calculation-owned exceptions and preserves their causes. `CalculationExceptionHandler` maps supported-year service exceptions, timestamp deserialization failures, malformed JSON, and other unreadable request bodies to their HTTP responses. Spring handles Bean Validation failures. The domain and Tax Rule areas do not depend on Spring Web.

The HTTP exception boundary logs each handled `4xx` response at `WARN` with a stable failure category, safe context, and no stack trace. It logs each handled `5xx` response once at `ERROR` with an internal stack trace. Invalid stored Charge Window or Daily Maximum content uses the `invalid-tax-rule-option` category and includes only the safe option type code. This rule applies to the calculation API exception handler, not to unrelated framework or servlet responses. The response does not contain internal failure data. The first calculation slice returns an empty HTTP `500` response for an unexpected failure. The later API validation issue replaces that body with the final safe Problem Details response.

A timestamp deserialization failure returns Problem Details JSON and uses the `invalid-passage-timestamp` category at `WARN` without a stack trace. The response reports the first invalid Passage because deserialization stops at the first invalid value.

Malformed JSON returns Problem Details JSON and uses the `invalid-json` category at `WARN` without a stack trace. The response has an empty `errors` list because Jackson cannot reliably identify a request field.

Other unreadable request bodies, such as a request with an unknown property, use the `invalid-request` category at `WARN` without a stack trace. They keep the empty HTTP `400` response. Complete Problem Details for these failures belong to the later API validation issue.

The timestamp, malformed JSON, and supported-year responses use `application/problem+json`, a stable top-level `code`, and an `errors` list. Each field error has a field, a stable code, and a human-readable message. The responses omit `type`. They do not expose exception class names, parser details, SQL, credentials, or stack traces.

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
  "code": "INVALID_JSON",
  "errors": []
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

The supported-year response uses the top-level code `INVALID_REQUEST`. Each field error uses the code `UNSUPPORTED_PASSAGE_YEAR`. Several unsupported Passages produce one error entry for each affected zero-based index, in request order. The supported-year response does not combine unsupported-year errors with other validation failure types. Complete aggregation and Problem Details for the other validation types belong to the later API issue.

| Condition | HTTP status |
|---|---:|
| Successful calculation | `200` |
| Invalid JSON, request data, Vehicle Type, or local year | `400` |
| Unknown city code | `404` |
| Missing or inconsistent stored rules or City time zone | `500` |
| PostgreSQL unavailable | `503` |
