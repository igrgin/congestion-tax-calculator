# HTTP API

The application exposes one calculation operation:

```text
POST /api/v1/cities/{cityCode}/congestion-tax/calculations
```

One request contains a vehicle type and one or more passage times for one vehicle. The path selects the stored city rules.

## Request

```json
{
  "vehicleType": "OTHER",
  "passages": [
    "2013-02-07 06:23:27",
    "2013-02-07 15:27:00"
  ]
}
```

The request has these rules:

- `vehicleType` is required and must contain a stored code.
- `passages` is required and must contain at least one value.
- Each passage must use the exact `uuuu-MM-dd HH:mm:ss` format.
- Each passage date must be in 2013.
- Passage times are local to the selected city. The request does not contain a time zone or UTC offset.
- Unknown JSON properties are invalid.

The runtime database contains the city code `gothenburg`. Vehicle type codes are `OTHER`, `EMERGENCY`, `BUS`, `DIPLOMAT`, `MOTORCYCLE`, `MILITARY`, and `FOREIGN`. Codes are case-sensitive.

## Response

```json
{
  "cityCode": "gothenburg",
  "vehicleType": "OTHER",
  "currency": "SEK",
  "totalAmount": 21.00,
  "dailyTaxes": [
    {
      "date": "2013-02-07",
      "taxExemptionReasons": [],
      "amount": 21.00
    }
  ]
}
```

The response contains one daily tax for each date in the request and one total. Daily results are ordered by date and include zero amounts. The `currency` value applies to every amount in the response.

An exempt date can contain one or more of these reasons:

- `VEHICLE_TYPE`
- `WEEKDAY`
- `MONTH`
- `PUBLIC_HOLIDAY`
- `DATE_BEFORE_PUBLIC_HOLIDAY`

## Errors

Errors use `application/problem+json`. A response contains a stable `code` and can contain field errors when the application can identify them.

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
    }
  ]
}
```

| Status | Meaning |
|---|---|
| `200` | The calculation completed. |
| `400` | The JSON, request values, passage year, timestamp, or vehicle type is invalid. |
| `404` | The city code does not exist. |
| `500` | Stored rules are missing or invalid, or the calculation failed. |
| `503` | PostgreSQL is unavailable. |

The application returns either a complete calculation or one error. It does not return partial daily results.

## OpenAPI

The running application publishes its generated contract at:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
