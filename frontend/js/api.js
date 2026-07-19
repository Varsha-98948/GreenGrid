/**
 * GreenGrid API client. Every other JS module calls through here rather
 * than using fetch() directly, so token storage/refresh/error-shape stays
 * in exactly one place.
 */
const GG_API_BASE = window.GG_CONFIG?.apiBaseUrl || '';

const GGAuth = {
  getAccessToken: () => localStorage.getItem('gg_access_token'),
  getRefreshToken: () => localStorage.getItem('gg_refresh_token'),

  setSession(auth) {
    localStorage.setItem('gg_access_token', auth.accessToken);
    localStorage.setItem('gg_refresh_token', auth.refreshToken);
    localStorage.setItem('gg_user', JSON.stringify({
      userId: auth.userId,
      email: auth.email,
      displayName: auth.displayName,
      onboardingCompleted: auth.onboardingCompleted,
    }));
  },

  updateOnboardingCompleted(value) {
    const user = GGAuth.getUser();
    if (user) {
      user.onboardingCompleted = value;
      localStorage.setItem('gg_user', JSON.stringify(user));
    }
  },

  getUser() {
    const raw = localStorage.getItem('gg_user');
    return raw ? JSON.parse(raw) : null;
  },

  clearSession() {
    localStorage.removeItem('gg_access_token');
    localStorage.removeItem('gg_refresh_token');
    localStorage.removeItem('gg_user');
  },

  isAuthenticated: () => !!localStorage.getItem('gg_access_token'),

  /** Redirects to login if there's no session. Call at the top of every protected page. */
  requireAuth() {
    if (!GGAuth.isAuthenticated()) {
      window.location.href = 'login.html';
    }
  },

  logout() {
    GGAuth.clearSession();
    window.location.href = 'login.html';
  },
};

class GGApiError extends Error {
  constructor(status, body) {
    super(body?.message || 'Request failed');
    this.status = status;
    this.body = body;
  }
}

async function ggRequest(path, options = {}, _isRetry = false) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  const token = GGAuth.getAccessToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const response = await fetch(`${GG_API_BASE}${path}`, { ...options, headers });

  if (response.status === 401 && !_isRetry && GGAuth.getRefreshToken()) {
    const refreshed = await ggTryRefresh();
    if (refreshed) return ggRequest(path, options, true);
    GGAuth.logout();
    return;
  }

  let body = null;
  const text = await response.text();
  if (text) {
    try { body = JSON.parse(text); } catch { body = null; }
  }

  if (!response.ok) {
    throw new GGApiError(response.status, body);
  }
  return body;
}

async function ggTryRefresh() {
  try {
    const res = await fetch(`${GG_API_BASE}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: GGAuth.getRefreshToken() }),
    });
    if (!res.ok) return false;
    const body = await res.json();
    GGAuth.setSession(body.data);
    return true;
  } catch {
    return false;
  }
}

const GGApi = {
  get: (path) => ggRequest(path, { method: 'GET' }),
  post: (path, data) => ggRequest(path, { method: 'POST', body: data ? JSON.stringify(data) : undefined }),
  put: (path, data) => ggRequest(path, { method: 'PUT', body: JSON.stringify(data) }),
  patch: (path, data) => ggRequest(path, { method: 'PATCH', body: JSON.stringify(data) }),
  delete: (path) => ggRequest(path, { method: 'DELETE' }),
};
