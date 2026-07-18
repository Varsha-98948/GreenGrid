package com.greengrid.repository;

import com.greengrid.entity.Difficulty;
import com.greengrid.entity.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link JpaSpecificationExecutor} backs the multi-field Search module
 * (title/topic/difficulty/language/platform/date), letting callers compose
 * only the filters the user actually provided instead of one derived-query
 * method per combination.
 */
public interface ProblemRepository extends JpaRepository<Problem, UUID>, JpaSpecificationExecutor<Problem> {

    Optional<Problem> findByIdAndUserId(UUID id, UUID userId);

    Page<Problem> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Problem> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndSolvedDate(UUID userId, LocalDate solvedDate);

    @Query("select p.difficulty as difficulty, count(p) as total " +
            "from Problem p where p.user.id = :userId group by p.difficulty")
    List<DifficultyCount> countByDifficultyForUser(@Param("userId") UUID userId);

    @Query("select p.language as language, count(p) as total " +
            "from Problem p where p.user.id = :userId group by p.language order by count(p) desc")
    List<LanguageCount> countByLanguageForUser(@Param("userId") UUID userId);

    @Query("select distinct p.solvedDate from Problem p where p.user.id = :userId order by p.solvedDate desc")
    List<LocalDate> findDistinctSolvedDatesForUser(@Param("userId") UUID userId);

    @Query("select t.name as topicName, count(p) as total from Problem p join p.tags t " +
            "where p.user.id = :userId group by t.name order by count(p) desc")
    List<TopicCount> countByTopicForUser(@Param("userId") UUID userId);

    interface TopicCount {
        String getTopicName();
        long getTotal();
    }

    @Query("select p.solvedDate as day, count(p) as total from Problem p " +
            "where p.user.id = :userId and p.solvedDate >= :since group by p.solvedDate")
    List<DailyCount> countPerDaySince(@Param("userId") UUID userId, @Param("since") LocalDate since);

    interface DifficultyCount {
        Difficulty getDifficulty();
        long getTotal();
    }

    interface LanguageCount {
        String getLanguage();
        long getTotal();
    }

    interface DailyCount {
        LocalDate getDay();
        long getTotal();
    }
}
