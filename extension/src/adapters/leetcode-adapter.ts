/**
 * LeetCode Platform Adapter
 * Implements PlatformAdapter interface for LeetCode (new and classic SPA UI).
 *
 * Responsibilities:
 * - Detects 'Accepted' submission results dynamically
 * - Extracts problem slug, title, URL, language, and submitted code
 * - Obtains submission ID when available for reliable deduplication
 * - Isolates all LeetCode DOM queries from the rest of the extension
 */

import { PlatformAdapter } from '../types/platform';
import { AcceptedSolution } from '../types/solution';
import { createFingerprint, isAlreadyProcessed, markAsProcessed } from '../utils/dedup';
import { logger } from '../utils/logger';

export class LeetCodeAdapter implements PlatformAdapter {
  readonly platformId = 'leetcode';
  readonly platformName = 'LeetCode';

  private observer: MutationObserver | null = null;
  private clickListener: ((e: MouseEvent) => void) | null = null;
  private isAwaitingSubmission = false;
  private submissionTimeout: number | null = null;
  private checkDebounceTimer: number | null = null;
  private lastAcceptedSolution: AcceptedSolution | null = null;

  public getLastAcceptedSolution(): AcceptedSolution | null {
    return this.lastAcceptedSolution;
  }

  initialize(onAccepted: (solution: AcceptedSolution) => void): () => void {
    logger.info('Initializing LeetCode adapter');

    // 1. Listen for clicks on Submit buttons to arm detection
    this.clickListener = (event: MouseEvent) => {
      const target = event.target as HTMLElement | null;
      if (!target) return;

      const submitBtn = target.closest(
        'button[data-e2e-locator="console-submit-button"], button[data-cy="submit-code-btn"], button:has(svg)'
      );

      const btnText = (submitBtn?.textContent || target.textContent || '').trim().toLowerCase();
      if (btnText === 'submit' || submitBtn?.getAttribute('data-e2e-locator') === 'console-submit-button') {
        logger.info('Submit button clicked, awaiting submission result');
        this.isAwaitingSubmission = true;

        if (this.submissionTimeout) {
          window.clearTimeout(this.submissionTimeout);
        }
        // Keep waiting window open for up to 45 seconds for LeetCode judge
        this.submissionTimeout = window.setTimeout(() => {
          this.isAwaitingSubmission = false;
        }, 45000);
      }
    };

    document.addEventListener('click', this.clickListener, true);

    // 2. Observe DOM mutations for the appearance of "Accepted" status
    this.observer = new MutationObserver(() => {
      if (this.checkDebounceTimer) {
        window.clearTimeout(this.checkDebounceTimer);
      }

      this.checkDebounceTimer = window.setTimeout(() => {
        this.checkForAcceptedSubmission(onAccepted);
      }, 400);
    });

    this.observer.observe(document.body, {
      childList: true,
      subtree: true,
      characterData: true,
    });

    // Also perform an initial check in case page was loaded on an accepted submission URL
    this.checkForAcceptedSubmission(onAccepted);

    // Return cleanup function
    return () => {
      logger.info('Tearing down LeetCode adapter');
      if (this.clickListener) {
        document.removeEventListener('click', this.clickListener, true);
        this.clickListener = null;
      }
      if (this.observer) {
        this.observer.disconnect();
        this.observer = null;
      }
      if (this.submissionTimeout) {
        window.clearTimeout(this.submissionTimeout);
      }
      if (this.checkDebounceTimer) {
        window.clearTimeout(this.checkDebounceTimer);
      }
    };
  }

  /**
   * Check DOM for an accepted submission result
   */
  private async checkForAcceptedSubmission(
    onAccepted: (solution: AcceptedSolution) => void
  ): Promise<void> {
    const slug = this.extractProblemSlug();
    if (!slug) return;

    const acceptedElement = this.findAcceptedElement();
    if (!acceptedElement) return;

    // Retrieve submission ID if available
    const submissionId = this.extractSubmissionId();
    const fingerprint = createFingerprint(this.platformId, slug, submissionId);

    // Check if already processed
    const alreadyProcessed = await isAlreadyProcessed(fingerprint);
    if (alreadyProcessed) {
      return;
    }

    // If we were awaiting submission or if a fresh submission result is explicitly visible
    const isResultPanel = this.isSubmissionResultContainer(acceptedElement);
    if (!this.isAwaitingSubmission && !isResultPanel) {
      return;
    }

    logger.info('Accepted submission detected for problem:', slug);

    // Extract code
    const code = this.extractCode();
    if (!code || code.trim().length === 0) {
      logger.warn('Accepted submission detected but code could not be extracted yet');
      return;
    }

    // Extract language
    const language = this.extractLanguage();

    // Extract problem name
    const problemName = this.extractProblemName(slug);
    const problemUrl = `https://leetcode.com/problems/${slug}/`;

    const solution: AcceptedSolution = {
      platform: 'LeetCode',
      problemName,
      problemUrl,
      problemSlug: slug,
      language,
      code,
      status: 'accepted',
    };

    // Mark as processed immediately so subsequent mutations do not re-trigger
    await markAsProcessed({
      platform: this.platformId,
      problemSlug: slug,
      submissionId,
      timestamp: Date.now(),
    });

    this.isAwaitingSubmission = false;
    if (this.submissionTimeout) {
      window.clearTimeout(this.submissionTimeout);
      this.submissionTimeout = null;
    }

    logger.info('Dispatching accepted solution to callback:', {
      name: problemName,
      language,
      codeLength: code.length,
      submissionId,
    });

    this.lastAcceptedSolution = solution;

    onAccepted(solution);
  }

  /**
   * Extract current accepted solution if available on the page or from memory.
   * Used by the 'Add to GitHub' button.
   */
  public extractCurrentAcceptedSolution(): AcceptedSolution | null {
    const slug = this.extractProblemSlug();
    if (!slug) return null;

    // 1. If we already recorded an accepted solution for this problem, use it
    if (this.lastAcceptedSolution && this.lastAcceptedSolution.problemSlug === slug) {
      return this.lastAcceptedSolution;
    }

    // 2. Check if page currently displays an accepted submission result
    const acceptedElement = this.findAcceptedElement();
    if (!acceptedElement) {
      return null;
    }

    // 3. Extract code
    const code = this.extractCode();
    if (!code || code.trim().length === 0) {
      return null;
    }

    const language = this.extractLanguage();
    const problemName = this.extractProblemName(slug);
    const problemUrl = `https://leetcode.com/problems/${slug}/`;

    const solution: AcceptedSolution = {
      platform: 'LeetCode',
      problemName,
      problemUrl,
      problemSlug: slug,
      language,
      code,
      status: 'accepted',
    };

    this.lastAcceptedSolution = solution;
    return solution;
  }

  /**
   * Extract problem slug from URL
   */
  private extractProblemSlug(): string | null {
    const match = window.location.pathname.match(/\/problems\/([^/]+)/);
    return match ? match[1] : null;
  }

  /**
   * Extract human-readable problem name without problem numbers
   */
  private extractProblemName(slug: string): string {
    // 1. Try DOM title elements
    const titleElem = document.querySelector(
      'div[data-cy="question-title"], .text-title-large, [class*="question-title"], a[href^="/problems/"][class*="title"]'
    );
    if (titleElem) {
      // Prefer inner problem link if present, or title element text
      const link = titleElem.matches('a') ? titleElem : titleElem.querySelector('a[href*="/problems/"]');
      const candidateText = link?.textContent || titleElem.textContent || '';
      const clean = this.cleanProblemTitle(candidateText);
      if (clean.length > 0) return clean;
    }

    // 2. Try document title (e.g. "1. Two Sum - LeetCode")
    const docTitle = document.title;
    if (docTitle) {
      const titleWithoutSite = docTitle.replace(/-?\s*LeetCode.*$/i, '').trim();
      const clean = this.cleanProblemTitle(titleWithoutSite);
      if (clean.length > 0 && clean.toLowerCase() !== 'leetcode') return clean;
    }

    // 3. Fallback: format slug (e.g. 'two-sum' -> 'Two Sum')
    return slug
      .split('-')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  /**
   * Clean problem title by removing leading or trailing problem numbers and NBSP.
   * e.g. "1. Two Sum" -> "Two Sum"
   * e.g. "49. Group Anagrams" -> "Group Anagrams"
   * e.g. "238. Product of Array Except Self" -> "Product of Array Except Self"
   * e.g. "Two Sum 1" -> "Two Sum"
   */
  public cleanProblemTitle(rawTitle: string): string {
    return cleanProblemTitle(rawTitle);
  }

  /**
   * Find DOM element containing the 'Accepted' verdict in the submission result
   */
  private findAcceptedElement(): HTMLElement | null {
    // Specific selectors for LeetCode submission result
    const selectors = [
      '[data-e2e-locator="submission-result"]',
      'span[data-state="accepted"]',
      'div[class*="status-accepted"]',
      'span.text-sd-green-500',
      'span.text-green-60',
      'div[data-headline="Accepted"]',
    ];

    for (const sel of selectors) {
      const el = document.querySelector(sel);
      if (el && el.textContent?.trim().toLowerCase().includes('accepted')) {
        return el as HTMLElement;
      }
    }

    // Look inside submission result areas for green "Accepted" text
    const candidates = document.querySelectorAll(
      'div[class*="result"], div[class*="submission"], div[id*="submission"]'
    );
    for (const candidate of Array.from(candidates)) {
      const text = candidate.textContent || '';
      if (text.includes('Accepted') && (text.includes('Runtime') || text.includes('Memory') || text.includes('ms'))) {
        return candidate as HTMLElement;
      }
    }

    return null;
  }

  /**
   * Verify that element belongs to submission result area, not history list or question header
   */
  private isSubmissionResultContainer(el: HTMLElement): boolean {
    const text = el.innerText || el.textContent || '';
    if (text.includes('Runtime') || text.includes('Memory') || text.includes('Beats') || text.includes('ms')) {
      return true;
    }
    const container = el.closest(
      '[data-e2e-locator="submission-result"], div[class*="result"], div[class*="submission-detail"], div[class*="SubmissionResult"]'
    );
    return container !== null;
  }

  /**
   * Extract submission ID if available from links or URL
   */
  private extractSubmissionId(): string | undefined {
    // Check URL
    const urlMatch = window.location.pathname.match(/\/submissions\/(?:detail\/)?(\d+)/);
    if (urlMatch) return urlMatch[1];

    // Check submission links in the page
    const subLink = document.querySelector('a[href*="/submissions/detail/"]');
    if (subLink) {
      const href = subLink.getAttribute('href') || '';
      const match = href.match(/\/submissions\/detail\/(\d+)/);
      if (match) return match[1];
    }

    return undefined;
  }

  /**
   * Extract programming language from LeetCode editor or submission panel
   */
  private extractLanguage(): string {
    // 1. Check language button/dropdown in editor
    const langBtn = document.querySelector(
      'button[id*="headlessui-listbox-button"], [data-cy="lang-select"] button, button[class*="rounded"]:has(svg)'
    );
    if (langBtn && langBtn.textContent) {
      const text = langBtn.textContent.trim();
      if (this.isValidLanguage(text)) {
        return text;
      }
    }

    // 2. Check data-mode-id in editor
    const monaco = document.querySelector('.monaco-editor[data-mode-id]');
    if (monaco) {
      const mode = monaco.getAttribute('data-mode-id');
      if (mode) return mode;
    }

    // 3. Check submission result language label
    const subLangEl = document.querySelector(
      'div[class*="submission"] [class*="language"], [data-e2e-locator="submission-lang"]'
    );
    if (subLangEl && subLangEl.textContent) {
      const text = subLangEl.textContent.trim();
      if (this.isValidLanguage(text)) return text;
    }

    // 4. Inspect language buttons inside editor toolbar
    const buttons = document.querySelectorAll('button');
    for (const btn of Array.from(buttons)) {
      const text = (btn.textContent || '').trim();
      if (this.isValidLanguage(text)) {
        return text;
      }
    }

    return 'Java'; // Safe fallback
  }

  private isValidLanguage(lang: string): boolean {
    const known = [
      'c++', 'cpp', 'java', 'python', 'python3', 'c', 'c#', 'javascript', 'typescript',
      'php', 'swift', 'kotlin', 'dart', 'go', 'golang', 'ruby', 'scala', 'rust', 'racket',
      'erlang', 'elixir', 'sql', 'mysql', 'postgresql'
    ];
    return known.includes(lang.toLowerCase());
  }

  /**
   * Extract actual submitted code
   */
  private extractCode(): string {
    let rawCode = '';

    // Strategy 1: Submission detail view / code block if open
    const submissionCodeBlock = document.querySelector(
      'div[class*="submission-detail"] pre code, div[class*="submission"] pre, pre[class*="code"]'
    );
    if (submissionCodeBlock && submissionCodeBlock.textContent) {
      const code = submissionCodeBlock.textContent;
      if (code.trim().length > 0) {
        logger.debug('Extracted code from submission pre/code element');
        rawCode = code;
      }
    }

    // Strategy 2: Extract from Monaco Editor lines (.view-lines)
    if (!rawCode) {
      const monacoLines = document.querySelectorAll('.monaco-editor .view-lines .view-line');
      if (monacoLines.length > 0) {
        const lines: string[] = [];
        monacoLines.forEach((line) => {
          // HTMLElement.innerText preserves spacing and empty lines better than textContent
          const text = (line as HTMLElement).innerText ?? line.textContent ?? '';
          lines.push(text);
        });
        const code = lines.join('\n');
        if (code.trim().length > 0) {
          logger.debug('Extracted code from Monaco editor .view-lines', { lineCount: lines.length });
          rawCode = code;
        }
      }
    }

    // Strategy 3: Check Monaco editor inputarea or container
    if (!rawCode) {
      const monacoEditor = document.querySelector('.monaco-editor');
      if (monacoEditor) {
        const text = (monacoEditor as HTMLElement).innerText;
        if (text && text.trim().length > 0) {
          logger.debug('Extracted code from Monaco editor innerText');
          rawCode = text;
        }
      }
    }

    if (!rawCode || rawCode.trim().length === 0) {
      return '';
    }

    // Normalize: Convert all non-breaking spaces (\u00A0) introduced by LeetCode/Monaco DOM into normal ASCII spaces (" "),
    // preserving indentation, newlines, tabs, and multiple spaces.
    return this.normalizeCode(rawCode);
  }

  /**
   * Normalize code extracted from LeetCode DOM.
   * Converts Monaco/DOM non-breaking spaces (\u00A0) into standard ASCII spaces (" "),
   * preserving indentation, newlines, tabs, and multiple spaces.
   */
  public normalizeCode(code: string): string {
    return normalizeCode(code);
  }
}

/**
 * Robust title cleaner that removes leading LeetCode problem numbers (e.g. "1. Two Sum" -> "Two Sum")
 * and trailing numbers (e.g. "Two Sum 1" -> "Two Sum"), preserving valid titles like "3Sum" and "132 Pattern".
 */
export function cleanProblemTitle(rawTitle: string): string {
  if (!rawTitle) return '';
  return rawTitle
    .replace(/\u00A0/g, ' ')
    .replace(/^\s*\d+\.\s*/, '')
    .replace(/\s+\d+$/, '')
    .trim();
}

/**
 * Normalizes code extracted from LeetCode / Monaco Editor DOM.
 * Converts Unicode non-breaking spaces (\u00A0) into standard ASCII spaces (" ")
 * while preserving newlines, tabs, indentation, and multiple spaces.
 * Also strips invisible zero-width spaces (\u200B, \uFEFF) injected by Monaco.
 */
export function normalizeCode(rawCode: string): string {
  if (!rawCode) return '';
  return rawCode
    .replace(/\u00A0/g, ' ')
    .replace(/[\u200B\uFEFF]/g, '');
}
