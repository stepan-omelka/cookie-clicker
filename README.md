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

Database settings can be overridden with `DB_URL`, `DB_USER` and `DB_PASSWORD`.

## API

| Method | Path | |
|---|---|---|
| GET | `/api/title` | app title |
| GET | `/api/game` | current game state |
| POST | `/api/game/clicks` | add clicks, body `{ "count": 5 }` (1–500) |
| POST | `/api/game/upgrades/{id}` | buy an upgrade (404 unknown, 409 not enough cookies) |
| DELETE | `/api/game` | reset the game |

Errors are returned as Problem Details (RFC 9457).

## How it works

- Passive income is computed from the time since the last update, so nothing runs on the server in the background.
- The frontend shows clicks immediately and sends them in batches once per second. All requests go through one `concatMap` queue, so pending clicks always reach the server before a purchase.
- Writes lock the game row (`SELECT … FOR UPDATE`), so parallel requests can't lose updates.

## Tests

```bash
cd backend && ./mvnw test
```

```bash
cd frontend && npm test
```

Backend tests start PostgreSQL in Docker through Testcontainers.
