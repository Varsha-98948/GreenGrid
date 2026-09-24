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

// Initialize platform adapter
function bootstrap() {
  const adapter = new LeetCodeAdapter();
  adapter.initialize((solution) => {
    showSavePrompt(solution);
  });
}

// Start when document is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', bootstrap);
} else {
  bootstrap();
}
