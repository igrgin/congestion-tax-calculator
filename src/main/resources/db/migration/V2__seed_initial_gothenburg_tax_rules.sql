INSERT INTO city (code, name, time_zone)
VALUES ('gothenburg', 'Gothenburg', 'Europe/Stockholm');

INSERT INTO vehicle_type (code, description)
VALUES ('OTHER', 'Other vehicle');

INSERT INTO tax_rule_set (
    city_id,
    effective_from,
    currency_code
)
VALUES (
    (
        SELECT id
        FROM city
        WHERE code = 'gothenburg'
    ),
    DATE '2013-01-01',
    'SEK'
);

INSERT INTO tax_time_band (
    rule_set_id,
    start_time,
    end_time,
    amount
)
VALUES (
    (
        SELECT tax_rule_set.id
        FROM tax_rule_set
        JOIN city
            ON city.id = tax_rule_set.city_id
        WHERE city.code = 'gothenburg'
          AND tax_rule_set.effective_from = DATE '2013-01-01'
    ),
    TIME '06:00:00',
    TIME '06:30:00',
    8.00
);
