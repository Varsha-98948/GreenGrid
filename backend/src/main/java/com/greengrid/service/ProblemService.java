package com.greengrid.service;

import com.greengrid.dto.problem.*;
import com.greengrid.entity.*;
import com.greengrid.exception.GitHubIntegrationException;
import com.greengrid.exception.ResourceNotFoundException;
import com.greengrid.github.CommitService;
import com.greengrid.repository.ProblemRepository;
import com.greengrid.repository.ProblemRevisionRepository;
import com.greengrid.repository.TagRepository;
import com.greengrid.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The core "Save Problem" workflow. Deliberately separates the database
 * commit from the GitHub commit: a Problem is always persisted first
 * (never lose the user's work because GitHub was briefly unreachable),
 * then the GitHub push is attempted and its outcome recorded on
 * {@code commitStatus} — PENDING / COMMITTED / FAILED — so a failed push
 * can be retried later without re-entering the solution.
 */
@Service
public class ProblemService {

    private static final Logger log = LoggerFactory.getLogger(ProblemService.class);

    private final ProblemRepository problemRepository;
    private final ProblemRevisionRepository problemRevisionRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final RepositoryService repositoryService;
    private final CommitService commitService;

    public ProblemService(ProblemRepository problemRepository,
                           ProblemRevisionRepository problemRevisionRepository,
                           TagRepository tagRepository,
                           UserRepository userRepository,
                           RepositoryService repositoryService,
                           CommitService commitService) {
        this.problemRepository = problemRepository;
        this.problemRevisionRepository = problemRevisionRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.repositoryService = repositoryService;
        this.commitService = commitService;
    }

    @Transactional
    public ProblemResponse createProblem(UUID userId, CreateProblemRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String normalizedTitle = normalizeTitle(request.title());
        var duplicateOpt = problemRepository.findDuplicateForUser(userId, normalizedTitle);
        if (duplicateOpt.isPresent()) {
            throw new com.greengrid.exception.DuplicateProblemException(
                    "A problem with title '" + request.title().trim() + "' already exists.",
                    duplicateOpt.get().getId());
        }

        Problem problem = new Problem();
        problem.setUser(user);
        problem.setPlatform(request.platform());
        problem.setTitle(request.title().trim());
        problem.setProblemUrl(request.problemUrl());
        problem.setDifficulty(request.difficulty());
        problem.setLanguage(request.language());
        problem.setCode(request.code());
        problem.setNotes(request.notes());
        problem.setTimeComplexity(request.timeComplexity());
        problem.setSpaceComplexity(request.spaceComplexity());
        problem.setSolvedDate(request.solvedDate() != null ? request.solvedDate() : LocalDate.now());
        problem.setTags(resolveTags(user, request.topics()));
        problem.setCommitStatus("PENDING");

        try {
            problem = problemRepository.save(problem);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            if (!isNormalizedTitleConstraintViolation(ex)) {
                throw ex;
            }
            var existing = problemRepository.findDuplicateForUser(userId, normalizedTitle);
            if (existing.isPresent()) {
                throw new com.greengrid.exception.DuplicateProblemException(
                        "A problem with title '" + request.title().trim() + "' already exists.",
                        existing.get().getId());
            }
            throw ex;
        }

        // Create Revision 1
        ProblemRevision rev1 = new ProblemRevision();
        rev1.setProblem(problem);
        rev1.setRevisionNumber(1);
        rev1.setTitle("Initial Solution");
        rev1.setLanguage(request.language());
        rev1.setCode(request.code());
        rev1.setNotes(request.notes());
        rev1.setTimeComplexity(request.timeComplexity());
        rev1.setSpaceComplexity(request.spaceComplexity());
        rev1.setCommitStatus("PENDING");

        rev1 = problemRevisionRepository.save(rev1);

        pushToGitHub(userId, problem, rev1);

        return toResponse(problemRepository.save(problem));
    }

    @Transactional
    public ProblemResponse updateProblem(UUID userId, UUID problemId, UpdateProblemRequest request) {
        Problem problem = getOwned(userId, problemId);
        User user = problem.getUser();

        if (request.title() != null && !request.title().isBlank()) {
            String normalizedTitle = normalizeTitle(request.title());
            var duplicateOpt = problemRepository.findDuplicateForUserExcludingId(userId, normalizedTitle, problemId);
            if (duplicateOpt.isPresent()) {
                throw new com.greengrid.exception.DuplicateProblemException(
                        "A problem with title '" + request.title().trim() + "' already exists.",
                        duplicateOpt.get().getId());
            }
            problem.setTitle(request.title().trim());
        }

        problem.setPlatform(request.platform());
        problem.setProblemUrl(request.problemUrl());
        problem.setDifficulty(request.difficulty());
        if (request.language() != null && !request.language().isBlank()) {
            problem.setLanguage(request.language());
        }
        if (request.code() != null && !request.code().isBlank()) {
            problem.setCode(request.code());
        }
        problem.setNotes(request.notes());
        problem.setTimeComplexity(request.timeComplexity());
        problem.setSpaceComplexity(request.spaceComplexity());
        problem.setTags(resolveTags(user, request.topics()));

        List<ProblemRevision> revisions = problemRevisionRepository.findByProblemIdOrderByRevisionNumberAsc(problemId);
        ProblemRevision latestRev = revisions.isEmpty() ? null : revisions.get(revisions.size() - 1);
        if (latestRev != null && request.code() != null && !request.code().isBlank()) {
            latestRev.setLanguage(request.language());
            latestRev.setCode(request.code());
            latestRev.setNotes(request.notes());
            latestRev.setTimeComplexity(request.timeComplexity());
            latestRev.setSpaceComplexity(request.spaceComplexity());
            problemRevisionRepository.save(latestRev);
        }

        pushToGitHub(userId, problem, latestRev);

        try {
            return toResponse(problemRepository.save(problem));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            if (!isNormalizedTitleConstraintViolation(ex)) {
                throw ex;
            }
            if (request.title() != null && !request.title().isBlank()) {
                String normalizedTitle = normalizeTitle(request.title());
                var existing = problemRepository.findDuplicateForUserExcludingId(userId, normalizedTitle, problemId);
                if (existing.isPresent()) {
                    throw new com.greengrid.exception.DuplicateProblemException(
                            "A problem with title '" + request.title().trim() + "' already exists.",
                            existing.get().getId());
                }
            }
            throw ex;
        }
    }

    @Transactional
    public ProblemResponse createRevision(UUID userId, UUID problemId, CreateRevisionRequest request) {
        Problem problem = getOwned(userId, problemId);
        User user = problem.getUser();

        // Update problem metadata if provided in request
        if (request.platform() != null && !request.platform().isBlank()) {
            problem.setPlatform(request.platform());
        }
        if (request.problemTitle() != null && !request.problemTitle().isBlank()) {
            problem.setTitle(request.problemTitle().trim());
        }
        if (request.problemUrl() != null) {
            problem.setProblemUrl(request.problemUrl().trim());
        }
        if (request.difficulty() != null) {
            problem.setDifficulty(request.difficulty());
        }
        if (request.topics() != null) {
            problem.setTags(resolveTags(user, request.topics()));
        }
        if (request.solvedDate() != null) {
            problem.setSolvedDate(request.solvedDate());
        }

        List<ProblemRevision> existingRevs = problemRevisionRepository.findByProblemIdOrderByRevisionNumberAsc(problemId);
        int nextRevNum = existingRevs.stream().mapToInt(ProblemRevision::getRevisionNumber).max().orElse(0) + 1;

        ProblemRevision revision = new ProblemRevision();
        revision.setProblem(problem);
        revision.setRevisionNumber(nextRevNum);
        revision.setTitle(request.title() != null && !request.title().isBlank()
                ? request.title().trim()
                : "Revision " + nextRevNum);
        revision.setLanguage(request.language());
        revision.setCode(request.code());
        revision.setNotes(request.notes());
        revision.setTimeComplexity(request.timeComplexity());
        revision.setSpaceComplexity(request.spaceComplexity());
        revision.setCommitStatus("PENDING");

        revision = problemRevisionRepository.save(revision);

        problem.setLanguage(request.language());
        problem.setCode(request.code());
        problem.setNotes(request.notes());
        problem.setTimeComplexity(request.timeComplexity());
        problem.setSpaceComplexity(request.spaceComplexity());

        pushToGitHub(userId, problem, revision);

        return toResponse(problemRepository.save(problem));
    }

    @Transactional
    public ProblemResponse updateRevision(UUID userId, UUID problemId, UUID revisionId, CreateRevisionRequest request) {
        Problem problem = getOwned(userId, problemId);
        User user = problem.getUser();

        // Update problem metadata if provided in request
        if (request.platform() != null && !request.platform().isBlank()) {
            problem.setPlatform(request.platform());
        }
        if (request.problemTitle() != null && !request.problemTitle().isBlank()) {
            problem.setTitle(request.problemTitle().trim());
        }
        if (request.problemUrl() != null) {
            problem.setProblemUrl(request.problemUrl().trim());
        }
        if (request.difficulty() != null) {
            problem.setDifficulty(request.difficulty());
        }
        if (request.topics() != null) {
            problem.setTags(resolveTags(user, request.topics()));
        }
        if (request.solvedDate() != null) {
            problem.setSolvedDate(request.solvedDate());
        }

        ProblemRevision revision = problemRevisionRepository.findByIdAndProblemId(revisionId, problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Revision not found"));

        if (request.title() != null && !request.title().isBlank()) {
            revision.setTitle(request.title().trim());
        }
        revision.setLanguage(request.language());
        revision.setCode(request.code());
        revision.setNotes(request.notes());
        revision.setTimeComplexity(request.timeComplexity());
        revision.setSpaceComplexity(request.spaceComplexity());

        revision = problemRevisionRepository.save(revision);

        List<ProblemRevision> existing = problemRevisionRepository.findByProblemIdOrderByRevisionNumberAsc(problemId);
        if (!existing.isEmpty() && existing.get(existing.size() - 1).getId().equals(revisionId)) {
            problem.setLanguage(request.language());
            problem.setCode(request.code());
            problem.setNotes(request.notes());
            problem.setTimeComplexity(request.timeComplexity());
            problem.setSpaceComplexity(request.spaceComplexity());
        }

        pushToGitHub(userId, problem, revision);

        return toResponse(problemRepository.save(problem));
    }


    @Transactional
    public ProblemResponse retryCommit(UUID userId, UUID problemId) {
        Problem problem = getOwned(userId, problemId);
        List<ProblemRevision> revisions = problemRevisionRepository.findByProblemIdOrderByRevisionNumberAsc(problemId);
        ProblemRevision latest = revisions.isEmpty() ? null : revisions.get(revisions.size() - 1);
        pushToGitHub(userId, problem, latest);
        return toResponse(problemRepository.save(problem));
    }

    private void pushToGitHub(UUID userId, Problem problem, ProblemRevision revision) {
        var optRepo = repositoryService.findActiveRepository(userId);
        if (optRepo.isEmpty()) {
            log.warn("GitHub push skipped for problem {}: No repository selected yet", problem.getId());
            problem.setCommitStatus("FAILED");
            if (revision != null) {
                revision.setCommitStatus("FAILED");
                problemRevisionRepository.save(revision);
            }
            return;
        }
        try {
            GitRepository repository = optRepo.get();
            String sha = commitService.commitProblem(userId, repository, problem);
            problem.setLastCommitSha(sha);
            problem.setCommitStatus("COMMITTED");
            if (revision != null) {
                revision.setLastCommitSha(sha);
                revision.setRepoFolderPath(problem.getRepoFolderPath());
                revision.setCommitStatus("COMMITTED");
                problemRevisionRepository.save(revision);
            }
        } catch (GitHubIntegrationException | ResourceNotFoundException ex) {
            log.warn("GitHub push failed for problem {}: {}", problem.getId(), ex.getMessage());
            problem.setCommitStatus("FAILED");
            if (revision != null) {
                revision.setCommitStatus("FAILED");
                problemRevisionRepository.save(revision);
            }
        }
    }

    @Transactional
    public ProblemResponse updateRevisionStatus(UUID userId, UUID problemId, RevisionUpdateRequest request) {
        Problem problem = getOwned(userId, problemId);
        if (request.revisionStatus() != null) {
            problem.setRevisionStatus(request.revisionStatus());
        }
        if (request.favorite() != null) {
            problem.setFavorite(request.favorite());
        }
        return toResponse(problemRepository.save(problem));
    }

    @Transactional
    public void deleteProblem(UUID userId, UUID problemId) {
        Problem problem = getOwned(userId, problemId);
        problemRepository.delete(problem);
    }

    @Transactional(readOnly = true)
    public ProblemResponse getProblem(UUID userId, UUID problemId) {
        return toResponse(getOwned(userId, problemId));
    }

    @Transactional(readOnly = true)
    public Page<ProblemResponse> listProblems(UUID userId, Pageable pageable) {
        return problemRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProblemResponse> searchProblems(UUID userId, String search, String title, String topic, Difficulty difficulty,
                                                 String language, String platform, Boolean favorite,
                                                 RevisionStatus revisionStatus, LocalDate date,
                                                 Pageable pageable) {
        var spec = org.springframework.data.jpa.domain.Specification.where(
                com.greengrid.repository.ProblemSpecifications.belongsToUser(userId));

        if (search != null && !search.isBlank()) {
            spec = spec.and(com.greengrid.repository.ProblemSpecifications.textSearch(search));
        }
        if (title != null && !title.isBlank()) {
            spec = spec.and(com.greengrid.repository.ProblemSpecifications.titleContains(title));
        }
        if (topic != null && !topic.isBlank()) {
            spec = spec.and(com.greengrid.repository.ProblemSpecifications.hasTopic(topic));
        }
        if (difficulty != null) {
            spec = spec.and(com.greengrid.repository.ProblemSpecifications.hasDifficulty(difficulty));
        }
        if (language != null && !language.isBlank()) {
            spec = spec.and(com.greengrid.repository.ProblemSpecifications.hasLanguage(language));
        }
        if (platform != null && !platform.isBlank()) {
            spec = spec.and(com.greengrid.repository.ProblemSpecifications.hasPlatform(platform));
        }
        if (favorite != null) {
            spec = spec.and(com.greengrid.repository.ProblemSpecifications.isFavorite(favorite));
        }
        if (revisionStatus != null) {
            spec = spec.and(com.greengrid.repository.ProblemSpecifications.hasRevisionStatus(revisionStatus));
        }
        if (date != null) {
            spec = spec.and(com.greengrid.repository.ProblemSpecifications.solvedOn(date));
        }

        return problemRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private Problem getOwned(UUID userId, UUID problemId) {
        return problemRepository.findByIdAndUserId(problemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));
    }

    private Set<Tag> resolveTags(User user, List<String> topicNames) {
        Set<Tag> tags = new HashSet<>();
        if (topicNames == null) return tags;

        for (String rawName : topicNames) {
            String name = rawName.trim();
            if (name.isEmpty()) continue;

            Tag tag = tagRepository.findByUserIdAndNameIgnoreCase(user.getId(), name)
                    .orElseGet(() -> tagRepository.save(new Tag(user, name)));
            tags.add(tag);
        }
        return tags;
    }

    public ProblemResponse toResponse(Problem p) {
        List<ProblemRevision> revs = problemRevisionRepository.findByProblemIdOrderByRevisionNumberAsc(p.getId());
        List<ProblemRevisionResponse> revResponses = revs.stream().map(this::toRevisionResponse).toList();
        int count = revResponses.isEmpty() ? 1 : revResponses.size();

        return new ProblemResponse(
                p.getId(), p.getPlatform(), p.getTitle(), p.getProblemUrl(), p.getDifficulty(),
                p.getTags().stream().map(Tag::getName).sorted().toList(),
                p.getLanguage(), p.getCode(), p.getNotes(), p.getTimeComplexity(), p.getSpaceComplexity(),
                p.getSolvedDate(), p.getRevisionStatus(), p.isFavorite(), p.getRepoFolderPath(),
                p.getLastCommitSha(), p.getCommitStatus(), p.getCreatedAt(),
                count, revResponses
        );
    }

    private ProblemRevisionResponse toRevisionResponse(ProblemRevision r) {
        return new ProblemRevisionResponse(
                r.getId(),
                r.getRevisionNumber(),
                r.getTitle(),
                r.getLanguage(),
                r.getCode(),
                r.getNotes(),
                r.getTimeComplexity(),
                r.getSpaceComplexity(),
                r.getRepoFolderPath(),
                r.getLastCommitSha(),
                r.getCommitStatus(),
                r.getCreatedAt() != null ? r.getCreatedAt().atOffset(ZoneOffset.UTC) : null
        );
    }

    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private boolean isNormalizedTitleConstraintViolation(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && "uk_problems_user_normalized_title".equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
