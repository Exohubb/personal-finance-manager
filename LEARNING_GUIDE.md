# Personal Finance Manager — Learning Guide

This document explains **every API endpoint** (what to send, what it checks,
what you get back) and **every Spring Boot concept** used to build this
project (what it is, why we used it, and where it lives in the code).

It's written for someone learning Kotlin + Spring Boot, so nothing is assumed —
each idea is explained in plain language before we look at the code for it.

Live API: `https://personal-finance-manager-wmvi.onrender.com/api`
Local API: `http://localhost:8080/api`

> The service is on Render's free tier, so it spins down after 15 minutes of
> no traffic. The first request after it's been idle can take 30-50 seconds
> while it wakes back up — that's normal, not a bug.

---

## Table of contents

**Part 1 — API Reference**
- [How authentication works](#how-authentication-works)
- [Auth endpoints](#auth-endpoints)
- [Transaction endpoints](#transaction-endpoints)
- [Category endpoints](#category-endpoints)
- [Savings goal endpoints](#savings-goal-endpoints)
- [Report endpoints](#report-endpoints)
- [Every error you might see](#every-error-you-might-see)

**Part 2 — Spring Boot Concepts Used In This Project**
- [The big picture: layered architecture](#the-big-picture-layered-architecture)
- [Spring Boot annotations, one by one](#spring-boot-annotations-one-by-one)
- [Spring Data JPA — talking to the database without SQL](#spring-data-jpa--talking-to-the-database-without-sql)
- [Spring Security — session-based authentication](#spring-security--session-based-authentication)
- [Validation with Jakarta Bean Validation](#validation-with-jakarta-bean-validation)
- [Exception handling with @ControllerAdvice](#exception-handling-with-controlleradvice)
- [Kotlin-specific things you'll see everywhere](#kotlin-specific-things-youll-see-everywhere)

---

# Part 1 — API Reference

## How authentication works

This API does **not** use tokens (like JWT) — it uses **cookies**, the same
way a classic website keeps you logged in.

Here's the flow, step by step:

1. You call `POST /auth/login` with your username and password.
2. If they're correct, the server creates a **session** (a little bit of
   memory on the server that says "this session ID belongs to user #5") and
   sends back a cookie called `JSESSIONID` in the response headers.
3. Your HTTP client (browser, curl, Postman) stores that cookie automatically
   and sends it back on every future request to this API.
4. The server reads the cookie, looks up which user it belongs to, and now
   knows who's making the request — without you sending a username/password
   again.
5. `POST /auth/logout` destroys the session server-side. The cookie becomes
   useless after that.

**If you're using curl**, you need to tell it to save and reuse cookies:

```bash
# -c saves cookies received from the server into a file
curl -c cookies.txt -X POST https://personal-finance-manager-wmvi.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"you@example.com","password":"yourPassword123"}'

# -b sends cookies from that file back on the next request
curl -b cookies.txt https://personal-finance-manager-wmvi.onrender.com/api/categories
```

**If you're using Postman or Insomnia**, cookies are handled automatically —
just log in once in the same workspace/session and every following request in
that same client will carry the cookie for you.

Every endpoint below is marked **Auth: required** or **Auth: none**.

---

## Auth endpoints

### `POST /auth/register` — create an account

**Auth:** none

Send:
```json
{
  "username": "you@example.com",
  "password": "yourPassword123",
  "fullName": "Your Name",
  "phoneNumber": "+10000000000"
}
```

What it checks:
- `username` must be present and look like a real email address (has an `@`
  and a domain).
- `password`, `fullName`, `phoneNumber` must all be present (not blank).
- `username` must not already belong to an existing account.

You get back:
```json
{ "message": "User registered successfully", "userId": 1 }
```
Status: `201 Created`

If the email is already registered, you get `409 Conflict` instead. If any
required field is missing, you get `400 Bad Request` with a message telling
you exactly which field.

---

### `POST /auth/login` — start a session

**Auth:** none

Send:
```json
{ "username": "you@example.com", "password": "yourPassword123" }
```

What it checks:
- Looks up the account by username.
- Compares the password you sent against the encrypted password stored in
  the database (see [BCrypt](#bcrypt-password-hashing) below for how that
  comparison works without ever storing your real password).

You get back:
```json
{ "message": "Login successful" }
```
Status: `200 OK`, plus a `Set-Cookie: JSESSIONID=...` header you need to keep.

If the username doesn't exist, or the password is wrong, you get
`401 Unauthorized` with the exact same message either way — `"Invalid
username or password"`. This is intentional: it stops someone from being able
to tell whether a given email even has an account by watching for a different
error message.

---

### `POST /auth/logout` — end your session

**Auth:** required

Send: nothing, no body needed. Just make sure your cookie is attached.

What it does: destroys your session on the server. The cookie you were using
becomes invalid immediately — any request with it afterward is treated as
logged out.

You get back:
```json
{ "message": "Logout successful" }
```
Status: `200 OK`

---

## Transaction endpoints

A transaction is one entry of money moving — either income or an expense.

### `POST /transactions` — create a transaction

**Auth:** required

Send:
```json
{
  "amount": 5000.00,
  "date": "2024-01-15",
  "category": "Salary",
  "description": "January Salary"
}
```

What it checks:
- `amount` must be a number greater than 0 (zero and negative numbers are rejected).
- `date` must be in `YYYY-MM-DD` format.
- `date` cannot be later than today — you can't log a transaction that hasn't
  happened yet. Today itself is fine.
- `category` must be a real category name — either one of the 7 default ones,
  or a custom one *that you personally created*. You can't use someone else's
  custom category, and typos won't silently pass through.
- `description` is optional — leave it out or send `null` if you don't want one.

You get back:
```json
{
  "id": 1,
  "amount": 5000.00,
  "date": "2024-01-15",
  "category": "Salary",
  "description": "January Salary",
  "type": "INCOME"
}
```
Status: `201 Created`

Notice `type` isn't something you send — it's decided automatically from
whatever category you picked. `Salary` is an income category, so the
transaction becomes `INCOME` on its own.

---

### `GET /transactions` — list your transactions

**Auth:** required

No body needed. Optional query parameters, all of which can be combined:

| Parameter | Example | What it does |
|---|---|---|
| `startDate` | `?startDate=2024-01-01` | Only transactions on/after this date |
| `endDate` | `?endDate=2024-01-31` | Only transactions on/before this date |
| `category` | `?category=Food` | Only transactions in this category (not case-sensitive) |
| `type` | `?type=INCOME` | Only `INCOME` or only `EXPENSE` |

Example:
```
GET /transactions?startDate=2024-01-01&endDate=2024-01-31&category=Food
```

You get back a list, **always sorted newest date first**:
```json
{
  "transactions": [
    { "id": 3, "amount": 400.00, "date": "2024-01-17", "category": "Food", "description": "Groceries", "type": "EXPENSE" }
  ]
}
```
Status: `200 OK`. If nothing matches, you get `{ "transactions": [] }` — never an error.

---

### `PUT /transactions/{id}` — update a transaction

**Auth:** required, and the transaction must belong to you

Send only the fields you want to change:
```json
{ "amount": 5500.00, "description": "Updated January Salary" }
```

What it checks:
- You can change `amount`, `category`, and `description`.
- **You cannot change the date, ever.** If you include a `"date"` field in
  your request, it's simply ignored — not rejected, just silently dropped.
  The transaction keeps its original date no matter what you send.
- If you're changing `category`, the same rule as creation applies — it must
  be a category you can actually use.
- The transaction ID must actually exist **and belong to you**.

You get back the full updated transaction, same shape as create.

If the ID doesn't exist, or belongs to a different user, you get
`404 Not Found`. (We intentionally return 404 rather than 403 here, so that
you can't use this endpoint to probe whether a given transaction ID exists on
someone else's account at all.)

---

### `DELETE /transactions/{id}` — delete a transaction

**Auth:** required, and the transaction must belong to you

Send: nothing.

What it does: permanently removes the transaction. This is a real deletion,
not a "hide it" flag — once deleted, it stops counting toward every savings
goal's progress and every report immediately, because those are calculated
live from whatever transactions currently exist.

You get back:
```json
{ "message": "Transaction deleted successfully" }
```
Status: `200 OK`, or `404 Not Found` if the ID doesn't exist or isn't yours.

---

## Category endpoints

Categories label your transactions as one of two types: `INCOME` or `EXPENSE`.

There are **7 default categories** that exist for everyone automatically and
can never be changed or deleted by anyone:

| Name | Type |
|---|---|
| Salary | INCOME |
| Food | EXPENSE |
| Rent | EXPENSE |
| Transportation | EXPENSE |
| Entertainment | EXPENSE |
| Healthcare | EXPENSE |
| Utilities | EXPENSE |

You can also make your own **custom categories** — they belong only to you,
and other users can't see or use them.

### `GET /categories` — list categories you can use

**Auth:** required

No body needed. Returns the 7 default categories plus any custom ones you've
personally created — never another user's custom categories.

```json
{
  "categories": [
    { "name": "Salary", "type": "INCOME", "custom": false },
    { "name": "Food", "type": "EXPENSE", "custom": false },
    { "name": "Freelance", "type": "INCOME", "custom": true }
  ]
}
```
Status: `200 OK`

---

### `POST /categories` — create a custom category

**Auth:** required

Send:
```json
{ "name": "Freelance", "type": "INCOME" }
```

What it checks:
- `name` must be present.
- `type` must be exactly `INCOME` or `EXPENSE` — anything else is rejected.
- The name must not already exist **among your own categories** (default +
  your custom ones). Two different users can both have a category called
  `Freelance` without conflict — uniqueness is per-user, not global.

You get back:
```json
{ "name": "Freelance", "type": "INCOME", "custom": true }
```
Status: `201 Created`, or `409 Conflict` if you already have a category with that name.

---

### `DELETE /categories/{name}` — delete your custom category

**Auth:** required

Send: nothing. The category name goes in the URL, for example
`DELETE /categories/Freelance`.

What it checks, in order:
1. Is this a category **you** created? If it belongs to another user, or
   doesn't exist at all under your account →
2. Does it even exist anywhere? If truly nowhere → `404 Not Found`.
3. Is it one of the 7 default categories? Those can never be deleted by
   anyone → `400 Bad Request`.
4. Does it belong to a different user? → `403 Forbidden`.
5. Is it currently used by any of your transactions? If yes → `400 Bad
   Request` — you'd have to delete or re-categorize those transactions first.
6. Otherwise, it's deleted.

You get back:
```json
{ "message": "Category deleted successfully" }
```
Status: `200 OK`

---

## Savings goal endpoints

A savings goal tracks progress toward a target amount of money, calculated
from your real transaction history — there's no separate "add money to your
goal" action, it just watches your income and expenses.

**The core formula, always:**
```
progress = (sum of all your income) − (sum of all your expenses)
           counting only transactions dated on or after the goal's startDate
```

This is recalculated **every single time you read the goal** — it's never
stored as a fixed number. Add a transaction, delete one, edit one — the very
next time you look at the goal, the progress reflects it immediately.

### `POST /goals` — create a goal

**Auth:** required

Send:
```json
{
  "goalName": "Emergency Fund",
  "targetAmount": 5000.00,
  "targetDate": "2027-01-01",
  "startDate": "2025-01-01"
}
```

What it checks:
- `goalName` must be present.
- `targetAmount` must be greater than 0.
- `targetDate` must be `YYYY-MM-DD` and must be **strictly in the future**
  (unlike transactions, today itself is not allowed here).
- `startDate` is **optional** — leave it out and it defaults to today's date.
- If you do provide `startDate`, it can't be later than `targetDate`.

You get back:
```json
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
Status: `201 Created`

- `currentProgress` — money saved so far (income minus expenses since `startDate`)
- `progressPercentage` — `currentProgress ÷ targetAmount × 100`, rounded to 2 decimals
- `remainingAmount` — `targetAmount − currentProgress`

---

### `GET /goals` — list all your goals

**Auth:** required

No body needed. Returns every goal you own, each with freshly calculated progress.

```json
{ "goals": [ { "id": 1, "goalName": "Emergency Fund", "...": "..." } ] }
```
Status: `200 OK`

---

### `GET /goals/{id}` — get one goal

**Auth:** required, goal must be yours

Same response shape as create. `404 Not Found` if the ID doesn't exist or belongs to someone else.

---

### `PUT /goals/{id}` — update a goal

**Auth:** required, goal must be yours

Send only what you want to change:
```json
{ "targetAmount": 6000.00, "targetDate": "2026-02-01" }
```

What it checks:
- You can change `targetAmount` and `targetDate` only — not `goalName` or
  `startDate`.
- A new `targetDate`, if provided, must still be in the future.
- `targetAmount`, if provided, must still be greater than 0.

You get back the goal with progress recalculated against the *new* target
(so `progressPercentage` and `remainingAmount` will reflect the update
immediately).

---

### `DELETE /goals/{id}` — delete a goal

**Auth:** required, goal must be yours

Send: nothing.

```json
{ "message": "Goal deleted successfully" }
```
Status: `200 OK`, or `404 Not Found`/`403 Forbidden` depending on ownership.

---

## Report endpoints

Reports summarize your transactions over a period, grouped by category.

### `GET /reports/monthly/{year}/{month}` — one month's summary

**Auth:** required

Example: `GET /reports/monthly/2024/1` for January 2024.

What it checks: `month` must be between 1 and 12. `13`, `0`, or negative
numbers all return `400 Bad Request`.

```json
{
  "month": 1,
  "year": 2024,
  "totalIncome": { "Salary": 3000.00, "Freelance": 500.00 },
  "totalExpenses": { "Food": 400.00, "Rent": 1200.00 },
  "netSavings": 1900.00
}
```
Status: `200 OK`. A month with no transactions at all still returns `200 OK`
with empty maps and `netSavings: 0`, never an error.

`netSavings` = total of every income category minus total of every expense category.

---

### `GET /reports/yearly/{year}` — one year's summary

**Auth:** required

Example: `GET /reports/yearly/2024`.

Same grouping logic as the monthly report, just aggregated across all 12
months of the given year.

```json
{
  "year": 2024,
  "totalIncome": { "Salary": 36000.00 },
  "totalExpenses": { "Food": 4800.00, "Rent": 14400.00 },
  "netSavings": 16800.00
}
```
Status: `200 OK`

---

## Every error you might see

Every error response looks like this, always:
```json
{ "message": "A human-readable explanation of what went wrong" }
```

| Status | What it means | When you'll see it |
|:--:|---|---|
| **400** Bad Request | Something about your request is invalid | Negative amount, badly formatted date, future-dated transaction, invalid month number, missing required field |
| **401** Unauthorized | You're not logged in, or your credentials were wrong | No cookie sent, expired session, wrong password at login |
| **403** Forbidden | You're logged in, but trying to touch something that isn't yours | Deleting another user's custom category |
| **404** Not Found | The thing you asked for doesn't exist | A transaction/goal/category ID or name that was never created |
| **409** Conflict | Your request would clash with something that already exists | Registering an email that's already taken, creating a category name you already have |

The API is designed so a `500 Internal Server Error` should never happen for
any input you can reasonably send — even something totally unexpected gets
caught and turned into a safe `400` response instead of crashing or leaking
an internal stack trace.

---

# Part 2 — Spring Boot Concepts Used In This Project

This section is a walkthrough of every meaningful Spring Boot / Kotlin idea
used to build the API above — what each thing is, why we reached for it, and
where to find it in the codebase.

## The big picture: layered architecture

The project is split into layers, and each layer only ever talks to the one
directly below it:

```
Controller  →  Service  →  Repository  →  Database
```

- **Controller** — the "front door". Reads the incoming HTTP request, calls
  a service to do the real work, and packages the result into an HTTP
  response with a status code. Controllers contain almost no logic on
  purpose — just plumbing.
- **Service** — where the actual rules live. "Is this amount positive?",
  "does this category belong to this user?", "how do I calculate goal
  progress?" — all of that lives here, with zero knowledge of HTTP at all.
- **Repository** — talks to the database. Nothing else.

Why split it this way? Because it means you can test a service's business
logic completely on its own (see the `src/test` folder), without needing a
real running web server or database — which is exactly how this project's
unit tests work.

You'll find this structure as folders under `src/main/kotlin/.../`:
`controller/`, `service/`, `repository/`, `entity/`, `dto/`.

---

## Spring Boot annotations, one by one

### `@SpringBootApplication`

Found on `PersonalfinancemanagerApplication.kt`, the entry point of the
whole app (the `fun main()` that actually starts everything). This one
annotation is really three combined into one:

- `@Configuration` — this class can define beans (see below)
- `@EnableAutoConfiguration` — Spring Boot looks at what's on your classpath
  (e.g. "oh, there's an H2 database jar and JPA here") and automatically
  wires up sensible defaults, so you don't hand-configure a database
  connection pool yourself
- `@ComponentScan` — Spring goes and finds every `@Service`, `@RestController`,
  `@Repository`, etc. anywhere in the package and registers them

### `@RestController`

Found on every class in `controller/`. Marks a class as something that
handles HTTP requests and returns data directly as the response body (as
JSON, in our case) — as opposed to plain `@Controller`, which is meant for
returning HTML page names to render.

### `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`

`@RequestMapping("/api/transactions")` on the class sets a shared URL prefix.
Then each method gets a more specific mapping for its HTTP verb, e.g.:

```kotlin
@PostMapping
fun createTransaction(...)   // handles POST /api/transactions

@GetMapping("/{id}")
fun getGoal(...)             // handles GET /api/goals/{id}
```

The verb matters: `GET` is for reading, `POST` for creating, `PUT` for
updating, `DELETE` for removing — this is just standard REST convention, and
Spring routes incoming requests to the right method based on matching both
the URL path and the HTTP verb.

### `@RequestBody`

Tells Spring "the JSON in the request's body should be converted into this
Kotlin object automatically". Example:

```kotlin
fun register(@Valid @RequestBody request: RegisterRequest)
```

Spring uses a library called Jackson behind the scenes to turn incoming JSON
text into a real `RegisterRequest` object with `username`, `password`, etc.
already filled in — you never parse JSON by hand anywhere in this codebase.

### `@PathVariable`

Pulls a value out of the URL itself. For
`@GetMapping("/monthly/{year}/{month}")`, a request to
`/reports/monthly/2024/1` gives you `year = 2024` and `month = 1` as real
`Int` parameters in your function — Spring converts the text from the URL
into the right type automatically.

### `@RequestParam`

Pulls a value from the query string (the `?key=value` part of a URL). Used
in `GET /transactions` for the optional filters:

```kotlin
fun getTransactions(
    @RequestParam(required = false) startDate: String?,
    ...
)
```

`required = false` (matched with a nullable Kotlin type, `String?`) means the
caller can simply not include that parameter at all.

### `@Valid`

Placed next to `@RequestBody`, this tells Spring "before running this
method's body, check the validation rules on this object's fields, and if
any fail, stop here and throw an error instead". See
[Validation](#validation-with-jakarta-bean-validation) below for what those
rules actually look like.

### `@Service`

Found on every class in `service/`. Marks a class as a "bean" — an object
Spring creates once, keeps around, and hands to anything else that needs it.
There's nothing magic about the word "Service" itself, it's just the
conventional name for "a class holding business logic", same idea as
`@Component` but more descriptive.

### `@Repository` (implicitly, via `JpaRepository`)

You won't actually find `@Repository` written anywhere in this project's
code — that's because every repository is just an `interface` extending
`JpaRepository`, and Spring Data JPA automatically detects and implements
those without needing the annotation spelled out. More on this below.

### `@Configuration` and `@Bean`

Found in `SecurityConfig.kt`. `@Configuration` marks a class as a place where
you manually build objects Spring should manage, rather than relying on
annotations like `@Service` to auto-detect them. Each `@Bean`-annotated
function inside it runs once at startup, and its return value becomes
available for other parts of the app to use. This is how we set up things
like the password encoder and the security filter chain — objects that come
from a library (Spring Security) rather than classes we wrote ourselves, so
we can't just slap `@Service` on them.

### `@Transactional`

Found on `updateTransaction()` and `updateGoal()` in the service layer.
Wraps the whole method in a single database transaction. The practical
effect: once you load an entity from the database inside a `@Transactional`
method and change one of its fields, Hibernate automatically saves that
change back to the database when the method finishes — no explicit
`.save()` call needed. This is called **dirty checking**. Without
`@Transactional`, that automatic save-on-exit behavior isn't guaranteed.

### `@Entity`, `@Id`, `@GeneratedValue`, `@Column`, `@ManyToOne`, `@JoinColumn`, `@Enumerated`

All from JPA (Java Persistence API), found in the `entity/` folder. These
describe how a Kotlin class maps onto a database table:

- `@Entity` — "this class is a database table"
- `@Id` — "this field is the primary key"
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` — "let the database
  auto-assign the next number for this field, like an auto-increment counter"
- `@Column(nullable = false, unique = true)` — customizes the matching
  database column, e.g. enforcing that two users can never share a username
  at the database level, not just in application code
- `@ManyToOne` — describes a relationship: many `Transaction` rows can point
  to the same one `User` (one user has many transactions). `fetch =
  FetchType.LAZY` means Hibernate won't bother loading the related `User`
  object until your code actually accesses it, keeping queries fast
- `@JoinColumn(name = "user_id")` — names the actual foreign key column
- `@Enumerated(EnumType.STRING)` — tells Hibernate to store our `INCOME`/
  `EXPENSE` enum as readable text in the database, instead of an
  unreadable plain number

---

## Spring Data JPA — talking to the database without SQL

Every file in `repository/` is just an `interface`:

```kotlin
interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>
    fun existsByUsername(username: String): Boolean
}
```

There's no implementation body at all — and that's the whole point. By
extending `JpaRepository<User, Long>` (entity type `User`, primary key type
`Long`), you get a long list of database methods completely for free:
`save()`, `findById()`, `findAll()`, `delete()`, `count()`, and more, with
zero code written by you.

On top of that, Spring Data JPA can build entire custom queries just by
reading the **method name**. `findByUsername(username: String)` gets turned
into the SQL equivalent of `SELECT * FROM app_user WHERE username = ?`
automatically, purely from parsing the method's name at startup. This is
called a **derived query method**.

For a couple of trickier cases, this project uses `@Query` to write the query
explicitly instead of relying on name parsing:

```kotlin
@Query("SELECT c FROM Category c WHERE c.name = :name AND (c.owner IS NULL OR c.owner.id = :ownerId)")
fun findVisibleCategoryByName(
    @Param("name") name: String,
    @Param("ownerId") ownerId: Long
): Optional<Category>
```

This is written in **JPQL** (Java Persistence Query Language) — SQL-like
syntax, but written against entity classes and their fields instead of raw
table/column names. We reached for this specifically because the equivalent
derived method name (`findByNameAndOwnerIsNullOrNameAndOwnerId`) was
ambiguous to Spring's name parser and caused a startup error — writing the
query explicitly sidesteps that entirely.

---

## Spring Security — session-based authentication

Spring Security is a large, pluggable framework. Here's how the specific
pieces we used fit together:

### The filter chain

Every incoming HTTP request passes through a **chain of filters** before it
ever reaches a controller. `SecurityConfig.kt`'s `securityFilterChain()`
function is where we configure that chain:

- Which URLs require login (`.authorizeHttpRequests { ... }`)
- What happens when someone isn't logged in and tries to access something
  protected (`.exceptionHandling { ... }`, pointed at our own
  `RestAuthenticationEntryPoint`, which returns clean JSON instead of Spring
  Security's default HTML login redirect)
- Whether sessions get created automatically (`.sessionManagement { ... }`)
- We turned off two built-in features that don't fit a JSON API:
  `.formLogin { it.disable() }` (no HTML login form) and
  `.logout { it.disable() }` (we handle logout ourselves in
  `AuthController`, explicitly, instead of letting Spring Security's built-in
  logout filter intercept the request silently)

### `UserDetailsService` and `UserDetails`

Spring Security doesn't know anything about our own `User` entity out of the
box — it works against its own interfaces. `AppUserDetailsService` implements
`UserDetailsService`, with one job: given a username, fetch the matching
`User` from the database via `UserRepository` and hand back something Spring
Security understands.

That "something" is `AppUserPrincipal`, which implements `UserDetails` — it
wraps our real `User` entity so we can still get to the actual database `id`
later, while also giving Spring Security everything it needs (`getUsername()`,
`getPassword()`, account status flags).

### `AuthenticationManager` and `DaoAuthenticationProvider`

When you call `POST /auth/login`, `AuthService` builds a
`UsernamePasswordAuthenticationToken` (Spring Security's "someone is trying
to log in with these credentials" object) and hands it to the
`AuthenticationManager`. That manager delegates to `DaoAuthenticationProvider`,
which:
1. Calls `AppUserDetailsService` to fetch the user by username
2. Uses the `PasswordEncoder` to check the submitted password against the
   stored hash
3. Throws an exception if either step fails, or returns a successful
   `Authentication` object if both pass

### BCrypt password hashing

`SecurityConfig` defines a `PasswordEncoder` bean backed by
`BCryptPasswordEncoder`. When you register, your plain password is run
through `passwordEncoder.encode(...)` before it's ever saved — turning
`"myPassword123"` into something like
`"$2a$10$N9qo8uLOickgx2ZMRZoMy..."`. This is a **one-way** transformation —
there's no way to turn the hash back into the original password. Checking a
login later works by hashing the *submitted* password the same way and
comparing the two hashes, never by reversing anything. BCrypt is also
deliberately slow, which makes brute-force password guessing impractically
slow for an attacker — this is the standard, industry-recommended way to
store passwords.

### Sessions and cookies

Once login succeeds, `AuthService` manually stores the `Authentication`
result into an `HttpSession` (via
`httpRequest.getSession(true)`), under a specific key Spring Security looks
for (`HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY`).
From that point on, the browser/client holds a `JSESSIONID` cookie, and every
future request carrying that cookie gets automatically recognized as
"this is user X" by Spring Security's filters — without needing to send
credentials again.

### Reading "who's logged in right now"

`SecurityUtils.currentUserId()` is a small helper used at the top of nearly
every controller method:

```kotlin
val userId = SecurityUtils.currentUserId()
```

It reads the current request's authenticated user out of
`SecurityContextHolder` (a thread-local storage Spring Security fills in
automatically for the current request) and pulls the real database ID off of
it. This is the single mechanism that makes **data isolation** possible —
every service method takes a `userId` and filters everything by it, and that
`userId` always comes from the trusted session, never from anything the
client typed into a request body.

---

## Validation with Jakarta Bean Validation

Every request DTO (the classes in `dto/`) has annotations directly on its
fields describing what "valid" means:

```kotlin
data class CreateTransactionRequest(
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.00", inclusive = false, message = "Amount must be a positive value")
    val amount: BigDecimal?,

    @field:NotBlank(message = "Date is required")
    val date: String,
    ...
)
```

- `@NotNull` — the field must be present at all (not missing entirely)
- `@NotBlank` — the field must be present *and* not just empty/whitespace text
- `@Email` — the text must look like a real email address
- `@DecimalMin(value = "0.00", inclusive = false)` — the number must be
  strictly greater than 0. `inclusive = false` is what makes zero itself
  invalid, not just negative numbers

The `@field:` prefix in front of each annotation is a Kotlin-specific detail —
it tells the compiler exactly where to physically attach the annotation (onto
the underlying field), which is where the validation library looks for it.
Without `@field:`, Kotlin might place the annotation somewhere validation
never checks, and the rule would silently do nothing.

These rules only actually run because the controller method parameter is
marked `@Valid`. If any rule fails, Spring throws a
`MethodArgumentNotValidException` automatically — which our global exception
handler catches and turns into a clean `400` response listing every failed
field.

One deliberate design choice: date fields (`date`, `targetDate`, `startDate`)
are typed as plain `String`, not `LocalDate`, in every request DTO. If they
were typed as `LocalDate` directly, a badly formatted date would fail *before*
our validation even runs, with a much uglier, harder-to-control error message.
Keeping them as strings lets the service layer parse them manually and report
one consistent, friendly error for both "wrong format" and "invalid value"
cases.

---

## Exception handling with `@ControllerAdvice`

`GlobalExceptionHandler.kt` is annotated `@RestControllerAdvice` — this makes
it a global safety net for every controller in the app. Instead of wrapping
every single controller method in a `try/catch`, services and controllers
just `throw` a plain exception whenever a business rule is broken, and this
one class catches every exception type it recognizes and turns it into the
right HTTP response.

Four custom exception classes exist just for this purpose
(`BadRequestException`, `ResourceNotFoundException`,
`AppAccessDeniedException`, `ConflictException`) — each maps to exactly one
HTTP status code (400, 404, 403, 409 respectively). Each `@ExceptionHandler`
method in the global handler catches one exception type and builds the
matching `ResponseEntity`:

```kotlin
@ExceptionHandler(BadRequestException::class)
fun handleBadRequest(ex: BadRequestException): ResponseEntity<Map<String, String>> {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(mapOf("message" to (ex.message ?: "Bad request")))
}
```

There's also a final catch-all `@ExceptionHandler(Exception::class)` at the
bottom. This exists specifically so that even a completely unplanned bug
somewhere in the code still returns a safe `400` response instead of an ugly
`500` with a leaked stack trace — matching the requirement that no 5xx errors
should ever occur for a known scenario.

---

## Kotlin-specific things you'll see everywhere

A few Kotlin language features show up constantly across the codebase and
are worth understanding on their own:

- **`data class`** — used for every DTO and entity. Automatically generates
  `equals()`, `hashCode()`, `toString()`, and a `copy()` method based on the
  constructor's properties, so you don't have to write that boilerplate by hand.
- **Nullable types (`String?` vs `String`)** — a huge part of how Kotlin
  avoids null-pointer crashes. A field typed `String?` might be null and the
  compiler forces you to handle that possibility; a plain `String` can never
  be null, guaranteed by the compiler at every call site.
- **The Elvis operator `?:`** — "use the left side if it's not null,
  otherwise use the right side". Used constantly for defaults, e.g.
  `request.startDate?.let { parseDate(it) } ?: LocalDate.now()` means "parse
  the provided start date if there is one, otherwise just use today".
- **`?.let { ... }`** — "if this value isn't null, run this block with it".
  Used everywhere for optional update fields:
  `request.amount?.let { transaction.amount = it }` means "only touch the
  amount field if the client actually sent a new amount".
- **Extension functions** (e.g. `private fun Transaction.toResponse()`) —
  Kotlin lets you attach a new function onto an existing class without
  editing that class's source. Used throughout the service layer to convert
  a database entity into its API response shape, keeping that conversion
  logic right next to where it's used instead of cluttering the entity class
  itself.
- **`when` expressions** — Kotlin's more powerful version of a `switch`
  statement, used for things like deciding which exception to throw based on
  multiple conditions at once (see `CategoryService.deleteCustomCategory`).
