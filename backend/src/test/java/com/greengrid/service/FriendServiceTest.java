package com.greengrid.service;

import com.greengrid.dto.friend.*;
import com.greengrid.dto.problem.CreateProblemRequest;
import com.greengrid.dto.problem.ProblemResponse;
import com.greengrid.entity.Difficulty;
import com.greengrid.entity.RevisionStatus;
import com.greengrid.entity.User;
import com.greengrid.exception.BadRequestException;
import com.greengrid.exception.ConflictException;
import com.greengrid.exception.ResourceNotFoundException;
import com.greengrid.repository.ProblemRepository;
import com.greengrid.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
public class FriendServiceTest {

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProblemService problemService;

    @Autowired
    private ProblemRepository problemRepository;

    private User userAlice;
    private User userBob;
    private User userCharlie;

    @BeforeEach
    void setUp() {
        userAlice = new User();
        userAlice.setEmail("alice_" + UUID.randomUUID() + "@greengrid.dev");
        userAlice.setDisplayName("Alice Developer");
        userAlice = userRepository.save(userAlice);

        userBob = new User();
        userBob.setEmail("bob_" + UUID.randomUUID() + "@greengrid.dev");
        userBob.setDisplayName("Bob Coder");
        userBob = userRepository.save(userBob);

        userCharlie = new User();
        userCharlie.setEmail("charlie_" + UUID.randomUUID() + "@greengrid.dev");
        userCharlie.setDisplayName("Charlie Engineer");
        userCharlie = userRepository.save(userCharlie);
    }

    @Test
    void testSendFriendRequestSuccess() {
        FriendRequestDto requestDto = friendService.sendFriendRequest(userAlice.getId(), userBob.getId());

        assertNotNull(requestDto.id());
        assertEquals(userAlice.getId(), requestDto.requester().userId());
        assertEquals(userBob.getId(), requestDto.addressee().userId());
        assertEquals("Alice Developer", requestDto.requester().displayName());
        assertEquals("Bob Coder", requestDto.addressee().displayName());

        // Verify pending requests lists
        FriendRequestsResponse bobPending = friendService.getPendingRequests(userBob.getId());
        assertEquals(1, bobPending.incoming().size());
        assertEquals(userAlice.getId(), bobPending.incoming().get(0).requester().userId());
        assertEquals(0, bobPending.outgoing().size());

        FriendRequestsResponse alicePending = friendService.getPendingRequests(userAlice.getId());
        assertEquals(0, alicePending.incoming().size());
        assertEquals(1, alicePending.outgoing().size());
        assertEquals(userBob.getId(), alicePending.outgoing().get(0).addressee().userId());
    }

    @Test
    void testSelfFriendRequestRejected() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> friendService.sendFriendRequest(userAlice.getId(), userAlice.getId())
        );
        assertTrue(ex.getMessage().contains("Cannot send a friend request to yourself"));
    }

    @Test
    void testDuplicateFriendRequestRejected() {
        friendService.sendFriendRequest(userAlice.getId(), userBob.getId());

        // Alice sending to Bob again -> ConflictException
        ConflictException ex1 = assertThrows(
                ConflictException.class,
                () -> friendService.sendFriendRequest(userAlice.getId(), userBob.getId())
        );
        assertTrue(ex1.getMessage().contains("pending friend request already exists"));

        // Bob sending to Alice when Alice's request is pending -> ConflictException
        ConflictException ex2 = assertThrows(
                ConflictException.class,
                () -> friendService.sendFriendRequest(userBob.getId(), userAlice.getId())
        );
        assertTrue(ex2.getMessage().contains("pending friend request already exists"));
    }

    @Test
    void testAcceptRequestCreatesFriendship() {
        FriendRequestDto requestDto = friendService.sendFriendRequest(userAlice.getId(), userBob.getId());

        // Bob accepts Alice's request
        friendService.acceptFriendRequest(userBob.getId(), requestDto.id());

        // Check friendship lists
        List<FriendSummaryDto> aliceFriends = friendService.getFriends(userAlice.getId());
        assertEquals(1, aliceFriends.size());
        assertEquals(userBob.getId(), aliceFriends.get(0).userId());
        assertEquals("Bob Coder", aliceFriends.get(0).displayName());

        List<FriendSummaryDto> bobFriends = friendService.getFriends(userBob.getId());
        assertEquals(1, bobFriends.size());
        assertEquals(userAlice.getId(), bobFriends.get(0).userId());
        assertEquals("Alice Developer", bobFriends.get(0).displayName());
    }

    @Test
    void testRejectRequestDoesNotCreateFriendship() {
        FriendRequestDto requestDto = friendService.sendFriendRequest(userAlice.getId(), userBob.getId());

        // Bob rejects Alice's request
        friendService.rejectFriendRequest(userBob.getId(), requestDto.id());

        // Verify no friendships created
        assertTrue(friendService.getFriends(userAlice.getId()).isEmpty());
        assertTrue(friendService.getFriends(userBob.getId()).isEmpty());

        // Incoming pending is now empty
        assertTrue(friendService.getPendingRequests(userBob.getId()).incoming().isEmpty());
    }

    @Test
    void testDuplicateFriendshipPrevented() {
        FriendRequestDto requestDto = friendService.sendFriendRequest(userAlice.getId(), userBob.getId());
        friendService.acceptFriendRequest(userBob.getId(), requestDto.id());

        // Trying to send request when already friends throws ConflictException
        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> friendService.sendFriendRequest(userAlice.getId(), userBob.getId())
        );
        assertTrue(ex.getMessage().contains("already friends"));
    }

    @Test
    void testFriendCanViewProgressAndNonFriendBlocked() {
        // Create problems for Bob
        CreateProblemRequest req1 = new CreateProblemRequest(
                "LeetCode", "Two Sum", "https://leetcode.com/problems/two-sum/",
                Difficulty.EASY, List.of("Array"), "Java",
                "class Solution {}", "Notes 1", "O(n)", "O(n)", LocalDate.now()
        );
        ProblemResponse p1 = problemService.createProblem(userBob.getId(), req1);

        CreateProblemRequest req2 = new CreateProblemRequest(
                "LeetCode", "3Sum", "https://leetcode.com/problems/3sum/",
                Difficulty.MEDIUM, List.of("Array", "Two Pointers"), "Python",
                "def threeSum(): pass", "Notes 2", "O(n^2)", "O(1)", LocalDate.now()
        );
        ProblemResponse p2 = problemService.createProblem(userBob.getId(), req2);

        // Mark p1 as MASTERED & favorite, p2 as NEEDS_REVISION
        problemService.updateRevisionStatus(userBob.getId(), p1.id(),
                new com.greengrid.dto.problem.RevisionUpdateRequest(RevisionStatus.MASTERED, true));
        problemService.updateRevisionStatus(userBob.getId(), p2.id(),
                new com.greengrid.dto.problem.RevisionUpdateRequest(RevisionStatus.NEEDS_REVISION, false));

        // 1. Non-friend Charlie tries to view Bob's progress -> AccessDeniedException (403)
        assertThrows(
                AccessDeniedException.class,
                () -> friendService.getFriendProgress(userCharlie.getId(), userBob.getId(), ZoneId.systemDefault())
        );

        // 2. Establish friendship between Alice and Bob
        FriendRequestDto requestDto = friendService.sendFriendRequest(userAlice.getId(), userBob.getId());
        friendService.acceptFriendRequest(userBob.getId(), requestDto.id());

        // 3. Friend Alice views Bob's progress -> Succeeds with aggregated metrics only
        FriendProgressResponse progress = friendService.getFriendProgress(userAlice.getId(), userBob.getId(), ZoneId.systemDefault());

        assertEquals(userBob.getId(), progress.userId());
        assertEquals("Bob Coder", progress.displayName());
        assertEquals(2, progress.totalProblems());
        assertEquals(1, progress.masteredCount());
        assertEquals(1, progress.needsRevisionCount());
        assertEquals(0, progress.notSetCount());
        assertEquals(1, progress.starredCount());
        assertEquals(50.0, progress.masteryPercentage());
        assertEquals(1L, progress.difficultyBreakdown().get("EASY"));
        assertEquals(1L, progress.difficultyBreakdown().get("MEDIUM"));
        assertEquals(1L, progress.languageBreakdown().get("Java"));
        assertEquals(1L, progress.languageBreakdown().get("Python"));
    }

    @Test
    void testSelfProgressAccessAllowed() {
        CreateProblemRequest req1 = new CreateProblemRequest(
                "LeetCode", "Two Sum", "https://leetcode.com/problems/two-sum/",
                Difficulty.EASY, List.of("Array"), "Java",
                "class Solution {}", "Notes 1", "O(n)", "O(n)", LocalDate.now()
        );
        problemService.createProblem(userAlice.getId(), req1);

        FriendProgressResponse progress = friendService.getFriendProgress(userAlice.getId(), userAlice.getId(), ZoneId.systemDefault());
        assertEquals(userAlice.getId(), progress.userId());
        assertEquals(1, progress.totalProblems());
    }

    @Test
    void testNonExistentUserProgressAccessThrowsNotFound() {
        UUID fakeUserId = UUID.randomUUID();
        assertThrows(
                ResourceNotFoundException.class,
                () -> friendService.getFriendProgress(userAlice.getId(), fakeUserId, ZoneId.systemDefault())
        );
    }

    @Test
    void testUserSearchByDisplayNameOrEmailExcludesEmail() {
        // 1. Search by display name
        List<UserSearchResultDto> resultsByName = friendService.searchUsers(userAlice.getId(), "Bob");
        assertEquals(1, resultsByName.size());
        assertEquals(userBob.getId(), resultsByName.get(0).userId());
        assertEquals("Bob Coder", resultsByName.get(0).displayName());
        assertEquals("NONE", resultsByName.get(0).relationshipStatus());

        // 2. Search by email internally -> matches userBob but DTO does NOT contain email
        List<UserSearchResultDto> resultsByEmail = friendService.searchUsers(userAlice.getId(), "bob_");
        assertEquals(1, resultsByEmail.size());
        assertEquals(userBob.getId(), resultsByEmail.get(0).userId());
        assertEquals("Bob Coder", resultsByEmail.get(0).displayName());
    }

    @Test
    void testRemoveFriendship() {
        FriendRequestDto requestDto = friendService.sendFriendRequest(userAlice.getId(), userBob.getId());
        friendService.acceptFriendRequest(userBob.getId(), requestDto.id());

        // Remove friend
        friendService.removeFriend(userAlice.getId(), userBob.getId());

        assertTrue(friendService.getFriends(userAlice.getId()).isEmpty());
        assertTrue(friendService.getFriends(userBob.getId()).isEmpty());

        // Bob's progress is no longer accessible to Alice -> AccessDeniedException (403)
        assertThrows(
                AccessDeniedException.class,
                () -> friendService.getFriendProgress(userAlice.getId(), userBob.getId(), ZoneId.systemDefault())
        );

        // New request can be sent again after removal
        FriendRequestDto newReq = friendService.sendFriendRequest(userAlice.getId(), userBob.getId());
        assertNotNull(newReq.id());
    }

    @Test
    void testUnauthorizedAcceptRejectThrowsException() {
        FriendRequestDto requestDto = friendService.sendFriendRequest(userAlice.getId(), userBob.getId());

        // Charlie (unauthorized third party) attempts to accept Alice's request to Bob -> BadRequestException
        assertThrows(
                BadRequestException.class,
                () -> friendService.acceptFriendRequest(userCharlie.getId(), requestDto.id())
        );

        // Charlie attempts to reject Alice's request to Bob -> BadRequestException
        assertThrows(
                BadRequestException.class,
                () -> friendService.rejectFriendRequest(userCharlie.getId(), requestDto.id())
        );
    }
}
