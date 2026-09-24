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
