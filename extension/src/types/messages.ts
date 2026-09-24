/**
 * Message types for communication between content scripts,
 * the background service worker, and the popup.
 */

import { AcceptedSolution } from './solution';

// --- Content Script → Service Worker ---

export interface SolutionDetectedMessage {
  type: 'SOLUTION_DETECTED';
  payload: AcceptedSolution;
}

export interface SaveSolutionMessage {
  type: 'SAVE_SOLUTION';
  payload: AcceptedSolution;
}

// --- Service Worker → Content Script ---

export interface SaveResultMessage {
  type: 'SAVE_RESULT';
  payload: SaveResult;
}

export interface SaveResult {
  success: boolean;
  problemName: string;
  revisionCount?: number;
  commitStatus?: string;
  isNewRevision?: boolean;
  error?: string;
  errorType?: 'AUTH_EXPIRED' | 'NOT_AUTHENTICATED' | 'NETWORK' | 'DUPLICATE' | 'GITHUB_FAILED' | 'UNKNOWN';
}

// --- Popup messages ---

export interface GetAuthStatusMessage {
  type: 'GET_AUTH_STATUS';
}

export interface AuthStatusResponse {
  type: 'AUTH_STATUS';
  payload: {
    isAuthenticated: boolean;
    email?: string;
    displayName?: string;
  };
}

export interface LoginMessage {
  type: 'LOGIN';
  payload: {
    email: string;
    password: string;
  };
}

export interface LoginResultMessage {
  type: 'LOGIN_RESULT';
  payload: {
    success: boolean;
    error?: string;
  };
}

export interface LogoutMessage {
  type: 'LOGOUT';
}

export type ExtensionMessage =
  | SolutionDetectedMessage
  | SaveSolutionMessage
  | SaveResultMessage
  | GetAuthStatusMessage
  | AuthStatusResponse
  | LoginMessage
  | LoginResultMessage
  | LogoutMessage;
