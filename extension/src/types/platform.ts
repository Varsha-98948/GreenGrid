import { AcceptedSolution } from './solution';

/**
 * Interface that every coding platform adapter must implement.
 * Designed to allow adding new platforms (e.g., HackerRank, Codeforces)
 * without rewriting the extension core.
 */
export interface PlatformAdapter {
  /** Unique identifier for this platform (e.g., 'leetcode') */
  readonly platformId: string;

  /** Human-readable name (e.g., 'LeetCode') */
  readonly platformName: string;

  /**
   * Initialize the adapter on the current page.
   * Sets up DOM observers, mutation observers, etc.
   * Returns a cleanup function to tear down all listeners.
   */
  initialize(onAccepted: (solution: AcceptedSolution) => void): () => void;
}
