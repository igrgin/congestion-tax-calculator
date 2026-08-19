# Congestion Tax Calculator

This Spring Boot application calculates congestion tax for one vehicle from its passage times. A caller selects a city and vehicle type, and the application reads that city's tax rules from PostgreSQL.

## What it implements

- An HTTP API for one or more passage times.
- Tax amounts based on local time bands.
- Tax-free weekdays, months, public holidays, dates before public holidays, and vehicle types.
- A charge window that charges only the highest amount in its period.
- A daily maximum and a daily result for each date in the request.
- City-specific tax rules stored outside the Java application.

The database contains the Gothenburg tax rules. These include the daily maximum, the 60-minute charge window, the tax-free calendar rules, and the tax-exempt vehicle types.

## Start the application

You need Java 17 or later and Docker with Docker Compose.

Start PostgreSQL:

```bash
docker compose up -d
```

Start the application:

```bash
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080` with the default `dev` profile.

To remove the local database and its volume:

```bash
docker compose down -v
```

## Make a calculation

```bash
curl --request POST \
  --url http://localhost:8080/api/v1/cities/gothenburg/congestion-tax/calculations \
  --header 'Content-Type: application/json' \
  --data '{
    "vehicleType": "OTHER",
    "passages": [
      "2013-02-08 06:20:27"
    ]
  }'
```

The response is:

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

## Run the tests

Run the regular tests:

```bash
./mvnw test
```

Run all tests and build checks:

```bash
./mvnw verify
```

The full verification uses Testcontainers and needs Docker.

## Inspect the application

- [Swagger UI](http://localhost:8080/swagger-ui.html)
- [OpenAPI JSON](http://localhost:8080/v3/api-docs)
- [Health](http://localhost:8080/actuator/health)
- [Prometheus metrics](http://localhost:8080/actuator/prometheus)

## Documentation

- [Calculation](docs/calculation.md)
- [Architecture](docs/architecture.md)
- [Persistence](docs/persistence.md)
- [HTTP API](docs/api.md)
- [Development and operations](docs/development.md)
- [Project language](CONTEXT.md)
- [Questions](questions.md)
