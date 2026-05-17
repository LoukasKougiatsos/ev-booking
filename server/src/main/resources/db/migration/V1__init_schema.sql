CREATE TYPE user_role      AS ENUM ('DRIVER', 'ADMIN');
CREATE TYPE booking_status AS ENUM ('ACTIVE', 'CANCELLED', 'COMPLETED');
CREATE TYPE connector_type AS ENUM ('TYPE2', 'CCS', 'CHADEMO', 'SCHUKO');

CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          user_role    NOT NULL DEFAULT 'DRIVER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE stations (
    id         BIGSERIAL        PRIMARY KEY,
    name       VARCHAR(255)     NOT NULL,
    address    VARCHAR(500),
    latitude   DOUBLE PRECISION NOT NULL,
    longitude  DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE TABLE connectors (
    id             BIGSERIAL      PRIMARY KEY,
    station_id     BIGINT         NOT NULL REFERENCES stations(id),
    connector_type connector_type NOT NULL,
    max_kw         NUMERIC(6,2)   NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE bookings (
    id           BIGSERIAL      PRIMARY KEY,
    connector_id BIGINT         NOT NULL REFERENCES connectors(id),
    user_id      BIGINT         NOT NULL REFERENCES users(id),
    start_time   TIMESTAMPTZ    NOT NULL,
    end_time     TIMESTAMPTZ    NOT NULL,
    status       booking_status NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_end_after_start CHECK (end_time > start_time)
);

-- Support fast overlap checks (used by the pessimistic-lock booking path)
CREATE INDEX idx_bookings_connector_status ON bookings (connector_id, status);
CREATE INDEX idx_bookings_user_id          ON bookings (user_id);

CREATE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_bookings_updated_at
    BEFORE UPDATE ON bookings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
