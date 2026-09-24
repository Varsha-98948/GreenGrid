/**
 * Lightweight logging utility.
 * All log messages are prefixed with [GreenGrid] for easy filtering.
 * Set DEBUG = false for production to suppress verbose logs.
 */

const DEBUG = true;
const PREFIX = '[GreenGrid]';

export const logger = {
  info: (...args: unknown[]) => {
    if (DEBUG) console.log(PREFIX, ...args);
  },

  warn: (...args: unknown[]) => {
    console.warn(PREFIX, ...args);
  },

  error: (...args: unknown[]) => {
    console.error(PREFIX, ...args);
  },

  debug: (...args: unknown[]) => {
    if (DEBUG) console.debug(PREFIX, ...args);
  },
};
