# EV Charging Station Booking System

Practice implementation of a cloud-ready booking system for EV charging
stations. Server: Spring Boot + Postgres. Client: static HTML + Leaflet,
served by the same Spring Boot app.

## Structure

```
ev-booking/
├── server/                         Spring Boot REST API (Java 17, Maven)
│   └── src/main/resources/
│       ├── static/                 HTML/JS client (to be added)
│       └── db/migration/           Flyway SQL migrations
├── docs/                           Design notes, ER diagram
└── docker-compose.yml              Local Postgres 16
```

## Local dev setup

### 1. Start Postgres

```bash
docker compose up -d
```

This brings up Postgres 16 on `localhost:5432` with:
- database: `ev_booking`
- user: `ev_user`
- password: `dev_password`

### 2. Run the server

```bash
cd server
./mvnw spring-boot:run
```

The active profile defaults to `dev` (see `application.yaml`), which points
at the local Docker database. Flyway runs the migrations automatically on
startup.

Seed accounts (password: `changeme`):

| email                   | role   |
|-------------------------|--------|
| admin@evbooking.local   | ADMIN  |
| alice@example.com       | DRIVER |
| bob@example.com         | DRIVER |

### 3. Stop Postgres

```bash
docker compose down          # keeps data volume
docker compose down -v       # also wipes the volume
```

## Deployment (Render)

Set the following environment variables in your Render web service:

| Variable                    | Value                                         |
|-----------------------------|-----------------------------------------------|
| `SPRING_PROFILES_ACTIVE`    | `prod`                                        |
| `SPRING_DATASOURCE_URL`     | `jdbc:postgresql://<host>:<port>/<db>`        |
| `SPRING_DATASOURCE_USERNAME`| db user                                       |
| `SPRING_DATASOURCE_PASSWORD`| db password                                   |

> Render's `DATABASE_URL` is in `postgres://` format. Convert it to the
> `jdbc:postgresql://` form before pasting into `SPRING_DATASOURCE_URL`.

## Status

In progress.
