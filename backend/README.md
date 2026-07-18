# GreenGrid Backend

Java 21 / Spring Boot 3 backend for GreenGrid — a Git-based developer
learning platform. Every problem a user saves is committed to *their own*
GitHub repository via the GitHub Contents API, using their own encrypted
OAuth token.

## Module status

- [x] **Module 1 — Foundation**: project setup, domain entities, repositories,
      Flyway schema, global exception handling
- [ ] Module 2 — Security (JWT + GitHub OAuth + token encryption)
- [ ] Module 3 — GitHub integration (repo create/list, commit/push)
- [ ] Module 4 — Problem management (save workflow, LeetCode metadata fetch)
- [ ] Module 5 — Analytics & dashboard
- [ ] Module 6 — Search & revision tracking
- [ ] Module 7 — Settings & account management
- [ ] Module 8 — Frontend

## Required environment variables

| Variable | Purpose |
|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Supabase Postgres connection |
| `JWT_SECRET` | HMAC signing key for access/refresh tokens |
| `TOKEN_ENCRYPTION_KEY` | Base64 32-byte key, AES-256-GCM for GitHub tokens |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `GITHUB_REDIRECT_URI` | GitHub OAuth app credentials |
| `FRONTEND_URL` | Allowed CORS origin |
| `PORT` | Server port (Render/Railway inject this automatically) |

## Local development

```bash
# start a local Postgres (or point application-dev.yml at Supabase directly)
docker run --name greengrid-db -e POSTGRES_USER=greengrid \
  -e POSTGRES_PASSWORD=greengrid_local -e POSTGRES_DB=greengrid \
  -p 5432:5432 -d postgres:16

mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway runs automatically on startup and creates the schema from
`src/main/resources/db/migration`. Hibernate is set to `ddl-auto: validate`
deliberately — the migration files are the single source of truth for
schema, never entity auto-generation.

## Architecture notes

- **UUID primary keys** everywhere — safe to expose in API responses/URLs.
- **Per-user data isolation** is enforced at the repository-query level
  (every query is scoped by `user_id`), not just in controllers.
- **GitHub tokens are never stored in plaintext.** See
  `GitHubAccount.encryptedAccessToken` and (Module 2)
  `TokenEncryptionService`.
- **Commits/pushes use the GitHub Contents API**, not a local git binary —
  this keeps the backend stateless and avoids managing SSH keys or
  repo clones on ephemeral containers.
