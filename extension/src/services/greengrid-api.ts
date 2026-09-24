/**
 * GreenGrid API client for the extension.
 * Handles authenticated requests to the existing GreenGrid backend.
 *
 * Integration strategy:
 *   - POST /api/problems → Create new problem (includes Revision 1 + GitHub commit)
 *   - POST /api/problems/{id}/revisions → Add revision to existing problem
 *   - GET /api/problems/fetch-metadata → Auto-fill LeetCode metadata
 *
 * On 409 (DuplicateProblemException), the response includes existingProblemId
 * so we can automatically add a revision instead.
 */

import { AcceptedSolution } from '../types/solution';
import { SaveResult } from '../types/messages';
import { getAuthState, refreshAccessToken, clearAuthState } from './auth-service';
import { logger } from '../utils/logger';

const API_BASE_URL_KEY = 'gg_api_base_url';
const DEFAULT_DEV_URL = 'http://localhost:8080';
const DEFAULT_PROD_URL = 'https://greengrid-byh0.onrender.com';

/**
 * Get the configured API base URL.
 */
export async function getApiBaseUrl(): Promise<string> {
  try {
    const result = await chrome.storage.local.get(API_BASE_URL_KEY);
    if (result[API_BASE_URL_KEY]) return result[API_BASE_URL_KEY];
  } catch {
    // Fallback for contexts without chrome.storage
  }
  return DEFAULT_PROD_URL;
}

/**
 * Set the API base URL (for switching between dev/prod).
 */
export async function setApiBaseUrl(url: string): Promise<void> {
  await chrome.storage.local.set({ [API_BASE_URL_KEY]: url });
}

/**
 * Make an authenticated request to the GreenGrid API.
 * Automatically retries with a refreshed token on 401.
 */
async function apiRequest(
  path: string,
  options: RequestInit = {},
  isRetry = false,
): Promise<Response> {
  const state = await getAuthState();

  if (!state.isAuthenticated || !state.accessToken) {
    throw new ApiError(401, 'Not authenticated');
  }

  const baseUrl = await getApiBaseUrl();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${state.accessToken}`,
    ...(options.headers as Record<string, string> || {}),
  };

  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers,
  });

  // Auto-refresh on 401
  if (response.status === 401 && !isRetry) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return apiRequest(path, options, true);
    }
    throw new ApiError(401, 'Session expired. Please reconnect to GreenGrid.');
  }

  return response;
}

class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public existingProblemId?: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/**
 * Map the LeetCode language identifier to GreenGrid's expected format.
 * LeetCode uses lowercase identifiers; GreenGrid stores them as display names.
 */
function normalizeLanguage(lang: string): string {
  const map: Record<string, string> = {
    cpp: 'C++',
    'c++': 'C++',
    java: 'Java',
    python: 'Python',
    python3: 'Python',
    javascript: 'JavaScript',
    typescript: 'TypeScript',
    c: 'C',
    csharp: 'C#',
    'c#': 'C#',
    go: 'Go',
    golang: 'Go',
    rust: 'Rust',
    kotlin: 'Kotlin',
    swift: 'Swift',
    ruby: 'Ruby',
    php: 'PHP',
    scala: 'Scala',
    sql: 'SQL',
  };
  return map[lang.toLowerCase()] || lang;
}

/**
 * Save an accepted solution to GreenGrid.
 *
 * Flow:
 * 1. Try POST /api/problems with full problem data
 * 2. If 409 (duplicate) → POST /api/problems/{existingId}/revisions
 * 3. Return result with revision info
 */
export async function saveSolution(solution: AcceptedSolution): Promise<SaveResult> {
  const normalizedLang = normalizeLanguage(solution.language);

  try {
    // First, try fetching LeetCode metadata for difficulty/topics
    let difficulty = 'MEDIUM'; // sensible default
    let topics: string[] = [];

    try {
      const metaResponse = await apiRequest(
        `/api/problems/fetch-metadata?url=${encodeURIComponent(solution.problemUrl)}`,
        { method: 'GET' },
      );

      if (metaResponse.ok) {
        const metaBody = await metaResponse.json();
        if (metaBody.data?.found) {
          difficulty = metaBody.data.difficulty || difficulty;
          topics = metaBody.data.topics || [];
        }
      }
    } catch (e) {
      logger.warn('Metadata fetch failed, using defaults', e);
    }

    // Step 1: Try creating the problem
    const createPayload = {
      platform: 'LeetCode',
      title: solution.problemName,
      problemUrl: solution.problemUrl,
      difficulty,
      topics,
      language: normalizedLang,
      code: solution.code,
      notes: null,
      timeComplexity: null,
      spaceComplexity: null,
      solvedDate: new Date().toISOString().split('T')[0],
    };

    logger.info('Sending solution to backend', {
      title: solution.problemName,
      language: normalizedLang,
      codeLength: solution.code.length,
    });

    const response = await apiRequest('/api/problems', {
      method: 'POST',
      body: JSON.stringify(createPayload),
    });

    if (response.ok) {
      const body = await response.json();
      const data = body.data;
      logger.info('Save successful', { id: data.id, commitStatus: data.commitStatus });

      return {
        success: true,
        problemName: data.title,
        revisionCount: data.revisionCount,
        commitStatus: data.commitStatus,
        isNewRevision: false,
      };
    }

    // Step 2: Handle duplicate — add revision to existing problem
    if (response.status === 409) {
      const errorBody = await response.json();
      const existingId = errorBody.existingProblemId;

      if (existingId) {
        logger.info('Problem already exists, creating new revision', { existingId });

        const revisionPayload = {
          language: normalizedLang,
          code: solution.code,
          notes: null,
          timeComplexity: null,
          spaceComplexity: null,
        };

        const revResponse = await apiRequest(`/api/problems/${existingId}/revisions`, {
          method: 'POST',
          body: JSON.stringify(revisionPayload),
        });

        if (revResponse.ok) {
          const revBody = await revResponse.json();
          const revData = revBody.data;
          logger.info('Revision created', { revisionCount: revData.revisionCount });

          return {
            success: true,
            problemName: revData.title,
            revisionCount: revData.revisionCount,
            commitStatus: revData.commitStatus,
            isNewRevision: true,
          };
        }

        const revError = await revResponse.json().catch(() => null);
        return {
          success: false,
          problemName: solution.problemName,
          error: revError?.message || 'Failed to add revision.',
          errorType: 'UNKNOWN',
        };
      }

      return {
        success: false,
        problemName: solution.problemName,
        error: 'This solution is already saved in GreenGrid.',
        errorType: 'DUPLICATE',
      };
    }

    // Other errors
    const errorBody = await response.json().catch(() => null);
    const message = errorBody?.message || `Request failed (${response.status})`;

    if (response.status === 401) {
      return {
        success: false,
        problemName: solution.problemName,
        error: 'Your GreenGrid session has expired. Please reconnect.',
        errorType: 'AUTH_EXPIRED',
      };
    }

    if (response.status === 502) {
      return {
        success: false,
        problemName: solution.problemName,
        error: 'GreenGrid could not complete the GitHub operation.',
        errorType: 'GITHUB_FAILED',
      };
    }

    return {
      success: false,
      problemName: solution.problemName,
      error: message,
      errorType: 'UNKNOWN',
    };
  } catch (e) {
    logger.error('Save failed with exception', e);

    if (e instanceof ApiError) {
      if (e.status === 401) {
        return {
          success: false,
          problemName: solution.problemName,
          error: 'Not authenticated. Please connect your GreenGrid account.',
          errorType: 'NOT_AUTHENTICATED',
        };
      }
    }

    return {
      success: false,
      problemName: solution.problemName,
      error: 'Unable to connect to GreenGrid.',
      errorType: 'NETWORK',
    };
  }
}
