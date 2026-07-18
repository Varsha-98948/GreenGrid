package com.greengrid.dto.dashboard;

import java.util.List;
import java.util.Map;

public record DashboardResponse(
        int todaysProgress,
        int currentStreak,
        int longestStreak,
        long totalProblemsSolved,
        List<RecentCommit> recentCommits,
        Map<String, Long> difficultyBreakdown,
        Map<String, Long> topicBreakdown,
        Map<String, Long> languageUsage,
        List<ContributionDay> contributionCalendar,
        RepoStatusSummary repositoryStatus
) {
    public record RecentCommit(String title, String difficulty, String commitStatus,
                                String commitSha, String solvedDate) {}

    public record ContributionDay(String date, long count) {}

    public record RepoStatusSummary(boolean connected, String fullName, String htmlUrl, String lastSyncStatus) {}
}
