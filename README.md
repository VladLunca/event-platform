# Event Platform

A microservices-based platform for managing artistic events, ticket packages, and client bookings.

---

## Table of Contents

- [Architecture](#architecture)
- [Services](#services)
- [Security Model](#security-model)
- [JWT Token Structure](#jwt-token-structure)
- [gRPC Contract](#grpc-contract)
- [REST API — Auth Service](#rest-api--auth-service)
- [REST API — Event Service](#rest-api--event-service)
- [REST API — Client Service](#rest-api--client-service)
- [Database Schemas](#database-schemas)
- [Environment Variables](#environment-variables)
- [Running with Docker](#running-with-docker)
- [Local Development](#local-development)
- [Design Principles](#design-principles)

---

## Architecture

```
                         ┌──────────────────────────────────────────────────┐
                         │                event-platform-net                 │
                         │                                                   │
                         │   ┌─────────────────────────────────────────┐    │
                         │   │         nginx reverse proxy (:80)        │    │
Angular (:4200) ──HTTP──►│   │  /api/auth/   → auth-service:8080        │    │
                         │   │  /api/events/ → event-service:8080       │    │
                         │   │  /api/clients/→ client-service:8081      │    │
                         │   └─────────────────────────────────────────┘    │
                         │          │              │              │          │
                         │          ▼              ▼              ▼          │
                         │   auth-service   event-service  client-service   │
                         │   REST :8080     REST :8080      REST :8081       │
                         │   gRPC :9090         │                │           │
                         │        ▲             │                │           │
                         │        └─────────────┴────────────────┘          │
                         │              gRPC token validation                │
                         │                                                   │
                         │   auth-db        event-db       client-db        │
                         │   MySQL:3306     MySQL:3306     MongoDB:27017     │
                         └──────────────────────────────────────────────────┘
```

### Communication Patterns

| Pattern | Used for | Protocol | Why |
|---------|----------|----------|-----|
| Client ↔ nginx | All external requests | HTTP/1.1 + JSON | Universal browser support |
| nginx → backend | Reverse proxy | HTTP/1.1 | Single entry point, no CORS |
| event-service → auth-service | Token validation per request | gRPC (HTTP/2 + Protobuf) | ~10x faster than REST, strict contract |
| client-service → auth-service | Token validation per request | gRPC (HTTP/2 + Protobuf) | Same reasoning |
| client-service → event-service | Ticket validation (chain pattern) | REST | Cross-domain resource access |

---

## Services

### auth-service
- **Role**: Identity Management (IDM) — the single source of truth for authentication and authorization
- **Stack**: Java 25, Spring Boot 4.x, Spring Data JPA, Spring Security Crypto
- **Database**: MySQL (`users` table)
- **Exposes**: REST API on `:8080`, gRPC server on `:9090`
- **Responsibilities**:
  - User lifecycle management (create, authenticate)
  - JWT token issuance, validation, and revocation
  - Token blacklist (in-memory; survives until service restart)

### event-service
- **Role**: Events, packages, and tickets management
- **Stack**: Java 25, Spring Boot 4.x, Spring Data JPA
- **Database**: MySQL (`events`, `packages`, `tickets`, `package_events` tables)
- **Exposes**: REST API on `:8080`
- **Responsibilities**:
  - CRUD for events and event packages
  - Ticket sales and validation
  - Enforces ownership rules (only the owner can modify their events/packages)
  - Calls auth-service via gRPC to validate tokens on every request

### client-service
- **Role**: Client profiles and purchased ticket management
- **Stack**: Java 25, Spring Boot 4.x, Spring Data MongoDB
- **Database**: MongoDB (`clients` collection)
- **Exposes**: REST API on `:8081`
- **Responsibilities**:
  - Client profile management (personal data, optional social links)
  - Purchased ticket history (short/full format)
  - Calls auth-service via gRPC for token validation
  - Calls event-service REST API for ticket validation and event details

### frontend
- **Role**: Single Page Application
- **Stack**: Angular, SCSS, nginx
- **Exposes**: HTTP on `:80` (mapped to host `:4200`)
- **Responsibilities**:
  - User interface for all three roles (admin, owner-event, client)
  - Communicates exclusively through nginx reverse proxy (no direct backend calls)

---

## Security Model

### Roles and Permissions

| Role | Capabilities |
|------|-------------|
| `ADMIN` | Create users with role `OWNER_EVENT` or `CLIENT`; no access to event/client data |
| `OWNER_EVENT` | Full CRUD on own events and packages; view public info of clients who bought tickets to own events; view other owners' events (read-only) |
| `CLIENT` | Manage own profile; view purchased tickets; view all active events/packages; purchase tickets |

### Authorization Flow

Every protected endpoint in event-service and client-service follows this flow:

```
HTTP Request + Authorization: Bearer <token>
         │
         ▼
Extract token from header
         │
         ▼
gRPC Validate(token) → auth-service
         │
         ├── valid=false → 401 Unauthorized
         │
         └── valid=true → { userId, role }
                  │
                  ├── role doesn't permit action → 403 Forbidden
                  │
                  └── authorized → process request
```

### Token Transport

Tokens are passed via the standard HTTP `Authorization` header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## JWT Token Structure

Tokens follow the **JWS (JSON Web Signature)** standard — RFC 7515/7519.  
Format: `base64url(header).base64url(payload).signature`

### Header
```json
{ "alg": "HS256", "typ": "JWT" }
```

### Payload (Claims)

| Claim | Type | Description |
|-------|------|-------------|
| `sub` | standard | User ID (subject) |
| `iss` | standard | Issuer URL of the auth-service |
| `exp` | standard | Expiration timestamp (default: 24h) |
| `iat` | standard | Issued-at timestamp |
| `jti` | standard | Unique token identifier (UUID) |
| `role` | custom | User role: `ADMIN`, `OWNER_EVENT`, or `CLIENT` |

### Signature
```
HMAC-SHA256(base64url(header) + "." + base64url(payload), secretKey)
```

The secret key is a 256-bit hex string configured via environment variable `JWT_SECRET`.

### Token Blacklist
On logout, the token is added to an in-memory `ConcurrentHashMap` set in auth-service. Every `Validate` gRPC call checks the blacklist before verifying the JWT signature. Tokens remain blacklisted until service restart.

---

## gRPC Contract

Defined in `auth.proto` — shared across all three services.

```protobuf
syntax = "proto3";
package auth;

option java_package = "com.example.auth.grpc";

service AuthService {
  rpc Login    (LoginRequest)    returns (LoginResponse);
  rpc Validate (ValidateRequest) returns (ValidateResponse);
  rpc Logout   (LogoutRequest)   returns (LogoutResponse);
}

message LoginRequest  { string username = 1; string password = 2; }
message LoginResponse { string token    = 1; }

message ValidateRequest  { string token   = 1; }
message ValidateResponse { bool valid     = 1; string user_id = 2; string role = 3; }

message LogoutRequest  { string token   = 1; }
message LogoutResponse { bool   success = 1; }
```

All RPCs use the **Unary RPC** pattern — one request, one response.

---

## REST API — Auth Service

Base path: `/auth`  
Port: `8080` (internal), `8090` (host, for debugging)

### Endpoints

| Method | Path | Auth | Status codes | Description |
|--------|------|------|-------------|-------------|
| `POST` | `/auth/login` | Public | 200, 401 | Authenticate user, receive JWT |
| `POST` | `/auth/logout` | Bearer | 200 | Invalidate token (add to blacklist) |
| `POST` | `/auth/users` | ADMIN Bearer | 201, 403, 409 | Create new user |

### POST `/auth/login`

Request body:
```json
{ "email": "admin@platform.com", "password": "admin123" }
```

Response `200`:
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.xxx" }
```

Response `401`:
```json
{ "error": "Credentiale invalide" }
```

### POST `/auth/logout`

Headers:
```
Authorization: Bearer <token>
```

Response `200`:
```json
{ "success": true }
```

### POST `/auth/users`

Headers:
```
Authorization: Bearer <admin_token>
```

Request body:
```json
{ "email": "owner@platform.com", "password": "secret123", "role": "OWNER_EVENT" }
```

Response `201`:
```json
{ "message": "User creat cu succes" }
```

Response `403`:
```json
{ "error": "Acces interzis" }
```

Response `409`:
```json
{ "error": "Email deja existent" }
```

---

## REST API — Event Service

Base path: `/api/event-manager`  
Port: `8080`  
All endpoints require `Authorization: Bearer <token>` unless noted.

### Events

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/events` | Any | List events (paginated, filterable) |
| `GET` | `/events/{id}` | Any | Get event by ID |
| `POST` | `/events` | OWNER_EVENT | Create event (ID_OWNER set from token) |
| `PUT` | `/events/{id}` | OWNER_EVENT (own) | Replace event |
| `PATCH` | `/events/{id}` | OWNER_EVENT (own) | Partial update |
| `DELETE` | `/events/{id}` | OWNER_EVENT (own) | Delete event |

#### Query parameters for `GET /events`
| Parameter | Description |
|-----------|-------------|
| `name` | Partial name match |
| `location` | Partial location match |
| `available_tickets` | Minimum available tickets |
| `page` | Page index |
| `items_per_page` | Items per page (default: 10) |

### Event Packages

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/event-packets` | Any | List packages (paginated, filterable) |
| `GET` | `/event-packets/{id}` | Any | Get package by ID |
| `POST` | `/event-packets` | OWNER_EVENT | Create package |
| `PUT` | `/event-packets/{id}` | OWNER_EVENT (own) | Replace package |
| `PATCH` | `/event-packets/{id}` | OWNER_EVENT (own) | Partial update |
| `DELETE` | `/event-packets/{id}` | OWNER_EVENT (own) | Delete package |

### Navigation routes

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/events/{id}/event-packets` | Packages containing this event |
| `GET` | `/event-packets/{id}/events` | Events in this package |
| `POST` | `/event-packets/{id}/events/{eid}` | Add event to package |
| `DELETE` | `/event-packets/{id}/events/{eid}` | Remove event from package |

### Tickets

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/tickets/{code}` | Any | Get ticket by code |
| `POST` | `/events/{id}/tickets` | CLIENT | Purchase ticket for event |
| `POST` | `/event-packets/{id}/tickets` | CLIENT | Purchase ticket for package |
| `GET` | `/events/{id}/tickets` | OWNER_EVENT (own) | All tickets for event |
| `DELETE` | `/tickets/{code}` | OWNER_EVENT (own) | Invalidate ticket |

### Business rules enforced
- Available tickets for a package ≤ minimum available tickets across constituent events
- Ticket purchase not allowed if `seatCount` is null
- Ticket count cannot be edited once at least one ticket has been sold
- Tickets cannot exceed the configured maximum

### HATEOAS links

All resource representations include `_links`:
```json
{
  "id": 1,
  "name": "Concert Iași",
  "_links": {
    "self":   { "href": "/api/event-manager/events/1" },
    "parent": { "href": "/api/event-manager/events" },
    "tickets": { "href": "/api/event-manager/events/1/tickets", "type": "GET" },
    "packages": { "href": "/api/event-manager/events/1/event-packets", "type": "GET" }
  }
}
```

---

## REST API — Client Service

Base path: `/api/client-manager`  
Port: `8081`  
All endpoints require `Authorization: Bearer <token>` unless noted.

### Client Profile

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/clients/{email}` | CLIENT (own) or OWNER_EVENT | Get client profile |
| `POST` | `/clients` | CLIENT | Create profile |
| `PATCH` | `/clients/{email}` | CLIENT (own) | Update profile fields |
| `DELETE` | `/clients/{email}` | CLIENT (own) | Delete profile |

### Tickets

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/clients/{email}/tickets` | CLIENT (own) | List tickets (short format) |
| `GET` | `/clients/{email}/tickets/{code}` | CLIENT (own) | Get ticket details (full format) |

#### Ticket formats

**Short format** — just the ticket code:
```json
{ "code": "TKT-2025-ABC123" }
```

**Full format** — enriched via call to event-service:
```json
{
  "code": "TKT-2025-ABC123",
  "event": "Concert Iași",
  "location": "Sala Palatului",
  "package_events": ["Concert 1", "Spectacol 2"]
}
```

### Chain call pattern (client-service → event-service)

When a client requests full ticket details, client-service calls event-service:
```
CLIENT request → client-service → event-service /validate-ticket/{code}
                                               ↓
                                    returns event/package details
                                               ↓
                      client-service assembles full response → CLIENT
```

---

## Database Schemas

### Auth Service — MySQL

**`users`**

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | INT | PK, AUTO_INCREMENT | |
| `email` | VARCHAR | UNIQUE, NOT NULL | Used as username |
| `parola` | VARCHAR | NOT NULL | BCrypt hashed |
| `rol` | ENUM | NOT NULL | `ADMIN`, `OWNER_EVENT`, `CLIENT` |

### Event Service — MySQL

**`events`**

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | INT | PK, AUTO_INCREMENT | |
| `owner_id` | INT | NOT NULL | FK → users.id |
| `name` | VARCHAR | UNIQUE, NOT NULL | |
| `location` | VARCHAR | NULLABLE | |
| `description` | VARCHAR | NULLABLE | |
| `seat_count` | INT | NULLABLE | Must be set before ticket sales |

**`packages`**

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | INT | PK, AUTO_INCREMENT | |
| `owner_id` | INT | NOT NULL | FK → users.id |
| `name` | VARCHAR | UNIQUE, NOT NULL | |
| `location` | VARCHAR | NULLABLE | |
| `description` | VARCHAR | NULLABLE | |
| `seat_count` | INT | NULLABLE | ≤ min(seat_count) of constituent events |

**`tickets`**

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `code` | VARCHAR | PK | Generated by application logic |
| `package_id` | INT | FK, NULLABLE | References `packages.id` |
| `event_id` | INT | FK, NULLABLE | References `events.id` |

**`package_events`** (package ↔ event many-to-many)

| Column | Type | Constraints |
|--------|------|-------------|
| `package_id` | INT | PK, FK → packages.id |
| `event_id` | INT | PK, FK → events.id |

### Client Service — MongoDB

**`clients` collection**

```json
{
  "_id": "ObjectID",
  "email": "client@example.com",
  "prenume": "Ion",
  "nume": "Popescu",
  "info_publica": true,
  "social_media": {
    "linkedin": "https://linkedin.com/in/...",
    "public": false
  },
  "bilete": [
    "TKT-2025-ABC123",
    "TKT-2025-DEF456"
  ]
}
```

Fields `prenume`, `nume`, `social_media` are optional and may be absent from the document.  
`info_publica` flag controls visibility to users with `OWNER_EVENT` role.

---

## Environment Variables

Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

| Variable | Default | Description |
|----------|---------|-------------|
| `AUTH_DB_NAME` | `auth_db` | Auth service MySQL database name |
| `AUTH_DB_USER` | `auth_user` | Auth service DB username |
| `AUTH_DB_PASSWORD` | — | Auth service DB password |
| `AUTH_DB_ROOT_PASSWORD` | — | MySQL root password |
| `AUTH_DB_PORT` | `3306` | Host port mapped to auth-db container |
| `EVENT_DB_NAME` | `event_db` | Event service MySQL database name |
| `EVENT_DB_USER` | `event_user` | Event service DB username |
| `EVENT_DB_PASSWORD` | — | Event service DB password |
| `EVENT_DB_ROOT_PASSWORD` | — | MySQL root password |
| `EVENT_DB_PORT` | `3307` | Host port mapped to event-db container |
| `CLIENT_DB_NAME` | `client_db` | Client service MongoDB database name |
| `CLIENT_DB_USER` | `client_user` | MongoDB username |
| `CLIENT_DB_PASSWORD` | — | MongoDB password |
| `CLIENT_DB_PORT` | `27017` | Host port mapped to client-db container |
| `JWT_SECRET` | — | 256-bit hex string (64 hex chars) for HS256 signing |
| `JWT_EXPIRATION_MS` | `86400000` | Token validity in milliseconds (default: 24h) |

---

## Running with Docker

### Prerequisites
- Docker Desktop running

### Start all services
```bash
docker compose up --build
```

### Start only databases (for local backend development)
```bash
docker compose up auth-db event-db client-db -d
```

### Rebuild a single service after code changes
```bash
docker compose up --build auth-service
```

### Stop and remove containers
```bash
docker compose down
```

### Stop and remove containers + volumes (wipe all data)
```bash
docker compose down -v
```

### Service URLs

| Service | External URL | Notes |
|---------|-------------|-------|
| Frontend | `http://localhost:4200` | Served by nginx |
| Auth REST | `http://localhost:8090` | Direct access for debugging |
| Event REST | `http://localhost:8080` | |
| Client REST | `http://localhost:8081` | |
| Auth gRPC | `localhost:9090` | Internal only (between containers) |
| Auth DB | `localhost:3306` | MySQL |
| Event DB | `localhost:3307` | MySQL |
| Client DB | `localhost:27017` | MongoDB |

---

## Local Development

### Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 25 | JDK required |
| Maven | 3.9+ | Or use `./mvnw` wrapper |
| Node.js | 20+ LTS | For Angular frontend |
| Angular CLI | latest | `npm install -g @angular/cli` |
| Docker Desktop | latest | Required for databases |

### 1. Start databases
```bash
docker compose up auth-db event-db client-db -d
```

### 2. Run auth-service
```bash
cd auth-service
mvn spring-boot:run
# REST available at http://localhost:8080
# gRPC available at localhost:9090
```

### 3. Run event-service
```bash
cd event-service
mvn spring-boot:run
# REST available at http://localhost:8080
```

### 4. Run client-service
```bash
cd client-service
mvn spring-boot:run
# REST available at http://localhost:8081
```

### 5. Run frontend
```bash
cd frontend
npm install
ng serve
# Available at http://localhost:4200
```

> When running locally, Angular's development proxy (`proxy.conf.json`) should forward `/api/*` calls to the appropriate service ports.

## Design Principles

### MVC Pattern
Each REST service follows the MVC pattern:
- **Model**: JPA entities / MongoDB documents, repositories
- **Controller**: REST routes, HTTP method mapping, request/response handling
- **View (DTO)**: Data Transfer Objects for serialization/deserialization — DTOs contain no business logic

### HATEOAS
Resource representations include navigational links per RFC 8288 and Roy Fielding's REST constraints:
- `self` — URI of the current resource
- `parent` — URI of the container resource
- Action links (state-dependent) — only available actions are included based on current resource state

### HTTP Status Codes

| Code | Used when |
|------|-----------|
| `200 OK` | Successful GET, DELETE with body, successful login/logout |
| `201 Created` | POST that creates a new resource |
| `204 No Content` | Successful DELETE with no body |
| `401 Unauthorized` | Missing, invalid, or expired JWT token |
| `403 Forbidden` | Valid token but insufficient role/ownership |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Unique constraint violation (duplicate email, duplicate event name) |
| `415 Unsupported Media Type` | Wrong content type |
| `422 Unprocessable Content` | Payload format correct but values invalid |

### Layered Architecture (per service)
```
Controller  →  Service  →  Repository
(HTTP/gRPC)    (logic)      (DB)
```
Controllers are thin adapters. Business logic lives exclusively in the Service layer.

---

## Default Admin Account

Created automatically on first startup by `DataSeeder`:

| Field | Value |
|-------|-------|
| Email | `admin@platform.com` |
| Password | `admin123` |
| Role | `ADMIN` |

> **Important**: Change the default admin credentials before any production or shared deployment.


