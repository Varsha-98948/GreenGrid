/**
 * Authentication service for the GreenGrid extension.
 * Stores JWT tokens in chrome.storage.local (service worker accessible,
 * more secure than content script localStorage).
 *
 * Reuses the existing GreenGrid JWT auth system:
 *   - POST /api/auth/login → { accessToken, refreshToken, userId, email, displayName }
 *   - POST /api/auth/refresh → { accessToken, refreshToken, ... }
 */

import { logger } from '../utils/logger';
import { getApiBaseUrl } from './greengrid-api';

const STORAGE_KEYS = {
  ACCESS_TOKEN: 'gg_access_token',
  REFRESH_TOKEN: 'gg_refresh_token',
  USER: 'gg_user',
} as const;

export interface StoredUser {
  userId: string;
  email: string;
  displayName: string;
}

export interface AuthState {
  isAuthenticated: boolean;
  accessToken?: string;
  refreshToken?: string;
  user?: StoredUser;
}

/**
 * Get the current auth state from chrome.storage.local.
 */
export async function getAuthState(): Promise<AuthState> {
  try {
    const result = await chrome.storage.local.get([
      STORAGE_KEYS.ACCESS_TOKEN,
      STORAGE_KEYS.REFRESH_TOKEN,
      STORAGE_KEYS.USER,
    ]);

    const accessToken = result[STORAGE_KEYS.ACCESS_TOKEN];
    const refreshToken = result[STORAGE_KEYS.REFRESH_TOKEN];
    const user = result[STORAGE_KEYS.USER];

    if (accessToken && refreshToken && user) {
      return { isAuthenticated: true, accessToken, refreshToken, user };
    }
    return { isAuthenticated: false };
  } catch (e) {
    logger.error('Failed to get auth state', e);
    return { isAuthenticated: false };
  }
}

/**
 * Store auth tokens and user info after successful login.
 */
export async function setAuthState(
  accessToken: string,
  refreshToken: string,
  user: StoredUser,
): Promise<void> {
  await chrome.storage.local.set({
    [STORAGE_KEYS.ACCESS_TOKEN]: accessToken,
    [STORAGE_KEYS.REFRESH_TOKEN]: refreshToken,
    [STORAGE_KEYS.USER]: user,
  });
  logger.info('Auth state saved for', user.email);
}

/**
 * Clear all stored auth data.
 */
export async function clearAuthState(): Promise<void> {
  await chrome.storage.local.remove([
    STORAGE_KEYS.ACCESS_TOKEN,
    STORAGE_KEYS.REFRESH_TOKEN,
    STORAGE_KEYS.USER,
  ]);
  logger.info('Auth state cleared');
}

/**
 * Login to GreenGrid using email/password.
 * Uses the existing POST /api/auth/login endpoint.
 */
export async function login(
  email: string,
  password: string,
): Promise<{ success: boolean; error?: string }> {
  try {
    const baseUrl = await getApiBaseUrl();
    const response = await fetch(`${baseUrl}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
      const body = await response.json().catch(() => null);
      return {
        success: false,
        error: body?.message || `Login failed (${response.status})`,
      };
    }

    const body = await response.json();
    const data = body.data;

    await setAuthState(data.accessToken, data.refreshToken, {
      userId: data.userId,
      email: data.email,
      displayName: data.displayName,
    });

    return { success: true };
  } catch (e) {
    logger.error('Login failed', e);
    return { success: false, error: 'Unable to connect to GreenGrid.' };
  }
}

/**
 * Attempt to refresh the access token using the stored refresh token.
 * Returns true if refresh succeeded.
 */
export async function refreshAccessToken(): Promise<boolean> {
  try {
    const state = await getAuthState();
    if (!state.refreshToken) return false;

    const baseUrl = await getApiBaseUrl();
    const response = await fetch(`${baseUrl}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: state.refreshToken }),
    });

    if (!response.ok) {
      logger.warn('Token refresh failed, clearing auth state');
      await clearAuthState();
      return false;
    }

    const body = await response.json();
    const data = body.data;

    await setAuthState(data.accessToken, data.refreshToken, {
      userId: data.userId,
      email: data.email,
      displayName: data.displayName,
    });

    logger.info('Token refreshed successfully');
    return true;
  } catch (e) {
    logger.error('Token refresh error', e);
    await clearAuthState();
    return false;
  }
}

/**
 * Sign in using GitHub OAuth via chrome.identity.launchWebAuthFlow.
 * Reuses the existing GreenGrid backend GitHub OAuth infrastructure:
 *   - Requests authorize URL from GET /api/auth/github/login-url?redirect={redirectUri}
 *   - Launches Chrome's interactive web auth flow
 *   - Receives redirection from GreenGrid backend carrying JWT accessToken and refreshToken in hash
 *   - Stores session in chrome.storage.local using setAuthState
 */
export async function loginWithGitHub(): Promise<{ success: boolean; error?: string }> {
  try {
    const baseUrl = await getApiBaseUrl();
    const redirectUri = chrome.identity.getRedirectURL();

    logger.info('Initiating GitHub OAuth flow with redirect URI:', redirectUri);

    const loginUrlResponse = await fetch(
      `${baseUrl}/api/auth/github/login-url?redirect=${encodeURIComponent(redirectUri)}`,
      { method: 'GET' }
    );

    if (!loginUrlResponse.ok) {
      const body = await loginUrlResponse.json().catch(() => null);
      return {
        success: false,
        error: body?.message || 'Failed to initialize GitHub sign-in.',
      };
    }

    const resJson = await loginUrlResponse.json();
    const authorizeUrl = resJson.data?.authorizeUrl;
    if (!authorizeUrl) {
      return { success: false, error: 'Invalid OAuth URL returned from GreenGrid.' };
    }

    const callbackUrl = await new Promise<string>((resolve, reject) => {
      chrome.identity.launchWebAuthFlow(
        {
          url: authorizeUrl,
          interactive: true,
        },
        (responseUrl) => {
          if (chrome.runtime.lastError) {
            reject(new Error(chrome.runtime.lastError.message));
          } else if (!responseUrl) {
            reject(new Error('No response URL received from GitHub.'));
          } else {
            resolve(responseUrl);
          }
        }
      );
    });

    const parsedUrl = new URL(callbackUrl);
    const hash = parsedUrl.hash.startsWith('#') ? parsedUrl.hash.substring(1) : parsedUrl.hash;
    const params = new URLSearchParams(hash);

    const accessToken = params.get('accessToken');
    const refreshToken = params.get('refreshToken');

    if (!accessToken || !refreshToken) {
      return { success: false, error: 'GitHub login failed. Please try again.' };
    }

    let userId = '';
    let email = '';
    let displayName = 'Developer';

    try {
      const payloadBase64 = accessToken.split('.')[1];
      const decoded = JSON.parse(atob(payloadBase64.replace(/-/g, '+').replace(/_/g, '/')));
      userId = decoded.sub || '';
      email = decoded.email || '';
      displayName = email ? email.split('@')[0] : 'Developer';
    } catch (err) {
      logger.warn('Failed to parse token payload for user details', err);
    }

    await setAuthState(accessToken, refreshToken, {
      userId,
      email,
      displayName,
    });

    logger.info('GitHub login succeeded for', email);
    return { success: true };
  } catch (e: unknown) {
    const err = e as Error;
    logger.error('GitHub login failed:', err);
    if (err.message && err.message.toLowerCase().includes('user') && err.message.toLowerCase().includes('cancel')) {
      return { success: false, error: 'GitHub login was cancelled.' };
    }
    return { success: false, error: 'GitHub login failed. Please try again.' };
  }
}
