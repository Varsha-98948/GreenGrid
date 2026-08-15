package com.greengrid.service;

import com.greengrid.dto.problem.CreateProblemRequest;
import com.greengrid.dto.problem.CreateRevisionRequest;
import com.greengrid.dto.problem.ProblemResponse;
import com.greengrid.dto.problem.UpdateProblemRequest;
import com.greengrid.entity.Difficulty;
import com.greengrid.entity.User;
import com.greengrid.repository.ProblemRepository;
import com.greengrid.repository.ProblemRevisionRepository;
import com.greengrid.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
public class ProblemServiceTest {

    @Autowired
    private ProblemService problemService;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private ProblemRevisionRepository problemRevisionRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("revisiontest_" + UUID.randomUUID() + "@greengrid.dev");
        testUser.setDisplayName("Revision Test User");
        testUser = userRepository.save(testUser);
    }

    @Test
    void testProblemCreationAndMultiRevisions() {
        // 1. Create initial problem (Two Sum - Python)
        CreateProblemRequest createReq = new CreateProblemRequest(
                "LeetCode", "Two Sum Test", "https://leetcode.com/problems/two-sum/",
                Difficulty.EASY, List.of("Array", "Hash Table"), "Python",
                "def twoSum(nums, target):\n    return []", "Initial notes",
                "O(n)", "O(n)", LocalDate.now()
        );

        ProblemResponse problemResp = problemService.createProblem(testUser.getId(), createReq);
        UUID problemId = problemResp.id();

        assertNotNull(problemId);
        assertEquals(1, problemResp.revisionCount());
        assertEquals(1, problemResp.revisions().size());
        assertEquals(1, problemResp.revisions().get(0).revisionNumber());
        assertEquals("Initial Solution", problemResp.revisions().get(0).title());
        assertEquals("Python", problemResp.revisions().get(0).language());

        // 2. Add Revision 2 (Java - Two Pointers approach)
        CreateRevisionRequest rev2Req = new CreateRevisionRequest(
                "Two Pointers Approach", "Java",
                "public int[] twoSum(int[] nums, int target) { return new int[0]; }",
                "Optimized approach notes", "O(n log n)", "O(1)"
        );

        ProblemResponse updatedResp = problemService.createRevision(testUser.getId(), problemId, rev2Req);

        // Verify problem ID is UNCHANGED
        assertEquals(problemId, updatedResp.id());
        assertEquals(2, updatedResp.revisionCount());
        assertEquals(2, updatedResp.revisions().size());
        assertEquals(2, updatedResp.revisions().get(1).revisionNumber());
        assertEquals("Two Pointers Approach", updatedResp.revisions().get(1).title());
        assertEquals("Java", updatedResp.revisions().get(1).language());

        // 3. Edit metadata of Problem
        UpdateProblemRequest updateMetaReq = new UpdateProblemRequest(
                "LeetCode", "Two Sum Test (Updated Title)", "https://leetcode.com/problems/two-sum/",
                Difficulty.MEDIUM, List.of("Array", "Hash Table", "Two Pointers"), "Java",
                "public int[] twoSum(int[] nums, int target) { return new int[0]; }",
                "Optimized approach notes", "O(n log n)", "O(1)"
        );

        ProblemResponse metaResp = problemService.updateProblem(testUser.getId(), problemId, updateMetaReq);
        assertEquals(problemId, metaResp.id());
        assertEquals("Two Sum Test (Updated Title)", metaResp.title());
        assertEquals(Difficulty.MEDIUM, metaResp.difficulty());

        // 4. Verify unique problem count on repository remains 1
        long userProblemCount = problemRepository.countByUserId(testUser.getId());
        assertEquals(1, userProblemCount);

        // 5. Verify problem_revisions count for problem is 2
        assertEquals(2, problemRevisionRepository.findByProblemIdOrderByRevisionNumberAsc(problemId).size());
    }
}
