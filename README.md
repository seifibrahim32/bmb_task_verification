# BMB Bank Project

A Spring Boot 4 REST API for managing Customer Information Files (CIFs) and bank accounts,
backed by Microsoft SQL Server, secured with JWT, and protected against XSS.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Structure](#project-structure)
3. [Quick Start with Docker](#quick-start-with-docker)
4. [Running Locally (without Docker)](#running-locally-without-docker)
5. [Database Migrations](#database-migrations)
6. [API Reference](#api-reference)
7. [Testing with Postman](#testing-with-postman)
8. [Swagger UI](#swagger-ui)
9. [Running the Test Suite](#running-the-test-suite)

---

## Tech Stack

| Layer          | Technology                       |
|----------------|----------------------------------|
| Runtime        | Java 17                          |
| Framework      | Spring Boot 4.0.6 / Spring 7     |
| Security       | Spring Security 7 + JJWT 0.12.6  |
| Database       | Microsoft SQL Server 2022        |
| ORM            | Spring Data JPA / Hibernate      |
| Migrations     | Flyway                           |
| Documentation  | SpringDoc OpenAPI 3 (Swagger UI) |
| Build          | Maven 3.9+                       |
| Containers     | Docker + Docker Compose          |

---

## Project Structure

```
bank_project/
├── docker/
│   └── mssql/
│       ├── Dockerfile          # Custom SQL Server image (creates bank_db)
│       ├── entrypoint.sh       # Waits for SQL Server then runs init-db.sql
│       └── init-db.sql         # Creates the bank_db database
├── src/
│   ├── main/
│   │   ├── java/com/bmb/bank_project/
│   │   │   ├── config/         # SecurityConfig, SwaggerConfig
│   │   │   ├── controller/     # CifController, AccountController
│   │   │   ├── dto/            # Request / Response DTOs
│   │   │   ├── model/          # Cif, Account JPA entities
│   │   │   ├── repository/     # CifRepository, AccountRepository
│   │   │   ├── security/       # JwtUtil, JwtAuthenticationFilter, XssFilter
│   │   │   └── service/        # CifService, AccountService
│   │   └── resources/
│   │       ├── db/migration/   # V1__create_cifs_table.sql, V2__create_accounts_table.sql
│   │       ├── application.properties
│   │       └── application-docker.properties
│   └── test/
│       ├── java/               # Unit tests + Integration tests
│       └── resources/
│           └── application-test.properties   # H2 in-memory, Flyway disabled
├── docker-compose.yml
└── pom.xml
```

---

## Quick Start with Docker

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- Ports **1433** and **8080** free on your machine

### 1 — Build and start all services

```bash
Try to use ```./mvnw clean package -DskipTests``` before making Docker image.

docker compose up --build
```

This command will:
1. Build a custom SQL Server 2022 image that auto-creates the `bank_db` database
2. Start SQL Server and wait until it is healthy (up to ~2 minutes on first run)
3. Build the Spring Boot application JAR inside a Maven build container
4. Start the application container on port **8080**

> **First run tip:** SQL Server takes 20–40 seconds to initialise. The `app` service will
> automatically wait (`depends_on: condition: service_healthy`) before starting.

### 2 — Verify services are running

```bash
docker compose ps
```

Expected output:

```
NAME          IMAGE                  STATUS
bank_mssql    bank_project-mssql     Up (healthy)
bank_app      bank_project-app       Up
```

Expected to be non-healthy based on device latency and the taken time to pull each image.

### 3 — Access the application

| Resource       | URL                                      |
|----------------|------------------------------------------|
| API base       | http://localhost:8080/api                |
| Swagger UI     | http://localhost:8080/swagger-ui.html    |
| OpenAPI JSON   | http://localhost:8080/api-docs           |

### 4 — Stop all services

```bash
docker compose down
```

To also remove the SQL Server data volume:

```bash
docker compose down -v
```

---

## Running Locally (without Docker)

### Prerequisites

- Java 17+
- Maven 3.9+
- A running SQL Server instance (local or remote) with a database named `bank_db`

### 1 — Configure the data source

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=bank_db;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=YourPassword123
```

### 2 — Run the application

```bash
./mvnw spring-boot:run
```

The application starts on **http://localhost:8080**.

---

## Database Migrations

Schema is managed by **Flyway**. Migrations live in `src/main/resources/db/migration/`:

| Script                         | Description                                  |
|--------------------------------|----------------------------------------------|
| `V1__create_cifs_table.sql`    | Creates the `cifs` table                     |
| `V2__create_accounts_table.sql`| Creates the `accounts` table with FK to CIF  |

**On a fresh database** — Flyway runs V1 then V2 automatically at startup.

**On an existing database** — `baseline-on-migrate=true` with `baseline-version=2` marks the
schema as already at V2 without re-running anything. No manual steps needed in either case.

---

## API Reference

### Base URL

```
http://localhost:8080/api
```

### Authentication

Account endpoints (`/api/accounts/**`) require a **Bearer JWT** token obtained from
`POST /api/cif/authenticate`. Pass it in every request header:

```
Authorization: Bearer <token>
```

---

### CIF Endpoints — no authentication required

#### Register a new CIF

```
POST /api/cif/register
```

**Request body:**
```json
{
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+966512345678"
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "CIF registered successfully.",
  "data": {
    "cifNumber": "CIF-A1B2C3D4",
    "fullName": "John Doe",
    "email": "john.doe@example.com",
    "status": "PENDING_TPIN"
  }
}
```

---

#### Set TPIN

```
POST /api/cif/set-tpin
```

**Request body:**
```json
{
  "cifNumber": "CIF-A1B2C3D4",
  "tpin": "1234",
  "confirmTpin": "1234"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "TPIN set successfully.",
  "data": null
}
```

---

#### Authenticate (get JWT)

```
POST /api/cif/authenticate
```

**Request body:**
```json
{
  "cifNumber": "CIF-A1B2C3D4",
  "tpin": "1234"
}
```

**Response 200 — success:**
```json
{
  "success": true,
  "message": "Authentication successful.",
  "data": {
    "authenticated": true,
    "cifNumber": "CIF-A1B2C3D4",
    "remainingAttempts": null,
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**Response 401 — wrong TPIN:**
```json
{
  "success": false,
  "message": "Invalid TPIN.",
  "data": {
    "authenticated": false,
    "cifNumber": "CIF-A1B2C3D4",
    "remainingAttempts": 2,
    "token": null
  }
}
```

**Response 403 — CIF blocked (3 failed attempts):**
```json
{
  "success": false,
  "message": "CIF is blocked. Please reset your TPIN.",
  "data": null
}
```

---

#### Reset TPIN (unblocks CIF)

```
POST /api/cif/reset-tpin
```

**Request body:**
```json
{
  "cifNumber": "CIF-A1B2C3D4",
  "newTpin": "5678",
  "confirmTpin": "5678"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "TPIN reset successfully.",
  "data": null
}
```

---

### Account Endpoints — Bearer JWT required

#### Open a new account

```
POST /api/accounts
Authorization: Bearer <token>
```

**Request body:**
```json
{
  "accountNumber": "ACC-001234",
  "holderName": "John Doe",
  "balance": 1500.00,
  "accountType": "SAVINGS"
}
```

`accountType` must be `SAVINGS` or `CURRENT`.

**Response 201:**
```json
{
  "success": true,
  "message": "Account created successfully.",
  "data": {
    "id": 1,
    "accountNumber": "ACC-001234",
    "holderName": "John Doe",
    "balance": 1500.00,
    "accountType": "SAVINGS",
    "cifNumber": "CIF-A1B2C3D4"
  }
}
```

---

#### List all accounts for the authenticated CIF

```
GET /api/accounts
Authorization: Bearer <token>
```

**Response 200:**
```json
{
  "success": true,
  "message": "Accounts retrieved.",
  "data": [
    {
      "id": 1,
      "accountNumber": "ACC-001234",
      "holderName": "John Doe",
      "balance": 1500.00,
      "accountType": "SAVINGS",
      "cifNumber": "CIF-A1B2C3D4"
    }
  ]
}
```

---

#### Get a specific account by ID

```
GET /api/accounts/{id}
Authorization: Bearer <token>
```

**Response 200:**
```json
{
  "success": true,
  "message": "Account retrieved.",
  "data": {
    "id": 1,
    "accountNumber": "ACC-001234",
    "holderName": "John Doe",
    "balance": 1500.00,
    "accountType": "SAVINGS",
    "cifNumber": "CIF-A1B2C3D4"
  }
}
```

---

### Common Error Responses

| HTTP Status | Meaning                                           |
|-------------|---------------------------------------------------|
| 400         | Validation error or invalid state (e.g. TPIN already set) |
| 401         | Missing / invalid JWT, or wrong TPIN              |
| 403         | CIF is blocked                                    |
| 404         | CIF or account not found                          |
| 409         | Duplicate email or account number                 |

---

## Testing with Postman

### Setup

1. Open **Postman** and create a new **Collection** called `BMB Bank Project`.
2. Add a Collection-level **Variable**:
   - `baseUrl` = `http://localhost:8080/api`
3. Add a second Collection-level Variable (leave the value empty for now):
   - `jwtToken` = *(empty)*

---

### Step 1 — Register a CIF

Create a request inside the collection:

| Field         | Value                              |
|---------------|------------------------------------|
| Method        | `POST`                             |
| URL           | `{{baseUrl}}/cif/register`         |
| Body (raw)    | JSON                               |

**Body:**
```json
{
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+966512345678"
}
```

**After sending**, copy the `data.cifNumber` value from the response
(e.g. `CIF-A1B2C3D4`) — you'll need it in the next steps.

Add another collection variable `cifNumber` and paste the value there.

---

### Step 2 — Set TPIN

| Field         | Value                              |
|---------------|------------------------------------|
| Method        | `POST`                             |
| URL           | `{{baseUrl}}/cif/set-tpin`         |
| Body (raw)    | JSON                               |

**Body:**
```json
{
  "cifNumber": "{{cifNumber}}",
  "tpin": "1234",
  "confirmTpin": "1234"
}
```

Expected: `200 OK` with `"success": true`.

---

### Step 3 — Authenticate and save the JWT automatically

| Field         | Value                                  |
|---------------|----------------------------------------|
| Method        | `POST`                                 |
| URL           | `{{baseUrl}}/cif/authenticate`         |
| Body (raw)    | JSON                                   |

**Body:**
```json
{
  "cifNumber": "{{cifNumber}}",
  "tpin": "1234"
}
```

After sending, `{{jwtToken}}` is populated automatically for all subsequent requests.

---

### Step 4 — Open a Bank Account

| Field      | Value                              |
|------------|------------------------------------|
| Method     | `POST`                             |
| URL        | `{{baseUrl}}/accounts`             |
| Body (raw) | JSON                               |
| Headers    | Type: **Bearer Token**, Token: `{{jwtToken}}` |

**Body:**
```json
{
  "accountNumber": "ACC-001234",
  "holderName": "John Doe",
  "balance": 1500.00,
  "accountType": "SAVINGS"
}
```

Expected: `201 Created`.

---

### Step 5 — List All Accounts

| Field     | Value                                         |
|-----------|-----------------------------------------------|
| Method    | `GET`                                         |
| URL       | `{{baseUrl}}/accounts`                        |
| Headers   | Type: **Bearer Token**, Token: `{{jwtToken}}` |

Expected: `200 OK` with an array of accounts.

---

### Step 6 — Get a Specific Account

| Field     | Value                                         |
|-----------|-----------------------------------------------|
| Method    | `GET`                                         |
| URL       | `{{baseUrl}}/accounts/1`                      |
| Headers   | Type: **Bearer Token**, Token: `{{jwtToken}}` |

Replace `1` with the actual account `id` from Step 4's response.

---

### Error Scenario — Wrong TPIN (block flow)

Send `POST /api/cif/authenticate` three times with `"tpin": "0000"` and observe:

| Attempt | Expected Status | `remainingAttempts` |
|---------|-----------------|---------------------|
| 1st     | 401             | 2                   |
| 2nd     | 401             | 1                   |
| 3rd     | 403 (blocked)   | —                   |

After the CIF is blocked, send `POST /api/cif/reset-tpin` with a new TPIN to unblock it.

---

### Error Scenario — No / Invalid JWT

Send `GET /api/accounts` without the `Authorization` header.

Expected:
```json
{
  "success": false,
  "message": "Unauthorized",
  "data": null
}
```
Status: **401 Unauthorized**.

---

## Swagger UI

The interactive API documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

To call a protected endpoint directly from Swagger:

1. Call `POST /api/cif/authenticate` and copy the token from the response.
2. Click the **Authorize** button (padlock icon) at the top right.
3. Paste the token into the **bearerAuth** field (without the `Bearer ` prefix — Swagger adds it).
4. Click **Authorize**, then close the dialog.
5. All subsequent "Try it out" calls will include the JWT header automatically.

---

## Running the Test Suite

```bash
# All tests (unit + slice + integration)
./mvnw test

# Unit tests only
./mvnw test -Dtest=CifServiceTest

# Controller slice tests
./mvnw test -Dtest=CifControllerTest

# Integration tests (uses H2 in-memory, no Docker needed)
./mvnw test -Dtest=CifIntegrationTest
```

The `test` profile (`application-test.properties`) uses an **H2 in-memory database** with
Hibernate `create-drop`, so no SQL Server or Docker is needed to run the tests.

## Important note

Make sure to try the ```docker compose up``` multiple times in root project locally if it failed multiple times since MSSQL installation would take time and other things.

## Postman collection testing

Here is a Postman collection to try API endpoints.

https://documenter.getpostman.com/view/43888644/2sBXqNmdwv

