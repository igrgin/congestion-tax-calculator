# HTTP API Design

This document defines how a caller requests one Congestion Tax Calculation and receives its result.

## Operation

```text
POST /api/v1/cities/{cityCode}/congestion-tax/calculations
```

One request contains one database-defined Vehicle Type and the passages for one vehicle. The calculator does not store registration plates, owners, passages, or results.

```json
{
  "vehicleType": "OTHER",
  "passages": [
    "2013-02-08T05:20:27Z",
    "2013-02-08T05:27:00Z"
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
      "vehicleExempt": false,
      "amount": 8.00
    }
  ]
}
```

Daily results are ordered by date and include zero amounts. Vehicle exemption is reported for each date because a request can use successive stored rule versions. One response has one currency. If applicable rule versions have different currencies, the application reports invalid server configuration.

The city code in the path selects the stored rules without changing the API contract. Springdoc supplies OpenAPI JSON and Swagger UI for the exact contract.

## Request Validation

- A request has from 1 through 10,000 passages.
- City codes contain lower-case letters, digits, and hyphens.
- Vehicle Type codes contain upper-case letters, digits, and underscores.
- A passage must be non-null ISO 8601 text with `Z` or an explicit UTC offset.
- Each resulting city-local date must be in 2013.
- Incorrect code casing is rejected and is not changed automatically.

The request model keeps passage values as text until validation is complete. This lets the application report all invalid passage indexes instead of stopping at the first timestamp parse failure. After validation, it converts each value to an `Instant`.

When several passages are invalid, the application reports all invalid passage indexes. Any invalid passage rejects the complete request. This includes a passage whose city-local date is outside 2013.

## Error Responses

Errors use Problem Details JSON with a stable `code` and an optional `errors` list. The optional `type` member is omitted. Responses do not expose exception class names, SQL, credentials, or stack traces.

One `@RestControllerAdvice` in the API package handles exceptions for all API controllers. Expected exception types stay close to the application or persistence area that uses them. Controllers do not repeat exception translation. Unexpected failures are logged internally and converted to a safe response.

```json
{
  "title": "Invalid calculation request",
  "status": 400,
  "detail": "The request contains invalid passages.",
  "code": "INVALID_REQUEST",
  "errors": [
    {
      "field": "passages[2]",
      "message": "A passage timestamp must contain Z or an explicit UTC offset."
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
