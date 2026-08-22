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

    @Test
    void testDeleteProblemWithRevisions() {
        CreateProblemRequest createReq = new CreateProblemRequest(
                "LeetCode", "Problem To Delete", "https://leetcode.com/problems/delete-me/",
                Difficulty.EASY, List.of("Tree"), "Java",
                "class Solution {}", "Notes",
                "O(n)", "O(n)", LocalDate.now()
        );

        ProblemResponse problemResp = problemService.createProblem(testUser.getId(), createReq);
        UUID problemId = problemResp.id();

        // Add a second revision
        CreateRevisionRequest rev2Req = new CreateRevisionRequest(
                "Revision 2", "Java", "class SolutionV2 {}", "Notes 2", "O(n)", "O(1)"
        );
        problemService.createRevision(testUser.getId(), problemId, rev2Req);

        // Verify problem and revisions exist
        assertTrue(problemRepository.findById(problemId).isPresent());
        assertEquals(2, problemRevisionRepository.findByProblemIdOrderByRevisionNumberAsc(problemId).size());

        // Perform delete
        problemService.deleteProblem(testUser.getId(), problemId);

        // Verify problem and revisions are deleted
        assertTrue(problemRepository.findById(problemId).isEmpty());
        assertTrue(problemRevisionRepository.findByProblemIdOrderByRevisionNumberAsc(problemId).isEmpty());
    }

    @Test
    void testSearchWithFiltersAndOrdering() {
        CreateProblemRequest req1 = new CreateProblemRequest(
                "LeetCode", "Binary Tree Inorder Traversal", "https://leetcode.com/problems/inorder/",
                Difficulty.EASY, List.of("Tree", "Binary Tree"), "Java",
                "class Solution {}", "Notes 1", "O(n)", "O(n)", LocalDate.now()
        );
        ProblemResponse p1 = problemService.createProblem(testUser.getId(), req1);

        CreateProblemRequest req2 = new CreateProblemRequest(
                "LeetCode", "Binary Tree Maximum Path Sum", "https://leetcode.com/problems/max-path/",
                Difficulty.HARD, List.of("Tree", "Binary Tree", "DP"), "Python",
                "def maxPathSum(root): pass", "Notes 2", "O(n)", "O(h)", LocalDate.now()
        );
        ProblemResponse p2 = problemService.createProblem(testUser.getId(), req2);

        // Set favorite & revision status
        problemService.updateRevisionStatus(testUser.getId(), p2.id(),
                new com.greengrid.dto.problem.RevisionUpdateRequest(com.greengrid.entity.RevisionStatus.NEEDS_REVISION, true));

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("createdAt")));

        // Search text = "Binary Tree", favorite = true, revisionStatus = NEEDS_REVISION
        var searchPage = problemService.searchProblems(
                testUser.getId(), "Binary Tree", null, null, null, null, null, true,
                com.greengrid.entity.RevisionStatus.NEEDS_REVISION, null, pageable);

        assertEquals(1, searchPage.getTotalElements());
        assertEquals(p2.id(), searchPage.getContent().get(0).id());
        assertTrue(searchPage.getContent().get(0).favorite());
        assertEquals(com.greengrid.entity.RevisionStatus.NEEDS_REVISION, searchPage.getContent().get(0).revisionStatus());
    }

    @Test
    void testLargeDatasetProductionScenario65PlusProblems() {
        // 1. Populate existing production-like database with 65 problems
        java.util.List<UUID> createdIds = new java.util.ArrayList<>();
        for (int i = 1; i <= 65; i++) {
            Difficulty diff = (i % 3 == 0) ? Difficulty.HARD : (i % 2 == 0 ? Difficulty.MEDIUM : Difficulty.EASY);
            List<String> topics = (i % 5 == 0) ? List.of("Binary Tree", "DP") : List.of("Array");
            String title = "Production Problem " + i + (i % 5 == 0 ? " Binary Tree" : "");

            CreateProblemRequest req = new CreateProblemRequest(
                    "LeetCode", title, "https://leetcode.com/problems/prob-" + i,
                    diff, topics, "Java",
                    "class Solution" + i + " {}", "Notes " + i,
                    "O(n)", "O(1)", LocalDate.now().minusDays(65 - i)
            );
            ProblemResponse resp = problemService.createProblem(testUser.getId(), req);
            createdIds.add(resp.id());

            // Add extra revisions to odd numbered problems
            if (i % 2 != 0) {
                CreateRevisionRequest revReq = new CreateRevisionRequest(
                        "Revision 2 for Prob " + i, "Python",
                        "def solve" + i + "(): pass", "Python notes " + i,
                        "O(n)", "O(n)"
                );
                problemService.createRevision(testUser.getId(), resp.id(), revReq);
            }

            // Set favorite and revision status for specific subset
            if (i % 4 == 0) {
                problemService.updateRevisionStatus(testUser.getId(), resp.id(),
                        new com.greengrid.dto.problem.RevisionUpdateRequest(com.greengrid.entity.RevisionStatus.MASTERED, true));
            } else if (i % 3 == 0) {
                problemService.updateRevisionStatus(testUser.getId(), resp.id(),
                        new com.greengrid.dto.problem.RevisionUpdateRequest(com.greengrid.entity.RevisionStatus.NEEDS_REVISION, false));
            }
        }

        // Verify total problems is 65
        assertEquals(65, problemRepository.countByUserId(testUser.getId()));

        // 2. Audit Delete Isolation: Delete problem #29 (which has 2 revisions)
        UUID targetDeleteId = createdIds.get(28); // index 28 is problem 29 (odd, so it has 2 revisions)
        assertTrue(problemRepository.findById(targetDeleteId).isPresent());
        assertEquals(2, problemRevisionRepository.findByProblemIdOrderByRevisionNumberAsc(targetDeleteId).size());

        problemService.deleteProblem(testUser.getId(), targetDeleteId);

        // Verify target problem and its revisions are deleted
        assertTrue(problemRepository.findById(targetDeleteId).isEmpty());
        assertTrue(problemRevisionRepository.findByProblemIdOrderByRevisionNumberAsc(targetDeleteId).isEmpty());

        // Verify EXACT count is now 64 and all other 64 problems are intact
        assertEquals(64, problemRepository.countByUserId(testUser.getId()));
        for (int i = 0; i < 65; i++) {
            if (i == 28) continue;
            assertTrue(problemRepository.findById(createdIds.get(i)).isPresent(),
                    "Problem index " + i + " must remain unaffected by deleting problem 29");
        }

        // 3. Audit Ordering: Recent Added First (createdAt DESC, id DESC)
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("createdAt"),
                        org.springframework.data.domain.Sort.Order.desc("id")));

        var page0 = problemService.searchProblems(testUser.getId(), null, null, null, null, null, null, null, null, null, pageable);
        assertEquals(64, page0.getTotalElements());
        assertEquals(10, page0.getContent().size());
        assertEquals(createdIds.get(64), page0.getContent().get(0).id(), "Problem 65 (most recently added) must be at top of page 0");

        // 4. Audit Combinable Filters on large dataset
        // Search = "Binary Tree", favorite = true, revisionStatus = MASTERED
        var filteredPage = problemService.searchProblems(
                testUser.getId(), "Binary Tree", null, null, null, null, null, true,
                com.greengrid.entity.RevisionStatus.MASTERED, null, pageable);

        for (ProblemResponse p : filteredPage.getContent()) {
            assertTrue(p.title().contains("Binary Tree") || p.topics().contains("Binary Tree"));
            assertTrue(p.favorite());
            assertEquals(com.greengrid.entity.RevisionStatus.MASTERED, p.revisionStatus());
        }

        // 5. Audit Add New Problem onto existing 64 dataset
        CreateProblemRequest newReq = new CreateProblemRequest(
                "LeetCode", "Brand New Problem 66", "https://leetcode.com/problems/brand-new/",
                Difficulty.EASY, List.of("Array"), "Java",
                "class BrandNew {}", "New notes", "O(1)", "O(1)", LocalDate.now()
        );
        ProblemResponse newResp = problemService.createProblem(testUser.getId(), newReq);
        assertEquals(65, problemRepository.countByUserId(testUser.getId()));

        var pageAfterAdd = problemService.searchProblems(testUser.getId(), null, null, null, null, null, null, null, null, null, pageable);
        assertEquals(newResp.id(), pageAfterAdd.getContent().get(0).id(), "Newly created problem 66 must appear at top of list");
    }

    @Test
    void testDuplicateProblemPrevention() {
        // 1. Create initial problem "Two Sum"
        CreateProblemRequest req1 = new CreateProblemRequest(
                "LeetCode", "Two Sum", "https://leetcode.com/problems/two-sum/",
                Difficulty.EASY, List.of("Array"), "Java",
                "class Solution {}", "Notes 1", "O(n)", "O(n)", LocalDate.now()
        );
        ProblemResponse resp1 = problemService.createProblem(testUser.getId(), req1);
        assertNotNull(resp1.id());

        // 2. Exact reproduction case:
        // Try creating another problem titled: "two sum" -> rejected as duplicate
        CreateProblemRequest reqLowerCase = new CreateProblemRequest(
                "LeetCode", "two sum", "https://leetcode.com/problems/two-sum/",
                Difficulty.EASY, List.of("Array"), "Java",
                "class Solution {}", "Notes lower", "O(n)", "O(n)", LocalDate.now()
        );
        com.greengrid.exception.DuplicateProblemException ex1 = assertThrows(
                com.greengrid.exception.DuplicateProblemException.class,
                () -> problemService.createProblem(testUser.getId(), reqLowerCase)
        );
        assertEquals(resp1.id(), ex1.getExistingProblemId());

        // Try another: "  TWO   SUM  " -> rejected as duplicate
        CreateProblemRequest reqNormalized = new CreateProblemRequest(
                "LeetCode", "  TWO   SUM  ", "https://leetcode.com/problems/two-sum/",
                Difficulty.EASY, List.of("Array"), "Java",
                "class Solution {}", "Notes norm", "O(n)", "O(n)", LocalDate.now()
        );
        com.greengrid.exception.DuplicateProblemException ex2 = assertThrows(
                com.greengrid.exception.DuplicateProblemException.class,
                () -> problemService.createProblem(testUser.getId(), reqNormalized)
        );
        assertEquals(resp1.id(), ex2.getExistingProblemId());

        // 3. Similar titles remain distinct.
        CreateProblemRequest reqBinaryTree = new CreateProblemRequest(
                "LeetCode", "Binary Tree", "https://leetcode.com/problems/binary-tree/",
                Difficulty.EASY, List.of("Tree"), "Java",
                "class Solution {}", "Notes B", "O(n)", "O(1)", LocalDate.now()
        );
        ProblemResponse binaryTree = problemService.createProblem(testUser.getId(), reqBinaryTree);
        ProblemResponse binaryTreeTraversal = problemService.createProblem(testUser.getId(), new CreateProblemRequest(
                "LeetCode", "Binary Tree Traversal", "https://leetcode.com/problems/binary-tree-traversal/",
                Difficulty.EASY, List.of("Tree"), "Java",
                "class Solution {}", "Notes traversal", "O(n)", "O(1)", LocalDate.now()
        ));
        assertNotEquals(binaryTree.id(), binaryTreeTraversal.id());

        // 4. Update title collision test:
        // Renaming B to "Two Sum" must point to problem A.
        UpdateProblemRequest updateToDuplicateReq = new UpdateProblemRequest(
                "LeetCode", "Two Sum", "https://leetcode.com/problems/binary-tree/",
                Difficulty.EASY, List.of("Tree"), "Java",
                "class Solution {}", "Notes B", "O(n)", "O(1)"
        );
        com.greengrid.exception.DuplicateProblemException updateEx = assertThrows(
                com.greengrid.exception.DuplicateProblemException.class,
                () -> problemService.updateProblem(testUser.getId(), binaryTree.id(), updateToDuplicateReq)
        );
        assertEquals(resp1.id(), updateEx.getExistingProblemId());

        // 5. Renaming B to its original title must succeed.
        ProblemResponse binaryTreeUpdated = problemService.updateProblem(testUser.getId(), binaryTree.id(), reqBinaryTreeToUpdateRequest());
        assertEquals("Binary Tree", binaryTreeUpdated.title());

        // 6. Updating problem retaining its own title must succeed cleanly
        UpdateProblemRequest updateOwnReq = new UpdateProblemRequest(
                "LeetCode", "Two Sum", "https://leetcode.com/problems/two-sum/",
                Difficulty.MEDIUM, List.of("Array", "Hash Table"), "Java",
                "class Solution { /* updated */ }", "Updated notes", "O(n)", "O(n)"
        );
        ProblemResponse ownUpdatedResp = problemService.updateProblem(testUser.getId(), resp1.id(), updateOwnReq);
        assertEquals(resp1.id(), ownUpdatedResp.id());
        assertEquals(Difficulty.MEDIUM, ownUpdatedResp.difficulty());

        // 7. Different users can have the same title
        User secondUser = new User();
        secondUser.setEmail("seconduser_" + UUID.randomUUID() + "@greengrid.dev");
        secondUser.setDisplayName("Second User");
        secondUser = userRepository.save(secondUser);

        ProblemResponse respUser2 = problemService.createProblem(secondUser.getId(), req1);
        assertNotNull(respUser2.id());

        // 8. Adding and then editing a revision works without duplicate-title validation.
        CreateRevisionRequest revReq = new CreateRevisionRequest(
                "Revision 2", "Python", "def twoSum(): pass", "Rev notes", "O(n)", "O(n)"
        );
        ProblemResponse revResp = problemService.createRevision(testUser.getId(), resp1.id(), revReq);
        assertEquals(2, revResp.revisionCount());

        ProblemResponse editedRevResp = problemService.updateRevision(testUser.getId(), resp1.id(),
                revResp.revisions().get(1).id(), new CreateRevisionRequest(
                        "Revision 2 edited", "Python", "def twoSum(): return []", "Edited rev notes", "O(n)", "O(n)"));
        assertEquals("Revision 2 edited", editedRevResp.revisions().get(1).title());
    }

    private UpdateProblemRequest reqBinaryTreeToUpdateRequest() {
        return new UpdateProblemRequest(
                "LeetCode", "Binary Tree", "https://leetcode.com/problems/binary-tree/",
                Difficulty.EASY, List.of("Tree"), "Java",
                "class Solution {}", "Notes B", "O(n)", "O(1)"
        );
    }
}
