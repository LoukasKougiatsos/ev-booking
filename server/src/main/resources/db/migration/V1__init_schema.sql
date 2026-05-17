-- Users
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(16) NOT NULL CHECK (role IN ('DRIVER', 'ADMIN')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Charging stations
CREATE TABLE stations (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    address         VARCHAR(500),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Connectors
CREATE TABLE connectors (
    id              BIGSERIAL PRIMARY KEY,
    station_id      BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    connector_type  VARCHAR(16) NOT NULL CHECK (connector_type IN ('TYPE2', 'CCS', 'CHADEMO', 'TESLA')),
    max_kw          NUMERIC(6,2) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_connectors_station ON connectors(station_id);

-- Bookings
CREATE TABLE bookings (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    connector_id    BIGINT NOT NULL REFERENCES connectors(id),
    start_time      TIMESTAMPTZ NOT NULL,
    end_time        TIMESTAMPTZ NOT NULL,
    status          VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLED', 'COMPLETED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_end_after_start CHECK (end_time > start_time)
);

-- Trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_bookings_updated_at
    BEFORE UPDATE ON bookings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Indexes for hot queries
CREATE INDEX idx_bookings_connector_time ON bookings(connector_id, start_time, end_time);
CREATE INDEX idx_bookings_user            ON bookings(user_id);
CREATE INDEX idx_bookings_status          ON bookings(status);
