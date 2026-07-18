package com.greengrid.controller;

import com.greengrid.dto.common.ApiResponse;
import com.greengrid.dto.problem.*;
import com.greengrid.github.LeetCodeMetadataService;
import com.greengrid.security.UserPrincipal;
import com.greengrid.service.ProblemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;
    private final LeetCodeMetadataService leetCodeMetadataService;

    public ProblemController(ProblemService problemService, LeetCodeMetadataService leetCodeMetadataService) {
        this.problemService = problemService;
        this.leetCodeMetadataService = leetCodeMetadataService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProblemResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                @Valid @RequestBody CreateProblemRequest request) {
        return ApiResponse.ok("Problem saved", problemService.createProblem(principal.getId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProblemResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody UpdateProblemRequest request) {
        return ApiResponse.ok("Problem updated", problemService.updateProblem(principal.getId(), id, request));
    }

    @PostMapping("/{id}/retry-commit")
    public ApiResponse<ProblemResponse> retryCommit(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable UUID id) {
        return ApiResponse.ok("Retry attempted", problemService.retryCommit(principal.getId(), id));
    }

    @PatchMapping("/{id}/revision")
    public ApiResponse<ProblemResponse> updateRevision(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable UUID id,
                                                        @RequestBody RevisionUpdateRequest request) {
        return ApiResponse.ok(problemService.updateRevisionStatus(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        problemService.deleteProblem(principal.getId(), id);
        return ApiResponse.message("Problem deleted");
    }

    @GetMapping("/{id}")
    public ApiResponse<ProblemResponse> get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.ok(problemService.getProblem(principal.getId(), id));
    }

    @GetMapping
    public ApiResponse<Page<ProblemResponse>> list(@AuthenticationPrincipal UserPrincipal principal,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ApiResponse.ok(problemService.listProblems(principal.getId(), pageable));
    }

    @GetMapping("/search")
    public ApiResponse<Page<ProblemResponse>> search(@AuthenticationPrincipal UserPrincipal principal,
                                                      @RequestParam(required = false) String title,
                                                      @RequestParam(required = false) String topic,
                                                      @RequestParam(required = false) com.greengrid.entity.Difficulty difficulty,
                                                      @RequestParam(required = false) String language,
                                                      @RequestParam(required = false) String platform,
                                                      @RequestParam(required = false)
                                                      @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                                      java.time.LocalDate date,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ApiResponse.ok(problemService.searchProblems(
                principal.getId(), title, topic, difficulty, language, platform, date, pageable));
    }

    @GetMapping("/fetch-metadata")
    public ApiResponse<LeetCodeMetadataResponse> fetchMetadata(@RequestParam String url) {
        return leetCodeMetadataService.fetchMetadata(url)
                .map(m -> ApiResponse.ok(new LeetCodeMetadataResponse(true, m.title(), m.difficulty(), m.topics())))
                .orElseGet(() -> ApiResponse.ok(new LeetCodeMetadataResponse(false, null, null, List.of())));
    }
}
