package com.greengrid.util;

import java.time.LocalDate;
import java.util.List;

/**
 * Computes the current daily-solve streak from a descending list of
 * distinct dates a user solved at least one problem. A streak counts as
 * "current" if the most recent solve was today or yesterday (so a user
 * doesn't lose their streak just because it's 11pm and they haven't
 * solved today's problem yet) and then walks backward through
 * consecutive calendar days.
 */
public final class StreakCalculator {

    private StreakCalculator() {}

    public static int calculateCurrentStreak(List<LocalDate> distinctSolvedDatesDesc) {
        if (distinctSolvedDatesDesc == null || distinctSolvedDatesDesc.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate mostRecent = distinctSolvedDatesDesc.get(0);

        if (mostRecent.isBefore(today.minusDays(1))) {
            return 0; // streak already broken
        }

        int streak = 1;
        LocalDate expectedPrevious = mostRecent.minusDays(1);

        for (int i = 1; i < distinctSolvedDatesDesc.size(); i++) {
            LocalDate current = distinctSolvedDatesDesc.get(i);
            if (current.isEqual(expectedPrevious)) {
                streak++;
                expectedPrevious = current.minusDays(1);
            } else if (current.isBefore(expectedPrevious)) {
                break;
            }
        }
        return streak;
    }

    public static int calculateLongestStreak(List<LocalDate> distinctSolvedDatesDesc) {
        if (distinctSolvedDatesDesc == null || distinctSolvedDatesDesc.isEmpty()) {
            return 0;
        }
        List<LocalDate> sortedAsc = distinctSolvedDatesDesc.stream().sorted().toList();

        int longest = 1;
        int current = 1;
        for (int i = 1; i < sortedAsc.size(); i++) {
            if (sortedAsc.get(i).equals(sortedAsc.get(i - 1).plusDays(1))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }
}
