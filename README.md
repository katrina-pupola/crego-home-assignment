# Payment Methods – Client Portal and Back Office

A standalone system with two Vue websites and one shared Spring Boot backend:

- **Client app**: a client registers, confirms their email, adds a bank card or bank account, and later requests a change to it.
- **Back Office (BO)**: staff review the requested changes and approve or reject them. The client sees the decision in the client app and gets an email.

All data is fictional. There is **no payment processing and no bank integration**. Cards and IBANs are only validated (Luhn and mod-97 checksums) and stored in a safe, minimal form.

<!-- Screenshots: add images to a screenshots/ folder and uncomment
![Client dashboard](screenshots/client-dashboard.png)
![Back Office request detail](screenshots/bo-request-detail.png)
-->

---

## Setup

**You need:** Git and Docker Desktop (running). Nothing else: no Java, Node or database setup.

```bash
git clone https://github.com/katrina-pupola/crego-home-assignment.git
cd crego-home-assignment
docker compose up --build
```

The first build takes a few minutes. Docker downloads the base images, then compiles the backend and builds both apps. The system is ready when the log shows `Started BackendApplication`. On first start, Flyway creates the schema and the seed data.

| What | URL |
|---|---|
| Client app | http://localhost:5173 |
| Back Office | http://localhost:5174 |
| Test inbox (Mailpit) | http://localhost:8025 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

If a port is already taken on your machine, override it, e.g. `CLIENT_PORT=3000 BO_PORT=3001 docker compose up --build` (see [Configuration](#configuration)). If `localhost` doesn't load, try `http://127.0.0.1:<port>`.

### Seeded accounts (fictional)

| App | Email | Password |
|---|---|---|
| Client (verified, no payment method yet) | `demo@crego.test` | `Demo-Client-2026` |
| Back Office | `admin@crego.test` | `Admin-Review-2026` |
| Back Office (second reviewer) | `reviewer@crego.test` | `Second-Review-2026` |

### Test data

| Type | Values |
|---|---|
| Cards | `4111 1111 1111 1111` (Visa), `5555 5555 5555 4444` (Mastercard), `3782 822463 10005` (Amex). Any holder, any future expiry such as `12/30` |
| IBANs | `LV80BANK0000435195001`, `DE89370400440532013000`, `FR1420041010050500013M02606` |
| Invalid, to see validation | card `4111 1111 1111 1112`, IBAN `LV81BANK0000435195001`, expiry `01/20` |

### Trying it out

1. **Register** in the client app with any email address (it doesn't need to exist), e.g. password `Secret-Pass-123`.
2. Open **Mailpit** (http://localhost:8025) and click **Confirm email address** in the email. You are now logged in.
3. **Add a card.** The first payment method is active immediately.
4. **Request a change** to an IBAN. The dashboard shows the request as pending, with a progress timeline.
5. In the **Back Office**, log in as `admin@crego.test`, open the request, and compare the current and proposed methods side by side. **Approve** it, or **reject** it with a reason.
6. Back in the client app, the dashboard shows the decision within about 10 seconds, without a reload. Mailpit receives the decision email.

To see the concurrency handling, open the same pending request as both staff accounts (e.g. one normal and one private browser window) and decide in both. The second one gets a clear "already decided" message.

---

## Features

### Client app
- Registration with a password policy (12+ characters, upper and lower case, digit, special character), a live checklist and password confirmation.
- **Email verification** before the first login. The link is single-use and valid for 24 hours, with a resend option.
- Add a first payment method: **bank card** (number, holder, expiry) or **bank account** (IBAN). Fields are validated as you type, and a live preview shows the card or account.
- **Request a change** to a different card or bank account. Requesting the same details as the current method is rejected; a renewed card with a new expiry is allowed.
- Dashboard with the active method, the latest request with a status timeline, cancelling a pending request, and a decision banner. While a request is pending, the dashboard checks for a decision every 10 seconds.
- The full IBAN of the client's own account is shown on request (**Show**, **Copy**). Everywhere else it is masked.
- Request history, session-expiry warning 2 minutes before logout, responsive layout down to phone width.

### Back Office
- Staff login (staff accounts are created by migration, not by self-registration).
- Review queue with status tabs (Pending, Approved, Rejected, Cancelled, All), pagination, and automatic refresh.
- Request detail: client info, **current vs proposed method side by side**, a list of exactly which fields change, and the progress timeline.
- **Approve**, with an optional comment, or **reject**, with a required reason shown to the client. Both are confirmed in a dialog.

### Backend
- REST API under `/api`: public auth endpoints, `/api/client/**` for the CLIENT role, `/api/bo/**` for the BO role.
- Business rules are enforced in the service layer **and** by database constraints.
- Approval is one transaction with optimistic locking.
- Emails (verification, decisions) are sent after the transaction commits.
- Consistent JSON errors (RFC 9457 `ProblemDetail`) with per-field validation messages.
- OpenAPI documentation with Swagger UI.

---

## Tech stack

| Area | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Security with OAuth2 Resource Server for JWT, Validation, Mail), Lombok, springdoc-openapi |
| Database | PostgreSQL 17, Flyway migrations (Hibernate only validates the schema) |
| Frontend | Vue 3, TypeScript, Vite, Pinia, Vue Router, axios, plain CSS (no UI library) |
| Tests | JUnit 5, Mockito, MockMvc, Testcontainers (real Postgres), Vitest |
| Runtime | Docker Compose: Postgres, Mailpit (local email inbox), backend (JRE image), both apps served by nginx |

### Project structure

```
backend/                Spring Boot app, packaged by feature:
  auth/                   security config, JWT, login, registration, email verification, staff users
  client/                 client profile
  paymentmethod/          payment methods, request validation
  changerequest/          change requests: client endpoints and BO review
  notification/           emails (sent after commit)
  common/                 error handling, card/IBAN helpers, OpenAPI config
  resources/db/migration/ Flyway migrations V1–V6
client-app/             Vue client portal (served by nginx in Docker, proxies /api)
bo-app/                 Vue Back Office (same setup)
docker-compose.yml      db, mailpit, backend, client-app, bo-app
```

---

## Running the tests

The tests need **Java 25**, **Node 22.18+ or 24.12+**, and Docker (for the backend's Testcontainers).

```bash
cd backend && ./mvnw test        # unit, service and integration tests; starts a throwaway Postgres in Docker
cd client-app && npm ci && npx vitest run
cd bo-app && npm ci && npx vitest run
```

The backend tests cover:
- card and IBAN validation;
- the business rules (one pending request, deciding twice, identical change, expired card);
- the full flow end to end over HTTP;
- role-based access;
- email verification (single-use, expiry, resend limits);
- database constraints;
- optimistic locking.

---

## Local development (without Docker for the apps)

```bash
docker compose up -d db mailpit                    # database and test inbox only
cd backend && ./mvnw spring-boot:run               # http://localhost:8080
cd client-app && npm install && npm run dev        # http://127.0.0.1:5173
cd bo-app && npm install && npm run dev            # http://127.0.0.1:5174
```

In development, Vite forwards `/api` to the backend. In Docker, nginx does the same. So the browser always talks to a single origin and no CORS configuration is needed.

## Configuration

All settings have working defaults. Override them as environment variables, or in a `.env` file next to `docker-compose.yml`.

| Variable | Default | Purpose |
|---|---|---|
| `CLIENT_PORT`, `BO_PORT`, `BACKEND_PORT` | 5173, 5174, 8080 | Host ports of the apps and the API |
| `DB_PORT` | 5433 | Host port of Postgres |
| `MAILPIT_PORT`, `MAILPIT_SMTP_PORT` | 8025, 1025 | Mailpit inbox and SMTP ports |
| `JWT_SECRET` | development value | Token signing key, at least 32 bytes. **Set your own outside local use.** |

## API overview

Full, interactive documentation is in Swagger UI. To call protected endpoints there, log in through an auth endpoint and paste the token into **Authorize**.

| Area | Endpoints |
|---|---|
| Auth (public) | `POST /api/auth/client/register`, `/client/verify`, `/client/resend-verification`, `/client/login`, `/bo/login` |
| Client | `GET /api/client/me`, `GET`/`POST /api/client/payment-method`, `GET`/`POST /api/client/change-requests`, `POST /api/client/change-requests/{id}/cancel` |
| Back Office | `GET /api/bo/me`, `GET /api/bo/change-requests?status=&page=&size=`, `GET /api/bo/change-requests/{id}`, `POST …/{id}/approve`, `POST …/{id}/reject` |

Status codes are used consistently:
- **400** validation error, with field messages;
- **401** not logged in;
- **403** wrong role, or someone else's resource;
- **404** not found;
- **409** business rule conflict (method already exists, request already pending or decided, identical change, expired card).

---

## Assumptions

- A client has **one active payment method** at a time, either a card or a bank account. A change can switch between the two.
- The **first** method is added directly. Every later change needs BO approval, and the current method stays active until a change is approved.
- A client can have **one pending** change request at a time and can cancel it while it is pending.
- A change identical to the current method is not a real change, so it is rejected. The same card with a new expiry counts as a renewed card and is allowed.
- A proposed card that has **expired while waiting** for review cannot be approved.
- Staff accounts are created by administrators (seeded here), not self-registered. All staff have the same rights.
- "The client sees the decision" means the status, the reviewer's comment or reason, a banner in the client app, and an email. A real-time push is not required.
- Clients must **confirm their email** before the first login.
- Single-language (English) UI, desktop first, usable on phones.

## Key decisions and trade-offs

| Topic | Decision | Why |
|---|---|---|
| Architecture | One backend, two API namespaces (`/api/client`, `/api/bo`) protected by two roles; two standalone Vue apps | Matches the two audiences. The apps deliberately copy a few shared components instead of sharing a package, to stay independent. |
| Authentication | Stateless JWT (HS256) via Spring's OAuth2 Resource Server; 1-hour sessions with a warning before expiry | Standard Spring Security with no extra library, and works the same for both SPAs. The token lives in `localStorage`, a known trade-off (see [Possible further development](#possible-further-development)). |
| Schema | Flyway migrations; Hibernate only validates | Reviewable, repeatable SQL. Applied migrations are never edited; changes go into new versions. |
| Business rules in the DB | Partial unique indexes (one ACTIVE method, one PENDING request per client) and CHECK constraints for enums and the card-vs-IBAN fields | The rules hold even under concurrent requests or a code bug |
| Approval | One `@Transactional` method: the old method becomes REPLACED (flushed first because of the unique index), the new one ACTIVE, and the request APPROVED with reviewer and timestamp | No half-applied state |
| Concurrent decisions | A status check plus `@Version` optimistic locking on the request | Two reviewers clicking at once cannot both win; the second gets 409 |
| Errors | `@RestControllerAdvice` with `ProblemDetail` | One JSON error format, per-field messages shown next to inputs |
| Emails | Spring events, sent after commit on a background thread, with SMTP timeouts; Mailpit locally | A slow or failing mail server can never undo or delay a registration or decision |
| Seeing the decision | Dashboard polls every 10 s while a request is pending, plus an email | Simple and reliable; push updates (Server-Sent Events) are a [possible next step](#possible-further-development) |
| CORS | None: Vite (development) and nginx (Docker) proxy `/api` | The browser sees a single origin |
| Tests | Testcontainers with real Postgres instead of an in-memory database | The partial indexes and CHECK constraints are Postgres features worth testing for real |

### Sensitive data handling

- **Card numbers:** the full number is used only to validate it (Luhn) and detect the brand. Only the **last 4 digits**, brand, holder and expiry are stored. The **CVV is never requested or stored**, in line with PCI DSS, and there is no transaction to verify it against.
- **IBANs:** validated with mod-97 and stored normalised, because an account is only usable with its full number. They are shown **masked** everywhere, except to their owner on request.
- **Passwords:** BCrypt hashes. The length limit counts bytes, since BCrypt only uses 72.
- **Email verification tokens:** 256-bit random values. Only their SHA-256 hash is stored; each is single-use and expires.
- **Account discovery:** login and resend take the same time and give the same answer whether an email exists or not. Registering an existing email does return "already registered", a deliberate usability trade-off.
- **nginx** sends a strict Content-Security-Policy and other security headers.
- All seed data, test cards and IBANs are fictional or public test values.

## Possible further development

- **Real-time updates** via Server-Sent Events instead of polling.
- **HttpOnly session cookies with CSRF protection**, or refresh tokens, instead of a token in `localStorage`.
- **Card tokenisation** through a payment provider, so the backend never sees card numbers at all.
- An **audit log** of every status change, and separate BO roles (e.g. reviewer vs administrator).
- **Rate limiting** on login, registration and resend, and lockout after repeated failures.
- A **real email provider** with SPF/DKIM on an own domain for a public deployment.
- A **CI pipeline** (GitHub Actions) running all tests and the Docker build on every push.
- A shared UI package for the two apps, and a Latvian translation.
