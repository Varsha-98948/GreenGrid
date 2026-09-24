/**
 * Content Script for GreenGrid Extension
 * Runs on supported coding platform pages (initially LeetCode).
 *
 * Responsibilities:
 * - Boots the platform adapter
 * - Mounts the GreenGrid confirmation UI in an isolated Shadow DOM container
 * - Handles communication with background service worker
 */

import React from 'react';
import ReactDOM from 'react-dom/client';
import { LeetCodeAdapter } from '../adapters/leetcode-adapter';
import { SavePrompt } from '../components/SavePrompt';
import { AcceptedSolution } from '../types/solution';
import { SaveResult, SaveSolutionMessage } from '../types/messages';
import { logger } from '../utils/logger';

let activeRoot: ReactDOM.Root | null = null;
let activeContainer: HTMLElement | null = null;

function dismissPrompt() {
  if (activeRoot) {
    activeRoot.unmount();
    activeRoot = null;
  }
  if (activeContainer) {
    activeContainer.remove();
    activeContainer = null;
  }
}

function showSavePrompt(solution: AcceptedSolution) {
  // Dismiss any existing active prompt
  dismissPrompt();

  logger.info('Displaying GreenGrid confirmation prompt for', solution.problemName);

  // Create isolated container
  const container = document.createElement('div');
  container.id = 'greengrid-root';
  container.style.position = 'fixed';
  container.style.bottom = '24px';
  container.style.right = '24px';
  container.style.zIndex = '2147483647';
  container.style.pointerEvents = 'auto';

  // Attach Shadow DOM to prevent LeetCode's Tailwind / styles from interfering
  const shadow = container.attachShadow({ mode: 'open' });

  // Add keyframe animations for spinner and entrance
  const styleEl = document.createElement('style');
  styleEl.textContent = `
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    @keyframes gg-slide-in {
      from {
        opacity: 0;
        transform: translateY(16px) scale(0.96);
      }
      to {
        opacity: 1;
        transform: translateY(0) scale(1);
      }
    }
    :host {
      animation: gg-slide-in 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    }
    * {
      box-sizing: border-box;
    }
  `;
  shadow.appendChild(styleEl);

  const mountPoint = document.createElement('div');
  shadow.appendChild(mountPoint);

  document.body.appendChild(container);
  activeContainer = container;

  const handleSave = async (): Promise<SaveResult> => {
    logger.info('Forwarding SAVE_SOLUTION message to service worker');
    const msg: SaveSolutionMessage = {
      type: 'SAVE_SOLUTION',
      payload: solution,
    };

    return new Promise((resolve) => {
      chrome.runtime.sendMessage(msg, (response: SaveResult) => {
        if (chrome.runtime.lastError) {
          logger.error('Runtime message error:', chrome.runtime.lastError.message);
          resolve({
            success: false,
            problemName: solution.problemName,
            error: chrome.runtime.lastError.message || 'Extension background error',
            errorType: 'UNKNOWN',
          });
        } else {
          resolve(response);
        }
      });
    });
  };

  const root = ReactDOM.createRoot(mountPoint);
  activeRoot = root;

  root.render(
    React.createElement(SavePrompt, {
      solution,
      onSave: handleSave,
      onDismiss: dismissPrompt,
    })
  );
}

// --- Injected "Add to GitHub" Button on LeetCode ---

const BTN_WRAPPER_ID = 'greengrid-btn-wrapper';
const BTN_ID = 'greengrid-add-github-btn';
const POPOVER_ID = 'greengrid-popover';
const STYLE_ID = 'greengrid-injected-styles';

function ensureStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement('style');
  style.id = STYLE_ID;
  style.textContent = `
    .greengrid-btn-wrapper {
      position: relative;
      display: inline-flex;
      align-items: center;
      margin-right: 8px;
      vertical-align: middle;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    }
    .greengrid-github-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background-color: #10b981;
      background-image: linear-gradient(135deg, #10b981 0%, #059669 100%);
      color: #ffffff;
      border: 1px solid rgba(16, 185, 129, 0.4);
      border-radius: 6px;
      padding: 4px 10px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.15s ease;
      line-height: 1.4;
      user-select: none;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
    }
    .greengrid-github-btn:hover:not(:disabled) {
      background-image: linear-gradient(135deg, #059669 0%, #047857 100%);
      box-shadow: 0 2px 5px rgba(16, 185, 129, 0.35);
      transform: translateY(-1px);
    }
    .greengrid-github-btn:active:not(:disabled) {
      transform: translateY(0);
    }
    .greengrid-github-btn:disabled {
      opacity: 0.65;
      cursor: not-allowed;
      transform: none;
    }
    .greengrid-github-btn.greengrid-status-success {
      background-image: linear-gradient(135deg, #10b981 0%, #059669 100%) !important;
      border-color: #10b981 !important;
    }
    .greengrid-github-btn.greengrid-status-error {
      background-image: linear-gradient(135deg, #ef4444 0%, #dc2626 100%) !important;
      border-color: #ef4444 !important;
    }
    .greengrid-github-btn.greengrid-status-warn {
      background-image: linear-gradient(135deg, #f59e0b 0%, #d97706 100%) !important;
      border-color: #f59e0b !important;
    }
    .greengrid-popover {
      position: absolute;
      bottom: calc(100% + 8px);
      left: 50%;
      transform: translateX(-50%);
      background: #1e293b;
      color: #f8fafc;
      padding: 7px 11px;
      border-radius: 6px;
      font-size: 12px;
      line-height: 1.4;
      white-space: nowrap;
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.5);
      border: 1px solid rgba(255, 255, 255, 0.12);
      z-index: 999999;
      pointer-events: none;
    }
    .greengrid-popover::after {
      content: "";
      position: absolute;
      top: 100%;
      left: 50%;
      margin-left: -5px;
      border-width: 5px;
      border-style: solid;
      border-color: #1e293b transparent transparent transparent;
    }
  `;
  document.head.appendChild(style);
}

function createGitHubButton(adapter: LeetCodeAdapter): HTMLElement {
  const wrapper = document.createElement('div');
  wrapper.id = BTN_WRAPPER_ID;
  wrapper.className = 'greengrid-btn-wrapper';

  const btn = document.createElement('button');
  btn.id = BTN_ID;
  btn.type = 'button';
  btn.className = 'greengrid-github-btn';
  btn.innerHTML = `
    <span class="greengrid-btn-icon">＋</span>
    <span class="greengrid-btn-text">Add to GitHub</span>
  `;

  const popover = document.createElement('div');
  popover.id = POPOVER_ID;
  popover.className = 'greengrid-popover';
  popover.style.display = 'none';

  wrapper.appendChild(btn);
  wrapper.appendChild(popover);

  let popoverTimer: number | null = null;
  const showPopover = (msg: string, duration = 4000) => {
    popover.textContent = msg;
    popover.style.display = 'block';
    if (popoverTimer) window.clearTimeout(popoverTimer);
    popoverTimer = window.setTimeout(() => {
      popover.style.display = 'none';
    }, duration);
  };

  btn.addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();

    // 1. Check whether user is authenticated with GreenGrid
    chrome.runtime.sendMessage({ type: 'GET_AUTH_STATUS' }, (authStatus: { isAuthenticated?: boolean } | undefined) => {
      if (chrome.runtime.lastError || !authStatus?.isAuthenticated) {
        showPopover('Please sign in to GreenGrid via the extension icon first.');
        return;
      }

      // 2. Check for current accepted solution
      const solution = adapter.extractCurrentAcceptedSolution();
      if (!solution || !solution.code || solution.code.trim().length === 0) {
        btn.classList.add('greengrid-status-warn');
        btn.innerHTML = `
          <span class="greengrid-btn-icon">ℹ</span>
          <span class="greengrid-btn-text">No accepted solution</span>
        `;
        showPopover('No accepted solution found. Please submit your solution and ensure it passes first.');
        setTimeout(() => {
          btn.classList.remove('greengrid-status-warn');
          btn.innerHTML = `
            <span class="greengrid-btn-icon">＋</span>
            <span class="greengrid-btn-text">Add to GitHub</span>
          `;
        }, 3500);
        return;
      }

      // 3. UI state: Processing
      btn.disabled = true;
      btn.innerHTML = `
        <span class="greengrid-btn-icon">⏳</span>
        <span class="greengrid-btn-text">Adding...</span>
      `;

      // 4. Send SAVE_SOLUTION message to service worker
      chrome.runtime.sendMessage({ type: 'SAVE_SOLUTION', payload: solution }, (result: SaveResult) => {
        if (chrome.runtime.lastError || !result?.success) {
          btn.classList.add('greengrid-status-error');
          btn.innerHTML = `
            <span class="greengrid-btn-icon">⚠</span>
            <span class="greengrid-btn-text">Failed</span>
          `;
          showPopover(result?.error || chrome.runtime.lastError?.message || 'Failed to commit to GitHub.');
          setTimeout(() => {
            btn.disabled = false;
            btn.classList.remove('greengrid-status-error');
            btn.innerHTML = `
              <span class="greengrid-btn-icon">＋</span>
              <span class="greengrid-btn-text">Add to GitHub</span>
            `;
          }, 3500);
          return;
        }

        // Success state
        btn.classList.add('greengrid-status-success');
        btn.innerHTML = `
          <span class="greengrid-btn-icon">✓</span>
          <span class="greengrid-btn-text">Added to GitHub</span>
        `;
        showPopover(
          result.isNewRevision
            ? `Revision #${result.revisionCount || 2} committed to GitHub!`
            : 'Committed solution to GitHub!'
        );
        setTimeout(() => {
          btn.disabled = false;
          btn.classList.remove('greengrid-status-success');
          btn.innerHTML = `
            <span class="greengrid-btn-icon">＋</span>
            <span class="greengrid-btn-text">Add to GitHub</span>
          `;
        }, 4000);
      });
    });
  });

  return wrapper;
}

function injectGitHubButton(adapter: LeetCodeAdapter) {
  const existingWrapper = document.getElementById(BTN_WRAPPER_ID);

  // Find target anchor: prefer LeetCode's submit button container
  const submitBtn = document.querySelector(
    'button[data-e2e-locator="console-submit-button"], button[data-cy="submit-code-btn"]'
  );

  const targetParent = submitBtn?.parentElement || document.querySelector('div[class*="action__"]');
  if (!targetParent) return;

  if (existingWrapper && existingWrapper.isConnected && targetParent.contains(existingWrapper)) {
    return;
  }

  if (existingWrapper) {
    existingWrapper.remove();
  }

  ensureStyles();
  const newWrapper = createGitHubButton(adapter);
  if (submitBtn && submitBtn.parentElement === targetParent) {
    targetParent.insertBefore(newWrapper, submitBtn);
  } else {
    targetParent.appendChild(newWrapper);
  }
}

// Initialize platform adapter
function bootstrap() {
  const adapter = new LeetCodeAdapter();
  adapter.initialize((solution) => {
    showSavePrompt(solution);
  });

  // Inject Add to GitHub button
  ensureStyles();
  injectGitHubButton(adapter);

  // Handle SPA navigation
  let currentPathname = window.location.pathname;
  const onLocationCheck = () => {
    if (window.location.pathname !== currentPathname) {
      currentPathname = window.location.pathname;
      injectGitHubButton(adapter);
    }
  };

  window.addEventListener('popstate', onLocationCheck);

  // Observe DOM changes (debounced) to handle React SPA re-renders
  let debounceTimer: number | null = null;
  const navObserver = new MutationObserver(() => {
    onLocationCheck();
    if (debounceTimer) window.clearTimeout(debounceTimer);
    debounceTimer = window.setTimeout(() => {
      injectGitHubButton(adapter);
    }, 400);
  });

  navObserver.observe(document.body, { childList: true, subtree: true });
}

// Start when document is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', bootstrap);
} else {
  bootstrap();
}

