-- Test users
-- driver passwords: driver123; admin password: admin123
INSERT INTO users (email, password_hash, role) VALUES
    ('driver1@example.com', '$2a$10$ReFXgUEi43ku3Vclr3yIXeQsbWQvpwN9ZsKbZP4BAuMJUhauvx922', 'DRIVER'),
    ('driver2@example.com', '$2a$10$ReFXgUEi43ku3Vclr3yIXeQsbWQvpwN9ZsKbZP4BAuMJUhauvx922', 'DRIVER'),
    ('admin@example.com',   '$2a$10$2Xb8TdKuCvTxsBWg4UIY/.9Wf4xqoqGis4teAEsf.x4UiW73hkm1O',  'ADMIN');

-- Demo stations around Athens
INSERT INTO stations (name, address, latitude, longitude) VALUES
    ('Syntagma Charge Hub', 'Syntagma Square, Athens 105 63',    37.9755, 23.7348),
    ('Piraeus Port Charge', 'Akti Miaouli, Piraeus 185 38',      37.9447, 23.6426),
    ('Marousi Power Stop',  'Kifisias Avenue, Marousi 151 25',   38.0561, 23.8080);

-- Connectors per station
INSERT INTO connectors (station_id, connector_type, max_kw) VALUES
    (1, 'TYPE2',   22.00),
    (1, 'CCS',     50.00),
    (1, 'CCS',     150.00),
    (2, 'TYPE2',   22.00),
    (2, 'CHADEMO', 50.00),
    (3, 'TYPE2',   11.00),
    (3, 'TESLA',   250.00);
