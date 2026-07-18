package com.greengrid.service;

import com.greengrid.dto.dashboard.DashboardResponse;
import com.greengrid.entity.GitRepository;
import com.greengrid.entity.Problem;
import com.greengrid.repository.GitRepositoryRepository;
import com.greengrid.repository.ProblemRepository;
import com.greengrid.util.StreakCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ProblemRepository problemRepository;
    private final GitRepositoryRepository gitRepositoryRepository;

    public DashboardService(ProblemRepository problemRepository, GitRepositoryRepository gitRepositoryRepository) {
        this.problemRepository = problemRepository;
        this.gitRepositoryRepository = gitRepositoryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse buildDashboard(UUID userId) {
        LocalDate today = LocalDate.now();

        List<LocalDate> solvedDatesDesc = problemRepository.findDistinctSolvedDatesForUser(userId);
        int currentStreak = StreakCalculator.calculateCurrentStreak(solvedDatesDesc);
        int longestStreak = StreakCalculator.calculateLongestStreak(solvedDatesDesc);

        long todaysProgress = problemRepository.countByUserIdAndSolvedDate(userId, today);
        long totalSolved = problemRepository.countByUserId(userId);

        List<DashboardResponse.RecentCommit> recentCommits = problemRepository
                .findTop10ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toRecentCommit)
                .toList();

        Map<String, Long> difficultyBreakdown = problemRepository.countByDifficultyForUser(userId).stream()
                .collect(Collectors.toMap(
                        row -> row.getDifficulty().name(), row -> row.getTotal(),
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, Long> topicBreakdown = problemRepository.countByTopicForUser(userId).stream()
                .collect(Collectors.toMap(
                        row -> row.getTopicName(), row -> row.getTotal(),
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, Long> languageUsage = problemRepository.countByLanguageForUser(userId).stream()
                .collect(Collectors.toMap(
                        row -> row.getLanguage(), row -> row.getTotal(),
                        (a, b) -> a, LinkedHashMap::new));

        List<DashboardResponse.ContributionDay> calendar = buildContributionCalendar(userId);

        DashboardResponse.RepoStatusSummary repoStatus = gitRepositoryRepository.findByUserId(userId)
                .map(this::toRepoStatus)
                .orElse(new DashboardResponse.RepoStatusSummary(false, null, null, null));

        return new DashboardResponse(
                (int) todaysProgress, currentStreak, longestStreak, totalSolved,
                recentCommits, difficultyBreakdown, topicBreakdown, languageUsage,
                calendar, repoStatus
        );
    }

    private List<DashboardResponse.ContributionDay> buildContributionCalendar(UUID userId) {
        LocalDate since = LocalDate.now().minusDays(364);
        Map<LocalDate, Long> counts = problemRepository.countPerDaySince(userId, since).stream()
                .collect(Collectors.toMap(ProblemRepository.DailyCount::getDay, ProblemRepository.DailyCount::getTotal));

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        return since.datesUntil(LocalDate.now().plusDays(1))
                .map(day -> new DashboardResponse.ContributionDay(day.format(fmt), counts.getOrDefault(day, 0L)))
                .toList();
    }

    private DashboardResponse.RecentCommit toRecentCommit(Problem p) {
        return new DashboardResponse.RecentCommit(
                p.getTitle(), p.getDifficulty().name(), p.getCommitStatus(),
                p.getLastCommitSha(), p.getSolvedDate().toString());
    }

    private DashboardResponse.RepoStatusSummary toRepoStatus(GitRepository repo) {
        return new DashboardResponse.RepoStatusSummary(true, repo.getFullName(), repo.getHtmlUrl(), repo.getLastSyncStatus());
    }
}
