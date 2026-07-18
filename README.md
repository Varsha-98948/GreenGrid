# GreenGrid

A Git-based developer learning platform: organize the coding problems you solve, and push each one as a real commit to your own GitHub repository.

```
greengrid/
├── backend/    Java 21 · Spring Boot 3 · PostgreSQL (Supabase) · JWT + GitHub OAuth
└── frontend/   HTML · CSS · Vanilla JS · Bootstrap 5 · Monaco Editor
```

## Quick start

**1. Database** — create a Supabase Postgres project (or run Postgres locally). Flyway will create the schema automatically on first boot.

**2. Backend**
```bash
cd greengrid-backend
export DATABASE_URL=jdbc:postgresql://<host>:5432/<db>
export DATABASE_USERNAME=...
export DATABASE_PASSWORD=...
export JWT_SECRET=$(openssl rand -base64 48)
export TOKEN_ENCRYPTION_KEY=$(openssl rand -base64 32)
export GITHUB_CLIENT_ID=...
export GITHUB_CLIENT_SECRET=...
export GITHUB_REDIRECT_URI=http://localhost:8080/api/github/oauth/callback
export FRONTEND_URL=http://localhost:5173
mvn spring-boot:run
```

You'll need a GitHub OAuth App (GitHub → Settings → Developer settings → OAuth Apps) with its
**Authorization callback URL** set to exactly `GITHUB_REDIRECT_URI` above.

**3. Frontend**
```bash
cd greengrid-frontend
# edit js/config.js -> apiBaseUrl if not using localhost:8080
npx serve .   # or any static file server
```

## What's implemented

All 8 modules from the build plan are here: project foundation, JWT + GitHub OAuth security,
GitHub commit integration (via the Contents API), the save/commit/push workflow with LeetCode
auto-fetch, dashboard analytics (streaks, breakdowns, contribution calendar), multi-field search
with a revision tracker, account settings, and the full frontend.

## Known gaps / what to check before treating this as production-ready

Being direct about this, since it matters:

- **Not compiled or run.** This sandbox has no access to Maven Central, so `mvn compile` / `mvn test` have not actually been executed against this code. I checked brace/paren balance and reviewed every file by hand, but you should run a real build before deploying.
- **No automated tests.** None were written — I'd suggest starting with `ProblemService`, `StreakCalculator`, `TokenEncryptionService`, and `ProblemSpecifications`, since they carry the most logic.
- **Rate limiting** isn't implemented on auth endpoints — add it (e.g. bucket4j or a gateway-level limiter) before going live.
- **GitHub API pagination**: `listRepositoriesForAuthenticatedUser` fetches one page of 100 repos; a user with more won't see the rest without adding pagination.
- **Refresh token rotation**: refresh tokens are currently stateless JWTs, not stored/revocable server-side. Fine for a first version; add a revocation list (or move to opaque refresh tokens in the DB) if you need to support "log out everywhere" or token theft response.
- **Email delivery** (verification, password reset) isn't built — there's no flow for either yet.
