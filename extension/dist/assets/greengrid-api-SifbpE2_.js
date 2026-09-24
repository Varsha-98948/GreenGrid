const PREFIX = "[GreenGrid]";
const logger = {
  info: (...args) => {
    console.log(PREFIX, ...args);
  },
  warn: (...args) => {
    console.warn(PREFIX, ...args);
  },
  error: (...args) => {
    console.error(PREFIX, ...args);
  },
  debug: (...args) => {
    console.debug(PREFIX, ...args);
  }
};

const STORAGE_KEYS = {
  ACCESS_TOKEN: "gg_access_token",
  REFRESH_TOKEN: "gg_refresh_token",
  USER: "gg_user"
};
async function getAuthState() {
  try {
    const result = await chrome.storage.local.get([
      STORAGE_KEYS.ACCESS_TOKEN,
      STORAGE_KEYS.REFRESH_TOKEN,
      STORAGE_KEYS.USER
    ]);
    const accessToken = result[STORAGE_KEYS.ACCESS_TOKEN];
    const refreshToken = result[STORAGE_KEYS.REFRESH_TOKEN];
    const user = result[STORAGE_KEYS.USER];
    if (accessToken && refreshToken && user) {
      return { isAuthenticated: true, accessToken, refreshToken, user };
    }
    return { isAuthenticated: false };
  } catch (e) {
    logger.error("Failed to get auth state", e);
    return { isAuthenticated: false };
  }
}
async function setAuthState(accessToken, refreshToken, user) {
  await chrome.storage.local.set({
    [STORAGE_KEYS.ACCESS_TOKEN]: accessToken,
    [STORAGE_KEYS.REFRESH_TOKEN]: refreshToken,
    [STORAGE_KEYS.USER]: user
  });
  logger.info("Auth state saved for", user.email);
}
async function clearAuthState() {
  await chrome.storage.local.remove([
    STORAGE_KEYS.ACCESS_TOKEN,
    STORAGE_KEYS.REFRESH_TOKEN,
    STORAGE_KEYS.USER
  ]);
  logger.info("Auth state cleared");
}
async function login(email, password) {
  try {
    const baseUrl = await getApiBaseUrl();
    const response = await fetch(`${baseUrl}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password })
    });
    if (!response.ok) {
      const body2 = await response.json().catch(() => null);
      return {
        success: false,
        error: body2?.message || `Login failed (${response.status})`
      };
    }
    const body = await response.json();
    const data = body.data;
    await setAuthState(data.accessToken, data.refreshToken, {
      userId: data.userId,
      email: data.email,
      displayName: data.displayName
    });
    return { success: true };
  } catch (e) {
    logger.error("Login failed", e);
    return { success: false, error: "Unable to connect to GreenGrid." };
  }
}
async function refreshAccessToken() {
  try {
    const state = await getAuthState();
    if (!state.refreshToken) return false;
    const baseUrl = await getApiBaseUrl();
    const response = await fetch(`${baseUrl}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: state.refreshToken })
    });
    if (!response.ok) {
      logger.warn("Token refresh failed, clearing auth state");
      await clearAuthState();
      return false;
    }
    const body = await response.json();
    const data = body.data;
    await setAuthState(data.accessToken, data.refreshToken, {
      userId: data.userId,
      email: data.email,
      displayName: data.displayName
    });
    logger.info("Token refreshed successfully");
    return true;
  } catch (e) {
    logger.error("Token refresh error", e);
    await clearAuthState();
    return false;
  }
}

const API_BASE_URL_KEY = "gg_api_base_url";
const DEFAULT_PROD_URL = "https://greengrid-byh0.onrender.com";
async function getApiBaseUrl() {
  try {
    const result = await chrome.storage.local.get(API_BASE_URL_KEY);
    if (result[API_BASE_URL_KEY]) return result[API_BASE_URL_KEY];
  } catch {
  }
  return DEFAULT_PROD_URL;
}
async function setApiBaseUrl(url) {
  await chrome.storage.local.set({ [API_BASE_URL_KEY]: url });
}
async function apiRequest(path, options = {}, isRetry = false) {
  const state = await getAuthState();
  if (!state.isAuthenticated || !state.accessToken) {
    throw new ApiError(401, "Not authenticated");
  }
  const baseUrl = await getApiBaseUrl();
  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${state.accessToken}`,
    ...options.headers || {}
  };
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers
  });
  if (response.status === 401 && !isRetry) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return apiRequest(path, options, true);
    }
    throw new ApiError(401, "Session expired. Please reconnect to GreenGrid.");
  }
  return response;
}
class ApiError extends Error {
  constructor(status, message, existingProblemId) {
    super(message);
    this.status = status;
    this.existingProblemId = existingProblemId;
    this.name = "ApiError";
  }
}
function normalizeLanguage(lang) {
  const map = {
    cpp: "C++",
    "c++": "C++",
    java: "Java",
    python: "Python",
    python3: "Python",
    javascript: "JavaScript",
    typescript: "TypeScript",
    c: "C",
    csharp: "C#",
    "c#": "C#",
    go: "Go",
    golang: "Go",
    rust: "Rust",
    kotlin: "Kotlin",
    swift: "Swift",
    ruby: "Ruby",
    php: "PHP",
    scala: "Scala",
    sql: "SQL"
  };
  return map[lang.toLowerCase()] || lang;
}
async function saveSolution(solution) {
  const normalizedLang = normalizeLanguage(solution.language);
  try {
    let difficulty = "MEDIUM";
    let topics = [];
    try {
      const metaResponse = await apiRequest(
        `/api/problems/fetch-metadata?url=${encodeURIComponent(solution.problemUrl)}`,
        { method: "GET" }
      );
      if (metaResponse.ok) {
        const metaBody = await metaResponse.json();
        if (metaBody.data?.found) {
          difficulty = metaBody.data.difficulty || difficulty;
          topics = metaBody.data.topics || [];
        }
      }
    } catch (e) {
      logger.warn("Metadata fetch failed, using defaults", e);
    }
    const createPayload = {
      platform: "LeetCode",
      title: solution.problemName,
      problemUrl: solution.problemUrl,
      difficulty,
      topics,
      language: normalizedLang,
      code: solution.code,
      notes: null,
      timeComplexity: null,
      spaceComplexity: null,
      solvedDate: (/* @__PURE__ */ new Date()).toISOString().split("T")[0]
    };
    logger.info("Sending solution to backend", {
      title: solution.problemName,
      language: normalizedLang,
      codeLength: solution.code.length
    });
    const response = await apiRequest("/api/problems", {
      method: "POST",
      body: JSON.stringify(createPayload)
    });
    if (response.ok) {
      const body = await response.json();
      const data = body.data;
      logger.info("Save successful", { id: data.id, commitStatus: data.commitStatus });
      return {
        success: true,
        problemName: data.title,
        revisionCount: data.revisionCount,
        commitStatus: data.commitStatus,
        isNewRevision: false
      };
    }
    if (response.status === 409) {
      const errorBody2 = await response.json();
      const existingId = errorBody2.existingProblemId;
      if (existingId) {
        logger.info("Problem already exists, creating new revision", { existingId });
        const revisionPayload = {
          language: normalizedLang,
          code: solution.code,
          notes: null,
          timeComplexity: null,
          spaceComplexity: null
        };
        const revResponse = await apiRequest(`/api/problems/${existingId}/revisions`, {
          method: "POST",
          body: JSON.stringify(revisionPayload)
        });
        if (revResponse.ok) {
          const revBody = await revResponse.json();
          const revData = revBody.data;
          logger.info("Revision created", { revisionCount: revData.revisionCount });
          return {
            success: true,
            problemName: revData.title,
            revisionCount: revData.revisionCount,
            commitStatus: revData.commitStatus,
            isNewRevision: true
          };
        }
        const revError = await revResponse.json().catch(() => null);
        return {
          success: false,
          problemName: solution.problemName,
          error: revError?.message || "Failed to add revision.",
          errorType: "UNKNOWN"
        };
      }
      return {
        success: false,
        problemName: solution.problemName,
        error: "This solution is already saved in GreenGrid.",
        errorType: "DUPLICATE"
      };
    }
    const errorBody = await response.json().catch(() => null);
    const message = errorBody?.message || `Request failed (${response.status})`;
    if (response.status === 401) {
      return {
        success: false,
        problemName: solution.problemName,
        error: "Your GreenGrid session has expired. Please reconnect.",
        errorType: "AUTH_EXPIRED"
      };
    }
    if (response.status === 502) {
      return {
        success: false,
        problemName: solution.problemName,
        error: "GreenGrid could not complete the GitHub operation.",
        errorType: "GITHUB_FAILED"
      };
    }
    return {
      success: false,
      problemName: solution.problemName,
      error: message,
      errorType: "UNKNOWN"
    };
  } catch (e) {
    logger.error("Save failed with exception", e);
    if (e instanceof ApiError) {
      if (e.status === 401) {
        return {
          success: false,
          problemName: solution.problemName,
          error: "Not authenticated. Please connect your GreenGrid account.",
          errorType: "NOT_AUTHENTICATED"
        };
      }
    }
    return {
      success: false,
      problemName: solution.problemName,
      error: "Unable to connect to GreenGrid.",
      errorType: "NETWORK"
    };
  }
}

export { login as a, getApiBaseUrl as b, clearAuthState as c, setApiBaseUrl as d, getAuthState as g, logger as l, saveSolution as s };
