# Cookie Clicker

Small full-stack game: PostgreSQL, Spring Boot and Angular.

Click the cookie, buy upgrades that give more cookies per click or per second. Progress is stored in the database, the server validates every purchase.

## Stack

- **Backend:** Java 21, Spring Boot 4, Spring Data JPA, Flyway, PostgreSQL
- **Frontend:** Angular 22 (zoneless, signals), RxJS
- **Tests:** JUnit 5, MockMvc, Testcontainers, Vitest

## Running locally

Requirements: Docker, JDK 21, Node 24.

```bash
docker compose up -d
```

```bash
cd backend && ./mvnw spring-boot:run
```

```bash
cd frontend && npm install && npm start
```

Open http://localhost:4200. The Angular dev server proxies `/api` to the backend on port 8080.

Database settings can be overridden with `DB_URL`, `DB_USER` and `DB_PASSWORD`, or with a single `DATABASE_URL` (`postgresql://user:password@host/db`) as Neon and Render provide it.

## API

| Method | Path | |
|---|---|---|
| GET | `/api/title` | app title |
| GET | `/api/game` | current game state |
| POST | `/api/game/clicks` | add clicks, body `{ "id": "<uuid>", "count": 5 }` (1–500) |
| POST | `/api/game/upgrades/{id}` | buy an upgrade (404 unknown, 409 not enough cookies) |
| DELETE | `/api/game` | reset the game, returns the fresh state |

Errors are returned as Problem Details (RFC 9457).

## How it works

- Passive income is computed from the time since the last update, so nothing runs on the server in the background.
- The frontend shows clicks immediately and sends them in batches once per second. All requests go through one `concatMap` queue, so pending clicks always reach the server before a purchase.
- Every click batch has an id. A retried batch (e.g. after a lost response) is counted only once. Clicks still unsent when the tab closes go out with `sendBeacon`.
- The server accepts at most 20 clicks per second (with a burst of 600), so a script can't add unlimited cookies.
- Writes lock the game row (`SELECT … FOR UPDATE`), so parallel requests can't lose updates.
- There is one shared save and no accounts. Anyone with access to the API can reset it.

## Deployment

Runs for free on [Render](https://render.com) (app) and [Neon](https://neon.tech) (PostgreSQL). Neither needs a card, and without a payment method a service that hits a limit is suspended until the next month instead of being billed.

The root `Dockerfile` builds Angular and packages it into the Spring Boot jar, so one service serves the game and the API.

1. **Neon:** create a project in AWS Europe Central (Frankfurt). Take the **direct** connection string (*Connect* with pooling turned off). `neon init` also writes it to `.env.local` as `DATABASE_URL_UNPOOLED`; that file is ignored by git.
2. **Render:** *New → Blueprint*, pick this repository. Render reads `render.yaml` and asks for `DATABASE_URL`: paste the direct Neon connection string (`postgresql://user:password@host/db?sslmode=require`).
3. After the first build the game runs at `https://cookie-clicker-<id>.onrender.com`. Every push to `main` redeploys.

Free tier limits:

- Render sleeps after 15 minutes without traffic, and the first request then takes about a minute.
- To avoid that, set the repository variable `APP_URL` (*Settings → Secrets and variables → Actions → Variables*) to the app URL. `.github/workflows/keep-awake.yml` then requests the home page every 10 minutes. One always-on service fits into Render's 750 free hours a month.
- The home page doesn't touch the database, and idle connections close after a minute, so Neon still sleeps when nobody plays and stays within its 100 compute hours.
- GitHub pauses scheduled workflows after 60 days without commits. The app then simply goes back to sleeping between visits.

## Tests

```bash
cd backend && ./mvnw test
```

```bash
cd frontend && npm test -- --watch=false
```

Backend tests start PostgreSQL in Docker through Testcontainers.
