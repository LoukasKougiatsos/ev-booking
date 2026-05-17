-- Test users
-- driver passwords: driver123; admin password: admin123
INSERT INTO users (email, password_hash, role) VALUES
    ('driver1@example.com', '$2a$10$ReFXgUEi43ku3Vclr3yIXeQsbWQvpwN9ZsKbZP4BAuMJUhauvx922', 'DRIVER'),
    ('driver2@example.com', '$2a$10$ReFXgUEi43ku3Vclr3yIXeQsbWQvpwN9ZsKbZP4BAuMJUhauvx922', 'DRIVER'),
    ('admin@example.com',   '$2a$10$2Xb8TdKuCvTxsBWg4UIY/.9Wf4xqoqGis4teAEsf.x4UiW73hkm1O',  'ADMIN');

-- Stations around central London
INSERT INTO stations (name, address, latitude, longitude) VALUES
    ('Kings Cross Hub',       '1 Pancras Square, London N1C 4AG',     51.5320, -0.1240),
    ('Westminster Charge',    '15 Victoria Street, London SW1H 0EU',  51.4987, -0.1356),
    ('Shoreditch Power Stop', '10 Hoxton Square, London N1 6NU',      51.5275, -0.0782);

-- Connectors per station
INSERT INTO connectors (station_id, connector_type, max_kw) VALUES
    (1, 'TYPE2',   22.00),
    (1, 'CCS',     50.00),
    (1, 'CCS',     150.00),
    (2, 'TYPE2',   22.00),
    (2, 'CHADEMO', 50.00),
    (3, 'TYPE2',   11.00),
    (3, 'TESLA',   250.00);
