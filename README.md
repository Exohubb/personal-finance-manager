# Personal Finance Manager

A REST API for tracking personal income, expenses, and savings goals. Built as a
backend assignment for the Syfe SDE role.

Users can register and log in with a session cookie, record transactions against
income/expense categories, set savings goals, and pull monthly/yearly reports on
their spending.

## Tech stack

| Component        | Choice                                    |
|-------------------|--------------------------------------------|
| Language          | Kotlin                                     |
| Framework         | Spring Boot 4.1.1                          |
| Security          | spring-boot-starter-security (session auth, BCrypt) |
| Persistence       | Spring Data JPA + Hibernate                |
| Database          | H2 (in-memory)                             |
| Build tool        | Maven                                      |
| Testing           | JUnit 5, Mockito, mockito-kotlin, JaCoCo   |

### A note on the Spring Boot version

The assignment brief asks for Spring Boot 3.x. By the time this was built, the
entire 3.x line (including 3.5, the last release) had reached end-of-life with no
further security patches. Spring Initializr no longer offers a 3.x option at all.
Rather than deliberately build on an unsupported framework version, this project
uses Spring Boot 4.1.1, the current stable release. The tradeoff: Boot 4 ships with
Jackson 3, whose classes live under a new package root (`tools.jackson.*` instead
of `com.fasterxml.jackson.*`) - this only matters if you're extending the code and
need to import Jackson classes directly.

## Getting started

### Prerequisites

- JDK 17 or newer
- No local Maven install needed - the project ships with the Maven wrapper (`mvnw`)

### Running locally

```bash
cd personalfinancemanager
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080/api`. It uses an in-memory H2 database,
so all data resets every time the app restarts - there's nothing to set up
beforehand, the seven default categories are created automatically on startup.

To inspect the database directly while the app is running, open
`http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:financedb`, username
`sa`, empty password).

### Running the tests

```bash
./mvnw test
```

This runs the full unit test suite and generates a coverage report at
`target/site/jacoco/index.html`.

### Running the end-to-end test script

With the app running locally in one terminal:

```bash
bash financial_manager_tests.sh http://localhost:8080/api
```

## Project structure

The code follows a standard layered architecture:

```
controller/   HTTP endpoints - request parsing, status codes, no business logic
service/      business rules, validation, calculations
repository/   Spring Data JPA interfaces - database access only
entity/       @Entity classes mapped to database tables
dto/          request/response shapes exposed by the API (never expose entities directly)
security/     session-based authentication setup
exception/    custom exceptions + a single @ControllerAdvice that maps them to HTTP status codes
config/       startup configuration (default category seeding)
util/         small shared helpers (money formatting)
```

## Authentication

Authentication is session-based, using Spring Security with a cookie
(`JSESSIONID`) rather than a token/JWT scheme, per the assignment's requirement.

- `POST /api/auth/register` and `POST /api/auth/login` are the only endpoints
  open to unauthenticated requests.
- On login, the server creates an HTTP session and returns a `Set-Cookie` header.
  Send that cookie back on every subsequent request.
- `POST /api/auth/logout` invalidates the session server-side.
- Every other endpoint returns `401 Unauthorized` without a valid session, and
  `403 Forbidden` when a valid session tries to touch another user's data.

Passwords are hashed with BCrypt before being stored - the plain text password is
never persisted.

## API reference

All endpoints are prefixed with `/api`. Request/response bodies are JSON.

### Auth

| Method | Path | Auth required | Description |
|--------|------|----------------|--------------|
| POST | `/auth/register` | No | Create an account |
| POST | `/auth/login` | No | Start a session |
| POST | `/auth/logout` | Yes | End the current session |

`POST /auth/register`
```json
{ "username": "user@example.com", "password": "password123", "fullName": "John Doe", "phoneNumber": "+1234567890" }
```

`POST /auth/login`
```json
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
(both `YYYY-MM-DD`), `category` (by name), `type` (`INCOME` or `EXPENSE`).

`POST /transactions`
```json
{ "amount": 50000.00, "date": "2024-01-15", "category": "Salary", "description": "January Salary" }
```

Rules:
- `amount` must be strictly positive.
- `date` must be `YYYY-MM-DD` and cannot be in the future (today is allowed).
- `category` must be a default category or a custom category owned by the caller.
- Updating a transaction can change `amount`, `category`, and `description`. Any
  `date` field sent in an update request is silently ignored - the date can never
  be changed after creation.
- Deleting a transaction removes it permanently. It immediately stops counting
  toward savings goal progress and reports.

### Categories

| Method | Path | Description |
|--------|------|--------------|
| GET | `/categories` | List default + the caller's own custom categories |
| POST | `/categories` | Create a custom category |
| DELETE | `/categories/{name}` | Delete a custom category by name |

The seven default categories (`Salary` - INCOME; `Food`, `Rent`, `Transportation`,
`Entertainment`, `Healthcare`, `Utilities` - EXPENSE) are seeded automatically on
startup and are shared by every user. They can never be edited or deleted.

Custom category names only need to be unique per user - two different users can
each have their own category called `Freelance`. A custom category currently used
by any transaction cannot be deleted.

`POST /categories`
```json
{ "name": "Freelance", "type": "INCOME" }
```

### Savings goals

| Method | Path | Description |
|--------|------|--------------|
| POST | `/goals` | Create a goal |
| GET | `/goals` | List all goals with live progress |
| GET | `/goals/{id}` | Get one goal with live progress |
| PUT | `/goals/{id}` | Update target amount and/or target date |
| DELETE | `/goals/{id}` | Delete a goal |

`POST /goals`
```json
{ "goalName": "Emergency Fund", "targetAmount": 5000.00, "targetDate": "2027-01-01", "startDate": "2025-01-01" }
```

- `startDate` is optional and defaults to today if omitted.
- `targetDate` must be strictly in the future.
- Progress is never stored - it's calculated fresh on every read as
  `(total income - total expenses)` across all of the user's transactions dated
  on or after the goal's `startDate`. Deleting or adding a transaction changes
  progress immediately on the next read, with no extra step needed.

### Reports

| Method | Path | Description |
|--------|------|--------------|
| GET | `/reports/monthly/{year}/{month}` | Income/expense breakdown for one month |
| GET | `/reports/yearly/{year}` | Income/expense breakdown for one year |

Both group amounts by category name and include a `netSavings` total
(income minus expenses). `month` must be between 1 and 12.

## Error handling

Every error response has the shape `{ "message": "..." }`. Status codes follow
the assignment's table:

| Status | Meaning |
|--------|---------|
| 400 | Validation failure or malformed input |
| 401 | Missing or invalid session |
| 403 | Valid session, but the resource belongs to someone else |
| 404 | Resource doesn't exist |
| 409 | Conflict (duplicate email, duplicate category name) |

A single `@RestControllerAdvice` (`GlobalExceptionHandler`) is responsible for
converting every exception the app can throw into one of the above. Unexpected
exceptions are caught by a final catch-all and returned as a generic `400`,
since the assignment specifies no 5xx response should ever occur for a known scenario.

## Design decisions worth calling out

- **Hard deletes for transactions.** Rather than a soft-delete flag, deleting a
  transaction removes the row outright. Every report and goal calculation reads
  directly from the transactions table, so a deleted transaction simply can't be
  counted anywhere - there's no separate filtering logic to keep in sync.
- **Category ownership via a nullable `owner` field.** A `Category` with
  `owner = null` is a shared default category; one with an `owner` set is a
  custom category scoped to that user. This single field drives every rule about
  who can see, create, or delete a category.
- **Goal progress is computed, not stored.** Storing a running total would go
  stale the moment a transaction changes. Recalculating on every read keeps the
  numbers always correct at the cost of a bit of extra computation on `GET`
  requests, which is a reasonable tradeoff at this scale.
- **Same error message for "wrong password" and "no such user".** Both login
  failure cases return an identical 401 message, so the API never reveals
  whether a given email address has an account at all.

## Testing

Unit tests cover the service layer (business rules, validation, calculations),
the global exception handler, and the security/session-identity helpers, using
Mockito to fake out repository dependencies so no real database is needed to run
them.

```bash
./mvnw test
```

Current coverage (via JaCoCo, `target/site/jacoco/index.html` after running
tests): **81%** line coverage.
