# HTTP API Design

This document defines how a caller requests one Congestion Tax Calculation and receives its result.

## Operation

```text
POST /api/v1/cities/{cityCode}/congestion-tax/calculations
```

One request contains one database-defined Vehicle Type and exactly one Passage for one vehicle. The request uses the final collection-based shape so that later issues can add multiple-Passage calculations without changing the request structure.

The calculator does not store registration plates, owners, Passages, or results.

```json
{
  "vehicleType": "OTHER",
  "passages": [
    "2013-02-08T05:20:27Z"
  ]
}
```

```json
{
  "cityCode": "gothenburg",
  "vehicleType": "OTHER",
  "currency": "SEK",
  "totalAmount": 8.00,
  "dailyTaxes": [
    {
      "date": "2013-02-08",
      "taxExemptionReasons": [],
      "amount": 8.00
    }
  ]
}
```

Daily results are ordered by date and include zero amounts. Each Daily Tax contains the Tax Exemption Reasons that caused its amount to be zero. An empty set means that no Tax Exemption applied.

One response has one currency. If Applicable Tax Rule Sets have different currencies, the application reports invalid server configuration.

The city code in the path selects the stored rules without changing the API contract. OpenAPI JSON and Swagger UI are planned for the later API documentation work.

## Request validation

For the one-Passage calculation:

- `vehicleType` is required and must not be blank.
- `passages` is required and must not be empty.
- Each Passage value is required.
- The request must contain exactly one Passage.
- The Passage must be ISO 8601 text with `Z` or an explicit UTC offset.
- The controller parses the Passage as an offset timestamp and converts it to an `Instant`.
- The City Local Time Service converts the `Instant` to City Local Time with the stored IANA time zone for the selected City.

Bean Validation checks the reusable request invariants. The controller checks the temporary exact-one rule. Complete Problem Details and validation of all Passage indexes belong to the later API validation work.

`CalculationRequest` stays inside the HTTP adapter. The controller parses its Passage values and creates a new unmodifiable Passage Instant list for `CalculationCommand`. `CalculationCommand` stores this list without making another copy. The mutable transport collection does not cross into the Calculation module.

## Error responses

The one-Passage implementation returns:

- HTTP `400` for an invalid request shape, Passage count, Passage timestamp, or unknown Vehicle Type;
- HTTP `404` for an unknown City.

`CalculationServiceImpl` translates lower lookup failures into calculation-owned exceptions and preserves their causes. `CalculationExceptionHandler` maps `CityNotFoundException` to HTTP `404` and `VehicleTypeNotFoundException` to HTTP `400`. Spring handles request-body and Bean Validation failures. The domain, City Local Time, and Tax Rule areas do not depend on Spring Web.

The HTTP exception boundary logs each handled `4xx` response at `WARN` with a stable failure category, safe context, and no stack trace. It logs each handled `5xx` response once at `ERROR` with an internal stack trace. This rule applies to the calculation API exception handler, not to unrelated framework or servlet responses. The response does not contain internal failure data. The first calculation slice returns an empty HTTP `500` response for an unexpected failure. The later API validation issue replaces that body with the final safe Problem Details response.

The final API will use Problem Details JSON with a stable `code` and an optional `errors` list. It will not expose exception class names, SQL, credentials, or stack traces.

The planned final error shape is:

```json
{
  "title": "Invalid calculation request",
  "status": 400,
  "detail": "The request contains invalid Passages.",
  "code": "INVALID_REQUEST",
  "errors": [
    {
      "field": "passages[2]",
      "message": "A Passage timestamp must contain Z or an explicit UTC offset."
    }
  ]
}
```

| Condition | HTTP status |
|---|---:|
| Successful calculation | `200` |
| Invalid JSON, request data, Vehicle Type, or local year | `400` |
| Unknown city code | `404` |
| Missing or inconsistent stored rules | `500` |
| PostgreSQL unavailable | `503` |
