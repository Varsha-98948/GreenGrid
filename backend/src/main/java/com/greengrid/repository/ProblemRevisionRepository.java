package com.greengrid.repository;

import com.greengrid.entity.ProblemRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProblemRevisionRepository extends JpaRepository<ProblemRevision, UUID> {

    List<ProblemRevision> findByProblemIdOrderByRevisionNumberAsc(UUID problemId);

    Optional<ProblemRevision> findByIdAndProblemId(UUID id, UUID problemId);

    Optional<ProblemRevision> findByProblemIdAndRevisionNumber(UUID problemId, int revisionNumber);

    Optional<ProblemRevision> findFirstByProblemIdOrderByRevisionNumberDesc(UUID problemId);
}
