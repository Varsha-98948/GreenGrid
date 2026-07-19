# GreenGrid Frontend

Plain HTML/CSS/vanilla JS + Bootstrap 5 + Monaco Editor. No build step required.

## Pages

| Page | Purpose |
|---|---|
| `index.html` | Landing page |
| `register.html` / `login.html` | Auth (email/password + "Continue with GitHub") |
| `auth-callback.html` | Picks up tokens after the GitHub sign-in redirect |
| `onboarding.html` | Connect GitHub → create or select a repository |
| `dashboard.html` | Streaks, breakdowns, contribution calendar, recent commits |
| `problem-form.html` | Add a problem (Monaco editor, LeetCode auto-fetch) |
| `search.html` | Multi-field search + revision tracker (Needs Revision / Mastered / Favorite) |
| `settings.html` | Reconnect GitHub, change repo, theme, export, delete account |

## Before deploying

Set `apiBaseUrl` in `js/config.js` to your deployed backend when the frontend
and API use different origins. For a same-origin reverse proxy, leave it empty:

```js
window.GG_CONFIG = { apiBaseUrl: 'https://your-backend.onrender.com' };
```

## Deploying to Vercel

This is a static site — no build command needed.

```bash
vercel deploy
```

Or connect the repo in the Vercel dashboard with:
- **Framework preset:** Other
- **Build command:** *(none)*
- **Output directory:** `.`

Make sure `FRONTEND_URL` on the backend matches your Vercel domain exactly (used for CORS and the OAuth redirect target).

## Architecture notes

- `js/api.js` is the single fetch wrapper: attaches the JWT, retries once via `/api/auth/refresh` on a 401, and normalizes errors into `GGApiError`.
- `js/shell.js` renders the shared sidebar/topbar shell around each authenticated page's `<div id="gg-page">` content — this is what keeps every app page's nav/logout logic in one place instead of duplicated per file.
- No frontend framework/build step by design, per the project's vanilla JS requirement — but every page is still split into small, single-purpose script includes rather than one monolithic file.
