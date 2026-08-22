package com.greengrid.dto.friend;

import com.greengrid.dto.dashboard.DashboardResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FriendProgressResponse(
        UUID userId,
        String displayName,
        String avatarUrl,
        String githubUsername,
        long totalProblems,
        long masteredCount,
        long needsRevisionCount,
        long notSetCount,
        long starredCount,
        int currentStreak,
        double masteryPercentage,
        Map<String, Long> difficultyBreakdown,
        Map<String, Long> languageBreakdown,
        List<DashboardResponse.ContributionDay> contributionCalendar
) {
}
