# EventApp

A production-grade event booking platform built with Spring Boot microservices. Designed with strong consistency guarantees, reliable async messaging, and a secure BFF gateway — ready to run locally with a single command.

> Patterns used: Saga, Outbox (Debezium CDC), Redis atomic locking, gRPC inter-service communication, OAuth2 BFF with Keycloak.

---

## Architecture Overview

### Services
| Service | Description |
|---|---|
| **user-service** | Fetches and manages users from Keycloak JWT tokens |
| **event-service** | Manages events (create, update, delete, cancel) with Redis + Lua scripts for atomic race condition prevention |
| **booking-service** | Handles ticket reservation with automatic release after TTL, fetches pricing from event-service via gRPC, uses Redis + Lua scripts for atomic inventory locking |
| **payment-service** | Mocks payment processing, fetches booking details via gRPC, reconciles stuck payments, triggers Kafka events via Debezium outbox table |
| **notification-service** | Sends email notifications to users on booking confirmation via Kafka events |

### Infrastructure
| Component | Description |
|---|---|
| **api-gateway** | BFF pattern — OAuth2 login, JWT stored in Redis session, routes and forwards tokens to downstream services |
| **config-service** | Centralized Git-backed configuration for all services |
| **discovery-server** | Eureka service registry |

### Patterns & Tech
- **Saga pattern** — compensating transactions if payment succeeds but a downstream failure occurs
- **Outbox pattern** — used in both booking-service and payment-service for reliable Kafka event publishing via Debezium CDC
- **gRPC** — synchronous inter-service communication (event-service ↔ booking-service, booking-service ↔ payment-service)
- **Kafka + Protobuf** — asynchronous event streaming with Schema Registry
- **Keycloak** — OAuth2 / OIDC identity provider
- **Flyway** — database migrations per service
- **Resilience4j** — circuit breakers and retry

---

## Prerequisites

- Java 17+
- Docker & Docker Compose
- Make

---

## Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/echcherqaoui/eventapp.git
cd eventapp
```

### 2. Configure environment
```bash
cp .env.example .env
```

Open `.env` and fill in all values marked as `your-*`.

Key values to set:
- `PG_USER`, `PG_PASSWORD` — PostgreSQL credentials
- `KC_ADMIN_PASSWORD` — Keycloak admin password
- `RD_PASSWORD` — Redis password
- `CLUSTER_ID` — Kafka cluster ID (generate with `kafka-storage random-uuid`)
- `KC_CLIENT_SECRET`, `IDENTITY_MANAGER_CLIENT_SECRET` — Keycloak client secrets (set after Keycloak setup below)
- `CONFIG_REPO_URL`, `GIT_USERNAME`, `REPO_PAT` — your config repo credentials
- `HMAC_SECRET_KEY` — shared secret between booking, payment, and notification services

### 3. Start infrastructure
```bash
make up-infra
```

Starts: PostgreSQL, Keycloak, Redis, Kafka, Schema Registry, Kafka Connect.

Wait for all containers to be healthy before proceeding.

---

## Keycloak Setup

After `make up-infra`, go to http://localhost:8443 and log in with your admin credentials.

### 1. Create Realm
- Name: `event-app-realm`

### 2. Create Client — `api-gateway` (BFF + User Management)
- Client ID: `api-gateway`
- Client authentication: ON
- Authorization: OFF
- Authentication flow: Standard flow ON, Service accounts ON
- Redirect URI: `http://localhost:8080/login/oauth2/code/keycloak`
- Service account roles: assign `view-users`, `manage-users`, `manage-realm` from `realm-management`
- Copy the client secret → paste into `.env` as `KC_CLIENT_SECRET`

### 3. Create Client — `identity-manager` (user-service admin API)
- Client ID: `identity-manager`
- Client authentication: ON
- Authentication flow: Service accounts only
- Service account roles: assign `view-users`, `manage-users` from `realm-management`
- Copy the client secret → paste into `.env` as `IDENTITY_MANAGER_CLIENT_SECRET`

---

## Finishing Setup

### 4. Register Protobuf schemas
```bash
make register-schemas
```

> Only required on first setup or when `.proto` files change. Schema Registry must be running.

### 5. Register Debezium connectors
```bash
make register-connectors
```

> Only required on first setup. Kafka Connect must be running.

### 6. Build and start the application
```bash
make rebuild-all
```

Builds all Maven modules, builds Docker images, and starts all services.

---

## Available Make Commands

| Command | Description |
|---|---|
| `make up-infra` | Start infrastructure only (Postgres, Keycloak, Redis, Kafka, etc.) |
| `make up-app` | Start application services only |
| `make up-dev-tools` | Start dev tools (pgAdmin, Mailpit, Kafka UI, Grafana) |
| `make up-dev` | Start everything (infra + app + dev tools) |
| `make rebuild-all` | Full Maven build + Docker build + start all services |
| `make rebuild-service SERVICE=<name>` | Rebuild and restart a single service (e.g. `SERVICE=booking-service`) |
| `make register-schemas` | Register Protobuf schemas to Schema Registry |
| `make register-connectors` | Register Debezium connectors to Kafka Connect |
| `make down` | Stop and remove all containers |

---

## Service Ports

| Service | Port |
|---|---|
| API Gateway | `8080` |
| Keycloak | `8443` |
| Discovery Server (Eureka) | `8761` |
| Config Service | `8888` |
| User Service | `8082` |
| Event Service | `8083` |
| Booking Service | `8084` |
| Payment Service | `8085` |
| Notification Service | `8086` |
| Schema Registry | `8181` |
| Kafka | `9096` |
| Kafka Connect | `8183` |
| Kafka UI | `8800` |
| pgAdmin | `8000` |
| Mailpit UI | `8025` |
| Grafana | `3000` |
| Tempo | `3200` |
| Prometheus | `9090` |

---

## Project Structure

```
├── common/
│   ├── booking-api-contract        # Protobuf contracts for booking events
│   ├── event-api-contract          # Protobuf contracts for event events
│   ├── eventapp-common-contracts   # Shared Protobuf + Schema Registry config
│   ├── eventapp-common-security    # HMAC signature verification for inter-service events
│   ├── redis-inventory-lib         # Shared Redis inventory locking logic
│   └── shared-utils                # JWT user extraction (user ID, email) + shared exception handling
├── infrastructure/
│   ├── api-gateway
│   ├── config-service
│   └── discovery-server
├── services/
│   ├── user-service
│   ├── event-service
│   ├── booking-service
│   ├── payment-service
│   └── notification-service
├── docker/
│   ├── infra.yml
│   ├── app.yml
│   ├── dev-tools.yml
│   ├── kafka-connect.Dockerfile
│   ├── prometheus.yml
│   ├── tempo.yml
│   ├── grafana/
│   └── connectors/
│       ├── booking-connector.json
│       ├── payment-connector.json
│       └── register.sh
├── Makefile
└── .env
```