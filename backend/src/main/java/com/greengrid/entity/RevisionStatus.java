package com.greengrid.entity;

/**
 * Tracks a user's self-assessed mastery of a solved problem.
 * Deliberately excludes "Favorite" — favoriting is an independent
 * boolean flag on Problem, since a problem can be both mastered
 * and favorited at the same time (these are not mutually exclusive states).
 */
public enum RevisionStatus {
    NEEDS_REVISION,
    MASTERED,
    NONE
}
