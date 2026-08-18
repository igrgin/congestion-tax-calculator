INSERT INTO vehicle_type (code, description)
VALUES
    ('EMERGENCY', 'Emergency vehicle'),
    ('BUS', 'Bus'),
    ('DIPLOMAT', 'Diplomat vehicle'),
    ('MOTORCYCLE', 'Motorcycle'),
    ('MILITARY', 'Military vehicle'),
    ('FOREIGN', 'Foreign vehicle');

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
        (TIME '06:30:00', TIME '07:00:00', 13.00),
        (TIME '07:00:00', TIME '08:00:00', 18.00),
        (TIME '08:00:00', TIME '08:30:00', 13.00),
        (TIME '08:30:00', TIME '15:00:00', 8.00),
        (TIME '15:00:00', TIME '15:30:00', 13.00),
        (TIME '15:30:00', TIME '17:00:00', 18.00),
        (TIME '17:00:00', TIME '18:00:00', 13.00),
        (TIME '18:00:00', TIME '18:30:00', 8.00),
        (TIME '18:30:00', TIME '06:00:00', 0.00)
) AS tax_time_band (start_time, end_time, amount)
WHERE city.code = 'gothenburg';

INSERT INTO tax_rule_option (
    rule_set_id,
    type_code,
    amount,
    duration_minutes,
    preceding_days,
    description
)
SELECT
    tax_rule_set.id,
    tax_rule_option.type_code,
    tax_rule_option.amount,
    tax_rule_option.duration_minutes,
    tax_rule_option.preceding_days,
    tax_rule_option.description
FROM tax_rule_set
JOIN city
    ON city.id = tax_rule_set.city_id
CROSS JOIN (
    VALUES
        (
            'CHARGE_WINDOW',
            NULL::NUMERIC,
            60,
            NULL::SMALLINT,
            'Highest Tax Amount in each 60-minute Charge Window'
        ),
        (
            'DAILY_MAXIMUM',
            60.00,
            NULL::INTEGER,
            NULL::SMALLINT,
            'Maximum Daily Tax of 60.00 SEK'
        ),
        (
            'HOLIDAY_PRECEDING',
            NULL::NUMERIC,
            NULL::INTEGER,
            1,
            'One tax-free date before each public holiday'
        )
) AS tax_rule_option (
    type_code,
    amount,
    duration_minutes,
    preceding_days,
    description
)
WHERE city.code = 'gothenburg';

INSERT INTO tax_exemption (
    rule_set_id,
    type_code,
    day_of_week,
    month_number,
    description
)
SELECT
    tax_rule_set.id,
    tax_exemption.type_code,
    tax_exemption.day_of_week,
    tax_exemption.month_number,
    tax_exemption.description
FROM tax_rule_set
JOIN city
    ON city.id = tax_rule_set.city_id
CROSS JOIN (
    VALUES
        ('WEEKDAY', 6, NULL::SMALLINT, 'Saturday'),
        ('WEEKDAY', 7, NULL::SMALLINT, 'Sunday'),
        ('MONTH', NULL::SMALLINT, 7, 'July')
) AS tax_exemption (
    type_code,
    day_of_week,
    month_number,
    description
)
WHERE city.code = 'gothenburg';

INSERT INTO tax_exemption (
    rule_set_id,
    type_code,
    vehicle_type_code,
    description
)
SELECT
    tax_rule_set.id,
    'VEHICLE_TYPE',
    vehicle_type.code,
    'Tax-free ' || vehicle_type.description
FROM tax_rule_set
JOIN city
    ON city.id = tax_rule_set.city_id
CROSS JOIN (
    VALUES
        ('EMERGENCY'),
        ('BUS'),
        ('DIPLOMAT'),
        ('MOTORCYCLE'),
        ('MILITARY'),
        ('FOREIGN')
) AS exempt_vehicle_type (code)
JOIN vehicle_type
    ON vehicle_type.code = exempt_vehicle_type.code
WHERE city.code = 'gothenburg';

INSERT INTO tax_exemption (
    rule_set_id,
    type_code,
    holiday_date,
    description
)
SELECT
    tax_rule_set.id,
    'PUBLIC_HOLIDAY',
    public_holiday.holiday_date,
    public_holiday.description
FROM tax_rule_set
JOIN city
    ON city.id = tax_rule_set.city_id
CROSS JOIN (
    VALUES
        (DATE '2013-01-01', 'New Year''s Day'),
        (DATE '2013-01-06', 'Epiphany'),
        (DATE '2013-03-29', 'Good Friday'),
        (DATE '2013-03-31', 'Easter Sunday'),
        (DATE '2013-04-01', 'Easter Monday'),
        (DATE '2013-05-01', 'May Day'),
        (DATE '2013-05-09', 'Ascension Day'),
        (DATE '2013-05-19', 'Whit Sunday'),
        (DATE '2013-06-06', 'National Day of Sweden'),
        (DATE '2013-06-22', 'Midsummer Day'),
        (DATE '2013-11-02', 'All Saints'' Day'),
        (DATE '2013-12-25', 'Christmas Day'),
        (DATE '2013-12-26', 'Boxing Day')
) AS public_holiday (holiday_date, description)
WHERE city.code = 'gothenburg';
