import React, { useEffect, useState } from 'react';
import { getAuthState, login, clearAuthState, AuthState } from '../services/auth-service';
import { getApiBaseUrl, setApiBaseUrl } from '../services/greengrid-api';

const PROD_URL = 'https://greengrid-byh0.onrender.com';
const DEV_URL = 'http://localhost:8080';
const FRONTEND_URL = 'https://varsha-98948.github.io/GreenGrid';

export const Popup: React.FC = () => {
  const [authState, setAuthState] = useState<AuthState>({ isAuthenticated: false });
  const [loading, setLoading] = useState(true);
  const [apiBaseUrl, setBaseUrl] = useState(PROD_URL);

  // Login form state
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loginLoading, setLoginLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    async function loadInitialData() {
      setLoading(true);
      const [state, url] = await Promise.all([getAuthState(), getApiBaseUrl()]);
      setAuthState(state);
      setBaseUrl(url);
      setLoading(false);
    }
    loadInitialData();
  }, []);

  const handleUrlChange = async (newUrl: string) => {
    setBaseUrl(newUrl);
    await setApiBaseUrl(newUrl);
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setErrorMessage('Please enter both email and password.');
      return;
    }

    setLoginLoading(true);
    setErrorMessage('');

    try {
      const res = await login(email, password);
      if (res.success) {
        const state = await getAuthState();
        setAuthState(state);
      } else {
        setErrorMessage(res.error || 'Failed to authenticate.');
      }
    } catch {
      setErrorMessage('Unable to connect to GreenGrid.');
    } finally {
      setLoginLoading(false);
    }
  };

  const handleLogout = async () => {
    await clearAuthState();
    setAuthState({ isAuthenticated: false });
  };

  const handleOpenGreenGrid = () => {
    window.open(FRONTEND_URL, '_blank');
  };

  if (loading) {
    return (
      <div style={styles.container}>
        <div style={styles.loadingWrap}>
          <div style={styles.spinner} />
        </div>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      {/* Brand Header */}
      <div style={styles.header}>
        <div style={styles.brandRow}>
          <span style={styles.logo}>🌱</span>
          <span style={styles.title}>GreenGrid</span>
        </div>
        <span style={styles.versionBadge}>v1.0</span>
      </div>

      {authState.isAuthenticated ? (
        /* Connected State */
        <div style={styles.body}>
          <div style={styles.statusCard}>
            <div style={styles.connectedBadge}>
              <span style={styles.check}>✓</span>
              <span>Connected</span>
            </div>

            <div style={styles.accountSection}>
              <div style={styles.userDisplay}>
                {authState.user?.displayName || 'GreenGrid User'}
              </div>
              <div style={styles.userEmail}>{authState.user?.email}</div>
            </div>

            <div style={styles.readyRow}>
              <span style={styles.pulseDot} />
              <span style={styles.readyText}>Ready to capture accepted submissions</span>
            </div>
          </div>

          {/* Environment Switcher */}
          <div style={styles.envRow}>
            <span style={styles.envLabel}>Backend API:</span>
            <select
              value={apiBaseUrl}
              onChange={(e) => handleUrlChange(e.target.value)}
              style={styles.select}
            >
              <option value={PROD_URL}>Production (Render)</option>
              <option value={DEV_URL}>Localhost (8080)</option>
            </select>
          </div>

          <div style={styles.actionColumn}>
            <button onClick={handleOpenGreenGrid} style={styles.primaryBtn}>
              Open GreenGrid Web
            </button>
            <button onClick={handleLogout} style={styles.secondaryBtn}>
              Disconnect Account
            </button>
          </div>
        </div>
      ) : (
        /* Disconnected / Login State */
        <div style={styles.body}>
          <div style={styles.introCard}>
            <div style={styles.introTitle}>Connect GreenGrid</div>
            <p style={styles.introDesc}>
              Log in to sync your accepted LeetCode solutions directly to your GreenGrid portfolio and GitHub repo.
            </p>
          </div>

          {errorMessage && <div style={styles.errorAlert}>{errorMessage}</div>}

          <form onSubmit={handleLogin} style={styles.form}>
            <div style={styles.formGroup}>
              <label style={styles.label}>Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="developer@example.com"
                style={styles.input}
                required
              />
            </div>

            <div style={styles.formGroup}>
              <label style={styles.label}>Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                style={styles.input}
                required
              />
            </div>

            <div style={styles.formGroup}>
              <label style={styles.label}>API Environment</label>
              <select
                value={apiBaseUrl}
                onChange={(e) => handleUrlChange(e.target.value)}
                style={styles.select}
              >
                <option value={PROD_URL}>Production (Render)</option>
                <option value={DEV_URL}>Localhost (8080)</option>
              </select>
            </div>

            <button type="submit" disabled={loginLoading} style={styles.primaryBtn}>
              {loginLoading ? 'Connecting...' : 'Connect to GreenGrid'}
            </button>
          </form>

          <div style={styles.footerLink}>
            <a
              href={FRONTEND_URL}
              target="_blank"
              rel="noreferrer"
              style={styles.link}
            >
              Don't have an account? Open GreenGrid
            </a>
          </div>
        </div>
      )}
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  container: {
    width: '320px',
    backgroundColor: '#0f172a',
    color: '#f8fafc',
    fontFamily:
      '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
    padding: '16px',
    boxSizing: 'border-box',
  },
  loadingWrap: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: '140px',
  },
  spinner: {
    width: '24px',
    height: '24px',
    border: '3px solid rgba(16, 185, 129, 0.2)',
    borderTop: '3px solid #10b981',
    borderRadius: '50%',
    animation: 'spin 0.8s linear infinite',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingBottom: '12px',
    borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
    marginBottom: '14px',
  },
  brandRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
  },
  logo: {
    fontSize: '20px',
  },
  title: {
    fontSize: '16px',
    fontWeight: '700',
    color: '#10b981',
    letterSpacing: '0.02em',
  },
  versionBadge: {
    fontSize: '11px',
    color: '#64748b',
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
    padding: '2px 6px',
    borderRadius: '4px',
  },
  body: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
  },
  statusCard: {
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
    border: '1px solid rgba(16, 185, 129, 0.2)',
    borderRadius: '10px',
    padding: '12px',
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
  },
  connectedBadge: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '6px',
    backgroundColor: 'rgba(16, 185, 129, 0.15)',
    color: '#34d399',
    borderRadius: '6px',
    padding: '3px 8px',
    fontSize: '12px',
    fontWeight: '600',
    alignSelf: 'flex-start',
  },
  check: {
    fontWeight: 'bold',
  },
  accountSection: {
    display: 'flex',
    flexDirection: 'column',
    gap: '2px',
  },
  userDisplay: {
    fontSize: '15px',
    fontWeight: '600',
    color: '#ffffff',
  },
  userEmail: {
    fontSize: '12px',
    color: '#94a3b8',
  },
  readyRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    paddingTop: '6px',
    borderTop: '1px solid rgba(255, 255, 255, 0.06)',
  },
  pulseDot: {
    width: '8px',
    height: '8px',
    borderRadius: '50%',
    backgroundColor: '#10b981',
    boxShadow: '0 0 8px #10b981',
  },
  readyText: {
    fontSize: '11px',
    color: '#94a3b8',
  },
  envRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '8px',
    fontSize: '12px',
    color: '#94a3b8',
  },
  envLabel: {
    fontSize: '12px',
    color: '#94a3b8',
  },
  select: {
    backgroundColor: '#1e293b',
    border: '1px solid rgba(255, 255, 255, 0.12)',
    color: '#f8fafc',
    borderRadius: '6px',
    padding: '6px 8px',
    fontSize: '12px',
    outline: 'none',
  },
  actionColumn: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    marginTop: '6px',
  },
  primaryBtn: {
    width: '100%',
    backgroundColor: '#10b981',
    color: '#ffffff',
    border: 'none',
    borderRadius: '8px',
    padding: '10px 14px',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
    boxShadow: '0 2px 4px rgba(16, 185, 129, 0.3)',
    transition: 'background-color 0.15s ease',
  },
  secondaryBtn: {
    width: '100%',
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    color: '#cbd5e1',
    border: '1px solid rgba(255, 255, 255, 0.1)',
    borderRadius: '8px',
    padding: '8px 12px',
    fontSize: '12px',
    fontWeight: '500',
    cursor: 'pointer',
  },
  introCard: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  introTitle: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#ffffff',
  },
  introDesc: {
    fontSize: '12px',
    color: '#94a3b8',
    lineHeight: '1.4',
    margin: 0,
  },
  errorAlert: {
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    border: '1px solid rgba(239, 68, 68, 0.3)',
    color: '#fca5a5',
    padding: '8px 10px',
    borderRadius: '6px',
    fontSize: '12px',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
  },
  formGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '4px',
  },
  label: {
    fontSize: '11px',
    fontWeight: '500',
    color: '#94a3b8',
  },
  input: {
    backgroundColor: '#1e293b',
    border: '1px solid rgba(255, 255, 255, 0.12)',
    color: '#ffffff',
    borderRadius: '6px',
    padding: '8px 10px',
    fontSize: '13px',
    outline: 'none',
    boxSizing: 'border-box',
  },
  footerLink: {
    textAlign: 'center',
    marginTop: '6px',
  },
  link: {
    fontSize: '11px',
    color: '#10b981',
    textDecoration: 'none',
  },
};
