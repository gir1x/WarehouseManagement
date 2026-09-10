# Deploying WMS for Free — Render + Neon

**The combo:** [Render](https://render.com) hosts the app itself (free web service, no
credit card), [Neon](https://neon.tech) hosts the database (free Postgres that never
expires, no credit card). Both verified current as of September 2026 — free-tier terms
change over time, so double-check each provider's pricing page if this is months old.

Worth knowing up front, so nothing surprises you:
- Render's free web service **spins down after 15 minutes of no traffic** and takes
  ~30–60 seconds to wake back up on the next request. Fine for a portfolio/demo project,
  not something you'd want for a real always-on product.
- Render also grants **750 free instance-hours per month** — one app running 24/7 uses
  about 720 of those, so you're fine with a single service.
- Neon's free tier: 0.5 GB storage, scale-to-zero compute, **no expiration date** —
  unlike Render's own free Postgres, which deletes itself 30 days after creation. That's
  exactly why this guide pairs Render (app) with Neon (database) instead of using
  Render for both.

---

## 1. Push the project to GitHub

Render deploys from a Git repository.

```bash
cd wms-project
git init
git add .
git commit -m "Initial commit"
```

Create a new repository on GitHub (public or private — Render supports both, private
just needs you to grant Render access during setup), then:

```bash
git remote add origin https://github.com/YOUR_USERNAME/wms-project.git
git branch -M main
git push -u origin main
```

**Before you push:** open `application.yml` one more time and make sure no real
secrets are sitting in it as literal values (the file now reads everything from
environment variables with placeholder-only local defaults — see the diff from this
session). If you'd previously pasted a real Gmail app password or database password
directly into the file, that's now gone from the file itself and needs to be set as an
environment variable on Render instead (step 4 below) — don't push a version of this
file with real credentials hardcoded into a repo, public or private.

## 2. Create a free Neon database

1. Sign up at [neon.tech](https://neon.tech) — no credit card required.
2. Create a new project (any name, e.g. `wms`).
3. On the project dashboard, find the **connection string**. Neon gives you something
   like:
   ```
   postgresql://neondb_owner:AbC123xyz@ep-cool-name-12345.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```
4. You need three pieces out of that for step 4 below:
   - **URL** (JDBC form): `jdbc:postgresql://ep-cool-name-12345.us-east-2.aws.neon.tech/neondb?sslmode=require`
     (same host/database as above, just with `jdbc:` in front and without the
     embedded username/password)
   - **Username**: `neondb_owner` (or whatever Neon shows you)
   - **Password**: the password Neon generated

You don't need to create any tables — `ddl-auto: update` (already configured) creates
every table automatically the first time the app starts against this database.

## 3. Create the Render web service

1. Sign up at [render.com](https://render.com) — no credit card required for the free
   tier.
2. **New +** → **Web Service** → connect your GitHub account → select the `wms-project`
   repo.
3. Render will detect the `Dockerfile` at the repo root automatically and set
   **Environment: Docker** — you shouldn't need to change this (Render has no native
   Java/Maven buildpack, which is exactly why this project now ships a Dockerfile).
4. **Instance Type**: choose **Free**.
5. Don't click "Create Web Service" yet — add the environment variables first (next
   step), so the very first deploy already has what it needs to start successfully.

## 4. Set environment variables on Render

In the same web service creation screen (or **Environment** tab afterward), add:

| Key | Value |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://ep-cool-name-12345.us-east-2.aws.neon.tech/neondb?sslmode=require` (your real Neon host) |
| `DATABASE_USERNAME` | your Neon username |
| `DATABASE_PASSWORD` | your Neon password |
| `APP_BASE_URL` | `https://your-app-name.onrender.com` (see note below) |
| `GOOGLE_CLIENT_ID` | your OAuth2 client ID (optional — leave unset to disable Google login) |
| `GOOGLE_CLIENT_SECRET` | your OAuth2 client secret (optional) |
| `MAIL_HOST` | `smtp.gmail.com` (or your provider) |
| `MAIL_PORT` | `587` |
| `MAIL_USERNAME` | your email address |
| `MAIL_PASSWORD` | your app password (never your real account password) |

**About `APP_BASE_URL`:** Render tells you your service's URL (`https://your-app-name.onrender.com`)
right after the first deploy — you can go back and add/edit this environment variable
afterward once you know it; the app will pick it up on the next restart.

Click **Create Web Service**. The first build takes a few minutes (Maven downloads
every dependency from scratch) — watch the logs; you're looking for the familiar
Spring Boot startup banner and `Started WmsApplication`.

## 5. Update the Google OAuth2 redirect URI (if using Google login)

In [Google Cloud Console](https://console.cloud.google.com/apis/credentials), edit
your OAuth2 client's **Authorized redirect URIs** and add:
```
https://your-app-name.onrender.com/login/oauth2/code/google
```
(Keep the `localhost` one too if you still want Google login to work locally —
Google allows multiple redirect URIs on the same client.)

Unlike the Cloudflare tunnel setup, **this URL never changes** between deploys — you
set it once and you're done.

## 6. Verify it

Visit `https://your-app-name.onrender.com`. You should land on `/login`. Sign in with
the seeded `admin` / `admin123` account, confirm the dashboard loads (this also proves
the Neon connection and `ddl-auto: update` table creation both worked), then try:
- Creating a warehouse
- "Continue with Google," if you set that up
- "Forgot password" — check your email (or the Render logs, if `MailService` fell
  back to logging an error) to confirm SMTP is reachable from Render's network

## Troubleshooting

**Build fails on Render with a Maven error** — check the build logs for the actual
Maven error; it's almost always either a dependency Maven Central couldn't resolve
(rare) or a genuine compile error that also would've failed locally — run
`mvn clean package` locally first to confirm the project builds cleanly before
troubleshooting the Render side.

**App starts but every page 500s / logs show a database connection error** — double
check `DATABASE_URL` uses the `jdbc:postgresql://...` form (not the raw
`postgresql://` connection string Neon shows you by default) and includes
`?sslmode=require` — Neon requires SSL and Render's outbound connections need that
query parameter to negotiate it correctly.

**"Error 401: invalid_client" on Google login** — same root cause as the earlier
Cloudflare tunnel issue: `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` are environment
variables, not values to paste inside `${...}` in the yml file. Set them as actual
Render environment variables (step 4), not by editing `application.yml`.

**First request after a while is really slow** — that's the free tier's 15-minute
spin-down behavior described at the top, not a bug. Render's paid Starter tier ($7/mo)
removes it if this becomes a problem.

## Next steps once this outgrows the free tier

- Render Starter ($7/mo): removes spin-down, more RAM/CPU.
- Neon's free tier caps at 0.5 GB — for a real dataset you'd move to Neon's paid tier
  or a different managed Postgres.
- Consider adding Flyway or Liquibase for real schema migrations instead of
  `ddl-auto: update` once this is anything more than a personal/demo project — auto-DDL
  is convenient for development but risky against a production database with real data.
