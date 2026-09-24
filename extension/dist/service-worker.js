import { l as logger, c as clearAuthState, a as login, g as getAuthState, s as saveSolution } from './assets/greengrid-api-BoxarH-a.js';

chrome.runtime.onInstalled.addListener((details) => {
  logger.info("GreenGrid extension installed/updated", details.reason);
});
chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  logger.debug("Received extension message:", message.type);
  switch (message.type) {
    case "SAVE_SOLUTION": {
      saveSolution(message.payload).then((result) => {
        logger.info("Save completed with result:", result);
        sendResponse(result);
      }).catch((error) => {
        logger.error("Unexpected error in saveSolution:", error);
        sendResponse({
          success: false,
          problemName: message.payload.problemName,
          error: error?.message || "Unexpected extension error",
          errorType: "UNKNOWN"
        });
      });
      return true;
    }
    case "GET_AUTH_STATUS": {
      getAuthState().then((state) => {
        sendResponse({
          isAuthenticated: state.isAuthenticated,
          email: state.user?.email,
          displayName: state.user?.displayName
        });
      }).catch(() => {
        sendResponse({ isAuthenticated: false });
      });
      return true;
    }
    case "LOGIN": {
      login(message.payload.email, message.payload.password).then((result) => {
        sendResponse(result);
      }).catch((error) => {
        sendResponse({
          success: false,
          error: error?.message || "Login failed"
        });
      });
      return true;
    }
    case "LOGOUT": {
      clearAuthState().then(() => {
        sendResponse({ success: true });
      }).catch(() => {
        sendResponse({ success: false });
      });
      return true;
    }
    default: {
      logger.warn("Unknown message type received");
      return false;
    }
  }
});
