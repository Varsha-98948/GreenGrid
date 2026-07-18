package com.greengrid.service;

import com.greengrid.dto.problem.CreateProblemRequest;
import com.greengrid.dto.problem.ProblemResponse;
import com.greengrid.dto.problem.RevisionUpdateRequest;
import com.greengrid.dto.problem.UpdateProblemRequest;
import com.greengrid.entity.*;
import com.greengrid.exception.GitHubIntegrationException;
import com.greengrid.exception.ResourceNotFoundException;
import com.greengrid.github.CommitService;
import com.greengrid.repository.ProblemRepository;
import com.greengrid.repository.TagRepository;
import com.greengrid.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final RepositoryService repositoryService;
    private final CommitService commitService;

    public ProblemService(ProblemRepository problemRepository, TagRepository tagRepository,
                           UserRepository userRepository, RepositoryService repositoryService,
                           CommitService commitService) {
        this.problemRepository = problemRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.repositoryService = repositoryService;
        this.commitService = commitService;
    }

    @Transactional
    public ProblemResponse createProblem(UUID userId, CreateProblemRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Problem problem = new Problem();
        problem.setUser(user);
        problem.setPlatform(request.platform());
        problem.setTitle(request.title());
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

        problem = problemRepository.save(problem);

        pushToGitHub(userId, problem);

        return toResponse(problemRepository.save(problem));
    }

    @Transactional
    public ProblemResponse updateProblem(UUID userId, UUID problemId, UpdateProblemRequest request) {
        Problem problem = getOwned(userId, problemId);
        User user = problem.getUser();

        problem.setPlatform(request.platform());
        problem.setTitle(request.title());
        problem.setProblemUrl(request.problemUrl());
        problem.setDifficulty(request.difficulty());
        problem.setLanguage(request.language());
        problem.setCode(request.code());
        problem.setNotes(request.notes());
        problem.setTimeComplexity(request.timeComplexity());
        problem.setSpaceComplexity(request.spaceComplexity());
        problem.setTags(resolveTags(user, request.topics()));

        pushToGitHub(userId, problem);

        return toResponse(problemRepository.save(problem));
    }

    @Transactional
    public ProblemResponse retryCommit(UUID userId, UUID problemId) {
        Problem problem = getOwned(userId, problemId);
        pushToGitHub(userId, problem);
        return toResponse(problemRepository.save(problem));
    }

    private void pushToGitHub(UUID userId, Problem problem) {
        try {
            GitRepository repository = repositoryService.getActiveRepository(userId);
            String sha = commitService.commitProblem(userId, repository, problem);
            problem.setLastCommitSha(sha);
            problem.setCommitStatus("COMMITTED");
        } catch (GitHubIntegrationException | ResourceNotFoundException ex) {
            log.warn("GitHub push failed for problem {}: {}", problem.getId(), ex.getMessage());
            problem.setCommitStatus("FAILED");
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
    public Page<ProblemResponse> searchProblems(UUID userId, String title, String topic, Difficulty difficulty,
                                                 String language, String platform, LocalDate date,
                                                 Pageable pageable) {
        var spec = org.springframework.data.jpa.domain.Specification.where(
                com.greengrid.repository.ProblemSpecifications.belongsToUser(userId));

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
        return new ProblemResponse(
                p.getId(), p.getPlatform(), p.getTitle(), p.getProblemUrl(), p.getDifficulty(),
                p.getTags().stream().map(Tag::getName).sorted().toList(),
                p.getLanguage(), p.getCode(), p.getNotes(), p.getTimeComplexity(), p.getSpaceComplexity(),
                p.getSolvedDate(), p.getRevisionStatus(), p.isFavorite(), p.getRepoFolderPath(),
                p.getLastCommitSha(), p.getCommitStatus(), p.getCreatedAt()
        );
    }
}
