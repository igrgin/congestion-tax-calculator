INSERT INTO city (code, name, time_zone)
VALUES ('london-test', 'London Test', 'Europe/London');

INSERT INTO tax_rule_set (
    city_id,
    currency_code
)
VALUES (
    (
        SELECT id
        FROM city
        WHERE code = 'london-test'
    ),
    'GBP'
);

INSERT INTO tax_time_band (
    rule_set_id,
    start_time,
    end_time,
    amount
)
SELECT
    tax_rule_set.id,
    tax_time_band.start_time,
    tax_time_band.end_time,
    tax_time_band.amount
FROM tax_rule_set
JOIN city
    ON city.id = tax_rule_set.city_id
CROSS JOIN (
    VALUES
        (TIME '00:00:00', TIME '12:00:00', 4.00),
        (TIME '12:00:00', TIME '00:00:00', 7.00)
) AS tax_time_band (start_time, end_time, amount)
WHERE city.code = 'london-test';

INSERT INTO tax_exemption (
    rule_set_id,
    type_code,
    day_of_week,
    description
)
SELECT
    tax_rule_set.id,
    'WEEKDAY',
    1,
    'Monday'
FROM tax_rule_set
JOIN city
    ON city.id = tax_rule_set.city_id
WHERE city.code = 'london-test';

INSERT INTO tax_rule_option (
    rule_set_id,
    type_code,
    amount,
    description
)
SELECT
    tax_rule_set.id,
    'DAILY_MAXIMUM',
    10.00,
    'Maximum Daily Tax of 10.00 GBP'
FROM tax_rule_set
JOIN city
    ON city.id = tax_rule_set.city_id
WHERE city.code = 'london-test';
