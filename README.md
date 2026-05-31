# ChargeFlow - EV Charging Station Booking System

ChargeFlow is a web application for finding electric vehicle charging stations
and reserving charging time slots. Drivers can register, log in, explore nearby
stations on a map, book a connector, and manage their reservations. Admin users
can manage stations, connectors, and bookings.

The backend is a Spring Boot REST API with PostgreSQL persistence. The frontend
is a static HTML, CSS, and JavaScript client served by the same Spring Boot
application.

## Features

- Driver registration and login with JWT authentication
- Role-based access control for DRIVER and ADMIN users
- Interactive station discovery map using Leaflet and OpenStreetMap
- Connector filtering and available time slot selection
- Booking creation, modification, and cancellation
- Conflict checks to prevent overlapping bookings
- Admin panel for managing stations, connectors, and all bookings
- Flyway database migrations and seed data
- Render-ready deployment configuration

## Tech Stack

- Java 17
- Spring Boot 3.3.5
- Spring Security
- Spring Data JPA
- PostgreSQL 16
- Flyway
- HTML, CSS, and JavaScript
- Leaflet.js with OpenStreetMap tiles
- Docker and Docker Compose
- Render

## Project Structure

```text
ev-booking/
├── docker-compose.yml              Local PostgreSQL database
├── render.yaml                     Render deployment blueprint
├── README.md
└── server/
    ├── Dockerfile                  Production container build
    ├── pom.xml                     Maven project file
    └── src/
        ├── main/java/com/evbooking/server/
        │   ├── admin/              Admin API
        │   ├── auth/               Login, registration, JWT
        │   ├── booking/            Booking API and business rules
        │   ├── config/             Security and request logging
        │   ├── discovery/          Station discovery API
        │   ├── entity/             JPA entities
        │   └── repository/         Spring Data repositories
        └── main/resources/
            ├── db/migration/       Flyway SQL migrations
            └── static/             Frontend pages and assets
```

## Local Setup

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This starts PostgreSQL on `localhost:55432`.

Database details:

```text
database: ev_booking
user:     ev_user
password: dev_password
```

### 2. Run the Server

```bash
cd server
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd server
.\mvnw.cmd spring-boot:run
```

The app runs at:

```text
http://localhost:8080
```

Flyway runs the database migrations automatically when the application starts.

## Seed Accounts

The local seed data creates these users:

| Email | Password | Role |
| --- | --- | --- |
| `driver1@example.com` | `driver123` | DRIVER |
| `driver2@example.com` | `driver123` | DRIVER |
| `admin@example.com` | `admin123` | ADMIN |

## Main Pages

| Page | Purpose |
| --- | --- |
| `/index.html` | Landing page |
| `/register.html` | Driver account registration |
| `/login.html` | User login |
| `/discover.html` | Map-based station discovery and booking |
| `/mybookings.html` | Driver booking management |
| `/admin.html` | Admin station, connector, and booking management |

## API Overview

| Endpoint Group | Purpose |
| --- | --- |
| `/auth` | Login and registration |
| `/api/stations` | Station discovery |
| `/api/connectors` | Connector list and slot availability |
| `/bookings` | Create, view, update, and cancel bookings |
| `/api/admin` | Admin-only station, connector, and booking management |

## Testing

Start PostgreSQL first, then run:

```bash
cd server
./mvnw test
```

On Windows PowerShell:

```powershell
cd server
.\mvnw.cmd test
```

## Deployment on Render

The project includes a `render.yaml` file and a server `Dockerfile` for Render
deployment.

For production, set these environment variables in Render:

| Variable | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host>:<port>/<db>` |
| `SPRING_DATASOURCE_USERNAME` | Render database username |
| `SPRING_DATASOURCE_PASSWORD` | Render database password |
| `JWT_SECRET` | Long random secret for signing JWTs |

Render may provide the database URL in `postgres://` format. The Spring Boot
configuration expects the JDBC format:

```text
jdbc:postgresql://<host>:<port>/<db>
```

## Notes

- The `dev` profile uses the local Docker PostgreSQL database.
- The `prod` profile reads database settings from environment variables.
- Booking times are stored as offset date-times.
- Booking conflict checks are handled on the backend to protect against double
  reservations.
