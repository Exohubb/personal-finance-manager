# Personal Finance Manager

A REST API for tracking personal income, expenses, and savings goals — built as
a backend take-home assignment for the Syfe SDE role.

Users register and log in with a session cookie, record transactions against
income/expense categories, set savings goals with live progress tracking, and
pull monthly/yearly reports on their spending. Every user's data is fully
isolated from every other user's.

## Table of contents

- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Architecture](#architecture)
- [How it works](#how-it-works)
- [API reference](#api-reference)
- [Error handling](#error-handling)
- [Testing](#testing)
- [Design decisions](#design-decisions)

## Tech stack

| Component   | Choice |
|-------------|--------|
| Language    | Kotlin |
| Framework   | Spring Boot 4.1.1 |
| Security    | spring-boot-starter-security — session-based auth, BCrypt password hashing |
| Persistence | Spring Data JPA + Hibernate |
| Database    | H2 (in-memory) |
| Build tool  | Maven (with the Maven Wrapper, no local install required) |
| Testing     | JUnit 5, Mockito + mockito-kotlin, JaCoCo for coverage |

<details>
<summary><b>Why Spring Boot 4.1.1 instead of the requested 3.x?</b></summary>

<br>

The assignment brief asks for Spring Boot 3.x. By the time this project was
built, the entire 3.x line — including 3.5, its final release — had reached
end-of-life with no further security patches, and Spring Initializr no longer
offers a 3.x option at all.

Rather than deliberately ship on an unsupported framework version, this project
targets Spring Boot 4.1.1, the current stable release. The main practical
consequence: Boot 4 bundles Jackson 3, whose classes live under a new package
root (`tools.jackson.*` instead of `com.fasterxml.jackson.*`). This only matters
if you're extending the code and need to import a Jackson class directly.

</details>

## Getting started

### Prerequisites

- JDK 17 or newer
- Nothing else — the project ships with the Maven Wrapper (`mvnw`), and the
  database is in-memory, so there's no external service to install or configure.

### Run it locally

```bash
git clone https://github.com/Exohubb/personal-finance-manager.git
cd personal-finance-manager
./mvnw spring-boot:run
```

The API comes up on `http://localhost:8080/api`. The seven default categories
are seeded automatically on startup — there's no setup step beyond starting the app.

Since the database is in-memory, all data resets every time the app restarts.
To poke at it directly while the app is running, open
`http://localhost:8080/h2-console`:

```
JDBC URL: jdbc:h2:mem:financedb
Username: sa
Password: (leave blank)
```

### Run the tests

```bash
./mvnw test
```

Generates a coverage report at `target/site/jacoco/index.html`.

### Run the end-to-end test script

With the app running locally in one terminal, in another:

```bash
bash financial_manager_tests.sh http://localhost:8080/api
```

## Architecture

The code follows a standard layered architecture — each layer only talks to the
one directly below it, which keeps business logic testable in isolation from
HTTP and database concerns:

```
Controller  →  Service  →  Repository  →  Database
   ↓              ↓
  DTOs        Entities
```

```
src/main/kotlin/com/syfe/personalfinancemanager/
├── controller/    HTTP endpoints — parses requests, picks status codes, delegates everything else
├── service/       business rules, validation, and calculations
├── repository/    Spring Data JPA interfaces — database access only
├── entity/        @Entity classes mapped to database tables
├── dto/           request/response shapes exposed by the API
├── security/      session-based authentication setup
├── exception/     custom exceptions + a single @ControllerAdvice mapping them to HTTP status codes
├── config/        startup configuration (default category seeding)
└── util/          small shared helpers (money formatting)
```

Controllers never contain business logic — they extract the current user's ID
from the session, hand the request off to a service, and wrap the result in a
`ResponseEntity`. Services never know about HTTP at all; they take plain
arguments and either return a result or throw one of the app's own exception
types (`BadRequestException`, `ResourceNotFoundException`,
`AppAccessDeniedException`, `ConflictException`), which a single global handler
translates into the right status code.

Entities are never returned directly from a controller — every response is
shaped by a dedicated DTO, so the API's JSON contract stays stable independently
of how data is actually stored.

## How it works

### Authentication

Authentication is session-based, using a `JSESSIONID` cookie rather than a
token/JWT scheme, per the assignment's requirement.

1. `POST /api/auth/register` and `POST /api/auth/login` are the only endpoints
   reachable without a session.
2. On login, Spring Security's `AuthenticationManager` verifies the
   username/password (comparing against a BCrypt hash — plain text passwords
   are never stored), then the server creates an HTTP session and returns a
   `Set-Cookie` header.
3. Every other endpoint requires that cookie. Missing or invalid → `401`.
   Valid, but pointed at another user's resource → `403`.
4. `POST /api/auth/logout` invalidates the session server-side immediately.

### Data isolation

Every query that touches transactions, categories, or goals is scoped to the
logged-in user's ID, which is read out of the Spring Security session context —
never trusted from anything the client sends in a request body. A user can
never see or modify another user's data, even by guessing a valid numeric ID.

### Categories: default vs. custom

A `Category` row has a nullable `owner` field:

- `owner = null` → one of the seven default categories (`Salary` income;
  `Food`, `Rent`, `Transportation`, `Entertainment`, `Healthcare`, `Utilities`
  expense), seeded once at startup, shared by every user, and never editable or
  deletable.
- `owner = <user>` → a custom category created by that specific user. Custom
  category names only need to be unique *per user* — two different users can
  each have their own `Freelance` category without conflict. A custom category
  currently referenced by any transaction cannot be deleted.

### Savings goal progress

Goal progress is never stored as a column — it's recalculated from scratch on
every read:

```
progress = (sum of income transactions) − (sum of expense transactions)
           for all of the user's transactions dated on/after the goal's startDate
```

This means adding, editing, or deleting a transaction changes every affected
goal's progress immediately on the next read, with no separate step to keep a
cached number in sync. `startDate` defaults to the goal's creation date if not
supplied.

### Transaction deletion

Deleting a transaction is a genuine hard delete — the row is removed from the
database, not flagged. Since goal progress and every report are calculated
directly from the current set of transaction rows, a deleted transaction is
automatically excluded everywhere with no extra filtering logic required.

### Reports

Monthly and yearly reports group a user's transactions by category name and sum
each side, using the same aggregation logic for both:

```
netSavings = (total income across all categories) − (total expenses across all categories)
```

## API reference

All endpoints are prefixed with `/api`. All request and response bodies are JSON.

### Auth

| Method | Path | Auth required | Description |
|--------|------|:--:|--------------|
| POST | `/auth/register` | No | Create an account |
| POST | `/auth/login` | No | Start a session |
| POST | `/auth/logout` | Yes | End the current session |

```json
// POST /auth/register
{ "username": "user@example.com", "password": "password123", "fullName": "John Doe", "phoneNumber": "+1234567890" }
```

```json
// POST /auth/login
{ "username": "user@example.com", "password": "password123" }
```

### Transactions

| Method | Path | Description |
|--------|------|--------------|
| POST | `/transactions` | Create a transaction |
| GET | `/transactions` | List transactions, newest first |
| PUT | `/transactions/{id}` | Update amount, category, or description |
| DELETE | `/transactions/{id}` | Delete a transaction |

`GET /transactions` accepts optional query parameters: `startDate`, `endDate`
(`YYYY-MM-DD`), `category` (by name), `type` (`INCOME` or `EXPENSE`).

```json
// POST /transactions
{ "amount": 50000.00, "date": "2024-01-15", "category": "Salary", "description": "January Salary" }
```

Rules:
- `amount` must be strictly positive.
- `date` must be `YYYY-MM-DD` and cannot be in the future (today is allowed).
- `category` must be a default category, or a custom category owned by the caller.
- An update can change `amount`, `category`, and `description`. Any `date`
  field included in an update request is silently ignored — a transaction's
  date can never change after creation.

### Categories

| Method | Path | Description |
|--------|------|--------------|
| GET | `/categories` | List default categories + the caller's own custom ones |
| POST | `/categories` | Create a custom category |
| DELETE | `/categories/{name}` | Delete a custom category by name |

```json
// POST /categories
{ "name": "Freelance", "type": "INCOME" }
```

### Savings goals

| Method | Path | Description |
|--------|------|--------------|
| POST | `/goals` | Create a goal |
| GET | `/goals` | List all goals, with live progress |
| GET | `/goals/{id}` | Get one goal, with live progress |
| PUT | `/goals/{id}` | Update target amount and/or target date |
| DELETE | `/goals/{id}` | Delete a goal |

```json
// POST /goals
{ "goalName": "Emergency Fund", "targetAmount": 5000.00, "targetDate": "2027-01-01", "startDate": "2025-01-01" }
```

```json
// Response
{
  "id": 1,
  "goalName": "Emergency Fund",
  "targetAmount": 5000.00,
  "targetDate": "2027-01-01",
  "startDate": "2025-01-01",
  "currentProgress": 1000.00,
  "progressPercentage": 20.0,
  "remainingAmount": 4000.00
}
```

- `startDate` is optional and defaults to today.
- `targetDate` must be strictly in the future.

### Reports

| Method | Path | Description |
|--------|------|--------------|
| GET | `/reports/monthly/{year}/{month}` | Income/expense breakdown for one month |
| GET | `/reports/yearly/{year}` | Income/expense breakdown for one year |

```json
// GET /reports/monthly/2024/1
{
  "month": 1,
  "year": 2024,
  "totalIncome": { "Salary": 3000.00, "Freelance": 500.00 },
  "totalExpenses": { "Food": 400.00, "Rent": 1200.00 },
  "netSavings": 1900.00
}
```

`month` must be between 1 and 12.

## Error handling

Every error response has the shape `{ "message": "..." }`. A single
`@RestControllerAdvice` (`GlobalExceptionHandler`) is responsible for converting
every exception the app can throw into one of the following:

| Status | Meaning | Example |
|:--:|---------|---------|
| 400 | Validation failure or malformed input | negative amount, invalid date format |
| 401 | Missing or invalid session | no cookie, expired session, wrong password |
| 403 | Valid session, resource belongs to someone else | editing another user's transaction |
| 404 | Resource doesn't exist | goal ID that was never created |
| 409 | Conflict | duplicate email, duplicate custom category name |

Any exception the app doesn't explicitly recognize is still caught and returned
as a generic `400` — the assignment specifies that no 5xx response should ever
occur for a known scenario, and this catch-all guarantees that holds even for
bugs that weren't anticipated.

## Testing

Unit tests cover the service layer (business rules, validation, calculations),
the global exception handler, and the security/session-identity helpers.
Repository dependencies are mocked with Mockito, so the tests run in
milliseconds with no real database involved.

```bash
./mvnw test
```

**42 tests, 81% line coverage** (measured with JaCoCo — full breakdown at
`target/site/jacoco/index.html` after running the tests).

The project also ships with `financial_manager_tests.sh`, a black-box
end-to-end script that exercises the live HTTP API directly with `curl` — full
user lifecycles, cross-user isolation, goal progress after transaction edits,
and report accuracy. **86/86 scenarios pass.**

## Design decisions

- **Hard deletes for transactions.** No soft-delete flag — deleting a
  transaction removes the row outright. Every report and goal calculation reads
  directly from the transactions table, so there's no secondary "is this
  deleted?" check to remember and keep consistent anywhere else in the codebase.
- **Category ownership via a nullable `owner` field.** One field on `Category`
  drives every rule about who can see, create, or delete it — no separate
  "is default" flag needed, since a default category is simply one with no owner.
- **Goal progress is computed on every read, never cached.** Storing a running
  total would go stale the instant a transaction changes. Recalculating on read
  keeps the numbers always correct, at the cost of a small amount of extra
  computation per `GET` — a reasonable tradeoff at this scale.
- **Identical error message for "wrong password" and "no such account."**
  Both login failures return the same generic 401 message, so the API never
  reveals whether a given email address is registered at all.
- **Date fields are parsed manually, not bound directly as `LocalDate`.**
  Request DTOs accept dates as plain strings and parse them explicitly in the
  service layer. This means a badly formatted date produces the same clean,
  predictable 400 error as any other validation failure, rather than a raw
  Jackson deserialization error escaping before validation even runs.
