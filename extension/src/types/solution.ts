/**
 * Represents an accepted solution detected by a platform adapter.
 * This is the normalized data structure shared between content scripts,
 * background service worker, and the backend API call.
 */
export interface AcceptedSolution {
  platform: string;
  problemName: string;
  problemUrl: string;
  problemSlug?: string;
  language: string;
  code: string;
  status: 'accepted';
}

/**
 * Submission identifier used for duplicate detection.
 * Prevents the same accepted submission from triggering multiple prompts.
 */
export interface SubmissionFingerprint {
  platform: string;
  problemSlug: string;
  /** LeetCode submission ID if available, otherwise a composite key */
  submissionId?: string;
  timestamp: number;
}
