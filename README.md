# Warehouse Management System — Spring Boot Implementation

This is the runnable implementation of the WMS training project documented separately
(`WMS_Technical_Documentation.md`). It implements the LLD patterns and RBAC design from
that document: **Builder**, **Strategy**, **State**, **Chain of Responsibility**, and
**Facade** — plus form login, optional Google OAuth2 login, self-service registration
with an admin-approval step, and a dynamic product catalog, all backed by PostgreSQL.

## Requirements

- Java 17+
- Maven 3.9+ (or use your IDE's built-in Maven support)
- A running PostgreSQL server (see below)

## Database setup

`src/main/resources/application.yml` is pre-configured to connect to:

```
jdbc:postgresql://localhost:5432/postgres
username: postgres
password: giri
```

That's the default `postgres` database, which already exists on any fresh Postgres
install — nothing to create first. Tables are created automatically on startup
(`ddl-auto: update`).

> **Note:** you may see `createDatabaseIfNotExist=true` in MySQL tutorials — that's a
> MySQL Connector/J parameter and the PostgreSQL driver doesn't support it. If you'd
> rather use a dedicated database name (e.g. `wms`) instead of `postgres`, create it
> once yourself first:
> ```bash
> psql -U postgres -c "CREATE DATABASE wms;"
> ```
> then change the URL in `application.yml` to `jdbc:postgresql://localhost:5432/wms`.

If your local Postgres uses different credentials, just edit the `spring.datasource`
block in `application.yml` directly, or override via the `DATABASE_URL` /
`DATABASE_USERNAME` / `DATABASE_PASSWORD` environment variables — those take priority
over the values above and are what a real deployment uses (see **Deploying this for
free**, below).

## Deploying this for free

See [`DEPLOYMENT.md`](DEPLOYMENT.md) for a full walkthrough — the short version:
[Render](https://render.com) for the app (free web service, deploys from the included
`Dockerfile`) paired with [Neon](https://neon.tech) for the database (free Postgres
that doesn't expire, unlike Render's own free Postgres tier). `application.yml` is
already environment-variable driven with sensible local-dev fallbacks, so nothing here
needs to change between local development and a real deployment — just set the
environment variables listed in `DEPLOYMENT.md`.

## Running it

```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080** and redirects to `/login`.

### Seeded accounts

On every startup, `DataSeeder` creates (if they don't already exist):

| Username | Password | Role | Purpose |
|---|---|---|---|
| `admin` | `admin123` | `ADMIN` | Full access — approve users, manage products/warehouses |
| `visitor` | `visitor123` | `VISITOR` | Day-to-day receive/pick/view access |
| `newuser` | `newuser123` | `PENDING` | Demonstrates the approval flow — sign in as `admin` and promote this account |

It also seeds one product: SKU `SKU-001` ("Sample Product").

## Accounts, registration, and the approval flow

There are three roles: `PENDING`, `VISITOR`, `ADMIN`.

**Signing in:** the login form's single field accepts either your username or your
email — both are checked in one query (`UserRepository.findByUsernameOrEmail`), so
typing either works with the same password.

- Anyone can create their own account from **`/register`** (username, email, password).
  Self-registered accounts always start as `PENDING`, which has **no access at all** —
  an admin has to explicitly grant access rather than every new sign-up getting in
  automatically.
- Signing in with **Google** creates (or reuses) an account with **`VISITOR`** access
  immediately — a Google-verified identity is trusted with baseline access right away,
  unlike an arbitrary self-registered username/password. An admin can still promote it
  to `ADMIN`, or demote it, from `/admin/users` exactly like any other account.
- An `ADMIN` visits **`/admin/users`** ("Manage users" in the nav), sees every account,
  and picks a role from a dropdown to change it.
- An admin can't change their own role from that screen — this stops you from locking
  yourself (or the only admin) out by mistake. Use a second admin account, or edit the
  `app_user` table directly if you ever really need to.

## Enabling OAuth2 (Google) login

The app runs fine with just form login out of the box. To turn on "Continue with Google":

1. Create OAuth2 credentials at the [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
   (application type: **Web application**).
2. Set the authorized redirect URI to: `http://localhost:8080/login/oauth2/code/google`
3. Uncomment the `spring.security.oauth2.client.registration.google` block in
   `src/main/resources/application.yml`.
4. Run with the credentials as environment variables:
   ```bash
   GOOGLE_CLIENT_ID=your-id GOOGLE_CLIENT_SECRET=your-secret mvn spring-boot:run
   ```
   In IntelliJ: Run/Debug Configurations → Environment variables (rather than editing
   the yml file directly — see the troubleshooting note below for why).

New Google sign-ins land as `VISITOR` immediately (see above) — no approval step needed,
though an admin can still change that role afterward.

### A note on how Google login is wired (worth understanding, not just copying)

Because the configured scope is `openid,profile,email`, Google login goes through
Spring Security's **OIDC** flow, not plain OAuth2 — those are two different code paths.
`CustomOidcUserService` (in `security/`) extends `OidcUserService` and is wired into
`SecurityConfig` via `.oidcUserService(...)`. If you ever add a non-OIDC provider (one
that doesn't do OpenID Connect), it needs the *other* hook, `.userService(...)`, backed
by a class extending `DefaultOAuth2UserService` instead — mixing these up is a real,
easy-to-hit mistake: Spring Security silently falls back to its own default handling
instead of erroring, so nothing crashes — you just get a generic `OIDC_USER` authority
with no role and no row ever created in `app_user`, and every `hasAnyRole(...)` check
then 403s. If that ever happens again after adding a new provider, this is the first
thing to check.

### Troubleshooting: "Error 401: invalid_client / The OAuth client was not found"

This means Google received a `client_id` that doesn't match any real registered OAuth
client — almost always because `${GOOGLE_CLIENT_ID}` in `application.yml` was never
actually replaced with a real value. `${...}` is Spring's syntax for "read this from an
environment variable" — it is **not** a fill-in-the-blank you paste your ID into. If you
see a URL like `...&client_id=%24%7B...` when the error page loads, decode it: `%24%7B`
is `${` — that confirms this is exactly what happened.

Fix it one of two ways:
- Actually set the environment variables before starting the app (see step 4 above —
  in IntelliJ this goes in Run/Debug Configurations → Environment variables), **or**
- Skip environment variables and replace the whole `${GOOGLE_CLIENT_ID}` token —
  braces and all — with your raw client ID value directly in `application.yml`.

Also double check: the redirect URI registered in Google Cloud Console matches
`http://localhost:8080/login/oauth2/code/google` exactly, the OAuth client type is "Web
application," and you restarted the app after changing the config (env vars are only
read at startup).

## Password reset

`/forgot-password` takes an email and, if it belongs to a `LOCAL` account (not a
Google-only one), sends a one-time reset link valid for 30 minutes via SMTP. The link
points to `/reset-password?token=...`, where the user sets a new password.

### Setting up SMTP

Fill in the `spring.mail` block in `application.yml` with your own credentials:

```yaml
mail:
  host: smtp.gmail.com
  port: 587
  username: your-email@gmail.com
  password: your-16-char-app-password
```

For Gmail, `password` must be an **App Password**, not your normal Gmail password
(Google blocks regular passwords for SMTP). Generate one at: Google Account → Security
→ 2-Step Verification → App passwords. Using a different provider? Just change the
host/port to match.

**Don't commit this file with real credentials filled in** if this repo is ever shared
or made public — treat it the same as any other secret.

### Pointing the reset link at the right place

The link inside the reset email is built from `app.base-url` (near the bottom of
`application.yml`), not guessed from the incoming request. Update it to wherever the
app is actually reachable from:

```yaml
app:
  base-url: http://localhost:8080
```

- Plain local development → leave it as `http://localhost:8080`
- Testing through a tunnel (Cloudflare, ngrok, etc.) → your tunnel's `https://` URL
- A real deployment → your real domain

**This is the same category of thing as the OAuth2 redirect URI** — both describe
"where this app is publicly reachable." If you've registered a Cloudflare tunnel URL as
your Google OAuth2 redirect URI and login works, but "forgot password" still doesn't,
this is almost always why: `app.base-url` wasn't updated to match, so the emailed link
still points at `localhost`, which is unreachable from anywhere except the machine
actually running the server. Update both together whenever your tunnel URL changes.

If sending fails for any reason (wrong credentials, network issue), `MailService` logs
the error and the request still completes normally — a broken SMTP connection never
turns into a failed password-reset request for the user.

**A note on the security behavior:** the forgot-password endpoint always responds the
same way regardless of whether the email exists, belongs to a Google-only account, etc.
— that's deliberate, standard practice to stop "forgot password" from being usable to
discover which emails are registered.

## Using the app in a browser

The UI is a deliberately maximalist, 8-bit/retro-game reskin — PICO-8's 16-color
palette, a real pixel font (Press Start 2P) for headings, a retro terminal font
(VT323) for body text, hard black borders, and flat drop-shadows everywhere (no
blur, nothing rounded). The floor-plan grid is the centerpiece: it's built to read
like a game board, with occupied/empty tiles as the two obvious colors and a
segmented "HP bar" for the occupancy report. All of this is CSS/HTML only — see
`static/css/style.css` — none of it touches the backend or the API contracts.

| Page | Who | What it does |
|---|---|---|
| `/login` | anyone | Form login + optional "Continue with Google" |
| `/register` | anyone | Create a local account (starts as `PENDING`) |
| `/forgot-password` | anyone | Request a password reset email |
| `/reset-password?token=...` | anyone with a valid link | Set a new password |
| `/dashboard` | any signed-in user | Landing page; shows a waiting notice if you're still `PENDING` |
| `/warehouses` | `ADMIN`, `VISITOR` | List + create warehouses (create form is ADMIN-only) |
| `/warehouses/{id}` | `ADMIN`, `VISITOR` | The floor plan grid — click an empty slot to receive into it, click an occupied slot to pick from it (partial quantities supported) |
| `/products` | `ADMIN`, `VISITOR` | Product catalog list + add form (ADMIN-only) |
| `/admin/users` | `ADMIN` | Approve pending accounts / change roles |

The Receive form's product field is a dropdown sourced live from `/api/products` — add
products on the Products page first (or via the API below) before receiving stock.

## Trying the API directly

All examples use HTTP Basic-style curl with `-u`, which Spring accepts the same way it
accepts the form login session.

**1. Register a new account (public)**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "priya", "email": "priya@example.com", "password": "secret123"}'
```
This account is `PENDING` until an admin promotes it — see step 2.

**2. Promote it to VISITOR (ADMIN only)**
```bash
# First find the new user's id
curl -u admin:admin123 http://localhost:8080/api/admin/users

# Then update its role
curl -u admin:admin123 -X PUT http://localhost:8080/api/admin/users/USER_ID/role \
  -H "Content-Type: application/json" \
  -d '{"role": "VISITOR"}'
```

**3. Add a product (ADMIN only)**
```bash
curl -u admin:admin123 -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"sku": "SKU-002", "name": "Pallet Wrap, 500m"}'
```

**4. Create a warehouse (ADMIN only)**
```bash
curl -u admin:admin123 -X POST http://localhost:8080/api/warehouses \
  -H "Content-Type: application/json" \
  -d '{"name": "Chennai DC-1", "aisles": 3, "tiers": 2}'
```
Copy the returned `id` — you'll need it below as `WAREHOUSE_ID`.

**5. View the grid (ADMIN or VISITOR)**
```bash
curl -u visitor:visitor123 http://localhost:8080/api/warehouses/WAREHOUSE_ID/grid
```

**6. Receive an item (ADMIN or VISITOR)**
```bash
# Auto-assign the first available slot (omit slotId, or send it as null)
curl -u visitor:visitor123 -X POST http://localhost:8080/api/warehouses/WAREHOUSE_ID/receive \
  -H "Content-Type: application/json" \
  -d '{"productSku": "SKU-001", "quantity": 10}'

# Or choose the exact destination slot yourself — grab a slot id from step 5's grid output
curl -u visitor:visitor123 -X POST http://localhost:8080/api/warehouses/WAREHOUSE_ID/receive \
  -H "Content-Type: application/json" \
  -d '{"productSku": "SKU-001", "quantity": 10, "slotId": "SOME_EMPTY_SLOT_ID"}'
```
Copy the returned `slotId` — you'll need it below.

**7. View the occupancy report**
```bash
curl -u visitor:visitor123 http://localhost:8080/api/warehouses/WAREHOUSE_ID/report/occupancy
```

**8. Pick some of the item back out (partial picks supported)**
```bash
# Picks 4 of the 10 units — the slot stays occupied with 6 remaining
curl -u visitor:visitor123 -X POST http://localhost:8080/api/warehouses/WAREHOUSE_ID/pick \
  -H "Content-Type: application/json" \
  -d '{"slotId": "SLOT_ID", "quantity": 4}'

# Pick the rest (quantity must match what's actually left) to empty the slot
curl -u visitor:visitor123 -X POST http://localhost:8080/api/warehouses/WAREHOUSE_ID/pick \
  -H "Content-Type: application/json" \
  -d '{"slotId": "SLOT_ID", "quantity": 6}'
```

**9. Confirm a VISITOR can't create a warehouse (should return 403)**
```bash
curl -u visitor:visitor123 -X POST http://localhost:8080/api/warehouses \
  -H "Content-Type: application/json" \
  -d '{"name": "Should Fail", "aisles": 1, "tiers": 1}'
```

## Where each pattern lives

| Pattern | File(s) |
|---|---|
| Builder | `domain/Warehouse.java` (nested `Builder` class) |
| Strategy | `service/allocation/SlotAllocationStrategy.java`, `FirstAvailableStrategy.java` |
| State | `domain/SlotState.java`, `EmptyState.java`, `OccupiedState.java`, driven from `Slot.java` |
| Chain of Responsibility | `service/validation/ReceiveValidationHandler.java` + 3 concrete handlers, wired in `config/BeanConfig.java` |
| Facade | `service/InventoryService.java` |
| RBAC / Security | `security/SecurityConfig.java`, `CustomUserDetailsService.java`, `CustomOAuth2UserService.java`, `service/UserAdminService.java` |

## Running tests

```bash
mvn test
```

Eight tests are included:
- `FirstAvailableStrategyTest`, `SlotStateTest` — from the documentation's original Testing
  Strategy suggestions (`SlotStateTest` now also covers partial/full/over-quantity picks)
- `AuthServiceTest` — new accounts always start `PENDING`, duplicate username/email rejected
- `UserAdminServiceTest` — an admin can promote another account but not their own
- `ProductServiceTest` — duplicate SKUs rejected
- `PasswordResetServiceTest` — reset only proceeds for `LOCAL` accounts with a known email,
  tokens are single-use and expire
- `MailServiceTest` — sending calls through to `JavaMailSender`, and a send failure never
  throws back to the caller
- `CustomUserDetailsServiceTest` — login resolves by username or by email, and the
  principal's name is always the real username either way

Add `ReceiveValidationChainTest` and an `InventoryServiceConcurrencyTest` next, as suggested
in the documentation.

## Notes / known simplifications (intentional, for a training project)

- `ddl-auto: update` auto-creates/updates tables from the entities — fine for learning,
  not recommended for production (use a migration tool like Flyway there instead).
- Product SKU/username/email uniqueness is checked at the service layer before insert,
  not guarded by a database-level retry — fine for a single-admin training project;
  under real concurrent writes you'd also want a unique constraint violation handler
  (there already *is* a DB unique constraint on each of those columns, so a genuine race
  would still fail loudly rather than silently duplicate — it just wouldn't come back
  as a friendly 400 message yet).
- No "forgot password" flow, no email verification on registration — registering just
  creates a `PENDING` account; the admin-approval step is the only gate.
