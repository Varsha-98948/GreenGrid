/**
 * Duplicate detection for accepted submissions.
 * Uses chrome.storage.local to persist processed submission fingerprints
 * so that page refreshes or re-renders don't trigger duplicate prompts.
 */

import { SubmissionFingerprint } from '../types/solution';
import { logger } from './logger';

const STORAGE_KEY = 'gg_processed_submissions';
const MAX_ENTRIES = 200; // Keep at most 200 recent fingerprints
const TTL_MS = 24 * 60 * 60 * 1000; // Expire fingerprints after 24 hours

/**
 * Generate a stable fingerprint for a submission.
 * Uses LeetCode submission ID if available, otherwise falls back
 * to a composite key of platform + slug + timestamp window.
 */
export function createFingerprint(
  platform: string,
  problemSlug: string,
  submissionId?: string,
): string {
  if (submissionId) {
    return `${platform}:${submissionId}`;
  }
  // Without a submission ID, use a 60-second time window to deduplicate
  const timeWindow = Math.floor(Date.now() / 60000);
  return `${platform}:${problemSlug}:${timeWindow}`;
}

/**
 * Check if a submission has already been processed.
 */
export async function isAlreadyProcessed(fingerprint: string): Promise<boolean> {
  try {
    const result = await chrome.storage.local.get(STORAGE_KEY);
    const entries: SubmissionFingerprint[] = result[STORAGE_KEY] || [];
    return entries.some(
      (entry) =>
        createFingerprint(entry.platform, entry.problemSlug, entry.submissionId) === fingerprint &&
        Date.now() - entry.timestamp < TTL_MS,
    );
  } catch (e) {
    logger.warn('Failed to check processed submissions', e);
    return false;
  }
}

/**
 * Mark a submission as processed.
 */
export async function markAsProcessed(entry: SubmissionFingerprint): Promise<void> {
  try {
    const result = await chrome.storage.local.get(STORAGE_KEY);
    let entries: SubmissionFingerprint[] = result[STORAGE_KEY] || [];

    // Remove expired entries
    const now = Date.now();
    entries = entries.filter((e) => now - e.timestamp < TTL_MS);

    // Add new entry
    entries.push(entry);

    // Trim to max size
    if (entries.length > MAX_ENTRIES) {
      entries = entries.slice(-MAX_ENTRIES);
    }

    await chrome.storage.local.set({ [STORAGE_KEY]: entries });
    logger.debug('Marked submission as processed', entry);
  } catch (e) {
    logger.warn('Failed to mark submission as processed', e);
  }
}
