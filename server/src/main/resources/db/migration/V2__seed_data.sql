-- Passwords are bcrypt(cost=10) of the string "changeme"
INSERT INTO users (email, password_hash, role) VALUES
    ('admin@evbooking.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN'),
    ('alice@example.com',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'DRIVER'),
    ('bob@example.com',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'DRIVER');

INSERT INTO stations (name, address, latitude, longitude) VALUES
    ('Syntagma Charging Hub', 'Syntagma Square, Athens, 10563', 37.9755, 23.7348),
    ('Piraeus Port Station',  'Akti Miaouli, Piraeus, 18535',  37.9478, 23.6460),
    ('Kifisia EV Point',      'Leoforos Kifisias, 14562',      38.0737, 23.8140);

-- station 1: two connectors, station 2: two connectors, station 3: two connectors
INSERT INTO connectors (station_id, connector_type, max_kw) VALUES
    (1, 'CCS',     150.00),
    (1, 'TYPE2',    22.00),
    (2, 'CCS',     100.00),
    (2, 'CHADEMO',  50.00),
    (3, 'TYPE2',    11.00),
    (3, 'TYPE2',    22.00);

-- A couple of future bookings so the UI has something to show on first launch
INSERT INTO bookings (connector_id, user_id, start_time, end_time, status) VALUES
    (1, 2, NOW() + INTERVAL '1 hour',  NOW() + INTERVAL '2 hours',  'ACTIVE'),
    (3, 3, NOW() + INTERVAL '3 hours', NOW() + INTERVAL '4 hours',  'ACTIVE');
