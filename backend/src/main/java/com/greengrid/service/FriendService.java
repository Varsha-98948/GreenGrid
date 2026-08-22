package com.greengrid.service;

import com.greengrid.dto.friend.*;
import com.greengrid.entity.*;
import com.greengrid.exception.BadRequestException;
import com.greengrid.exception.ConflictException;
import com.greengrid.exception.ResourceNotFoundException;
import com.greengrid.repository.*;
import com.greengrid.util.StreakCalculator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final GitHubAccountRepository gitHubAccountRepository;
    private final ProblemRepository problemRepository;

    public FriendService(UserRepository userRepository,
                         FriendRequestRepository friendRequestRepository,
                         FriendshipRepository friendshipRepository,
                         GitHubAccountRepository gitHubAccountRepository,
                         ProblemRepository problemRepository) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.gitHubAccountRepository = gitHubAccountRepository;
        this.problemRepository = problemRepository;
    }

    @Transactional(readOnly = true)
    public List<UserSearchResultDto> searchUsers(UUID currentUserId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cleanQuery = query.trim();
        List<User> users = userRepository.searchUsersByDisplayNameOrEmail(cleanQuery, currentUserId);

        return users.stream().map(user -> {
            String ghUsername = gitHubAccountRepository.findByUserId(user.getId())
                    .map(GitHubAccount::getGithubUsername).orElse(null);

            String relationshipStatus = "NONE";
            if (friendshipRepository.areFriends(currentUserId, user.getId())) {
                relationshipStatus = "FRIENDS";
            } else {
                Optional<FriendRequest> pending = friendRequestRepository.findPendingRequestBetween(currentUserId, user.getId());
                if (pending.isPresent()) {
                    if (pending.get().getRequester().getId().equals(currentUserId)) {
                        relationshipStatus = "PENDING_SENT";
                    } else {
                        relationshipStatus = "PENDING_RECEIVED";
                    }
                }
            }

            return new UserSearchResultDto(
                    user.getId(),
                    user.getDisplayName(),
                    user.getAvatarUrl(),
                    ghUsername,
                    relationshipStatus
            );
        }).toList();
    }

    @Transactional
    public FriendRequestDto sendFriendRequest(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("Cannot send a friend request to yourself");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        if (friendshipRepository.areFriends(currentUserId, targetUserId)) {
            throw new ConflictException("You are already friends with this user");
        }

        if (friendRequestRepository.findPendingRequestBetween(currentUserId, targetUserId).isPresent()) {
            throw new ConflictException("A pending friend request already exists between you and this user");
        }

        List<FriendRequest> existingRequests = friendRequestRepository.findAllRequestsBetween(currentUserId, targetUserId);
        Optional<FriendRequest> existingFromMe = existingRequests.stream()
                .filter(r -> r.getRequester().getId().equals(currentUserId))
                .findFirst();

        FriendRequest request;
        if (existingFromMe.isPresent()) {
            request = existingFromMe.get();
            if (request.getStatus() == FriendRequestStatus.ACCEPTED) {
                throw new ConflictException("You are already friends with this user");
            }
            request.setStatus(FriendRequestStatus.PENDING);
        } else {
            request = new FriendRequest();
            request.setRequester(currentUser);
            request.setAddressee(targetUser);
            request.setStatus(FriendRequestStatus.PENDING);
        }

        request = friendRequestRepository.save(request);
        return toFriendRequestDto(request);
    }

    @Transactional(readOnly = true)
    public FriendRequestsResponse getPendingRequests(UUID currentUserId) {
        List<FriendRequestDto> incoming = friendRequestRepository
                .findByAddresseeIdAndStatus(currentUserId, FriendRequestStatus.PENDING)
                .stream().map(this::toFriendRequestDto).toList();

        List<FriendRequestDto> outgoing = friendRequestRepository
                .findByRequesterIdAndStatus(currentUserId, FriendRequestStatus.PENDING)
                .stream().map(this::toFriendRequestDto).toList();

        return new FriendRequestsResponse(incoming, outgoing);
    }

    @Transactional
    public void acceptFriendRequest(UUID currentUserId, UUID requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!request.getAddressee().getId().equals(currentUserId)) {
            throw new BadRequestException("You are not authorized to accept this friend request");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BadRequestException("Friend request is not pending");
        }

        request.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequestRepository.save(request);

        User reqUser = request.getRequester();
        User addresseeUser = request.getAddressee();

        // Enforce canonical ordering user1 < user2 for UUIDs (using String comparison to match DB check constraint)
        boolean reqIsSmaller = reqUser.getId().toString().compareTo(addresseeUser.getId().toString()) < 0;
        User u1 = reqIsSmaller ? reqUser : addresseeUser;
        User u2 = reqIsSmaller ? addresseeUser : reqUser;

        if (!friendshipRepository.areFriends(u1.getId(), u2.getId())) {
            Friendship friendship = new Friendship();
            friendship.setUser1(u1);
            friendship.setUser2(u2);
            friendshipRepository.saveAndFlush(friendship);
        }
    }

    @Transactional
    public void rejectFriendRequest(UUID currentUserId, UUID requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!request.getAddressee().getId().equals(currentUserId)) {
            throw new BadRequestException("You are not authorized to reject this friend request");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BadRequestException("Friend request is not pending");
        }

        request.setStatus(FriendRequestStatus.REJECTED);
        friendRequestRepository.save(request);
    }

    @Transactional
    public void cancelFriendRequest(UUID currentUserId, UUID requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!request.getRequester().getId().equals(currentUserId)) {
            throw new BadRequestException("You are not authorized to cancel this friend request");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BadRequestException("Friend request is not pending");
        }

        friendRequestRepository.delete(request);
    }

    @Transactional(readOnly = true)
    public List<FriendSummaryDto> getFriends(UUID currentUserId) {
        List<Friendship> friendships = friendshipRepository.findAllByUserId(currentUserId);

        return friendships.stream().map(f -> {
            User friendUser = f.getUser1().getId().equals(currentUserId) ? f.getUser2() : f.getUser1();
            String ghUsername = gitHubAccountRepository.findByUserId(friendUser.getId())
                    .map(GitHubAccount::getGithubUsername).orElse(null);

            return new FriendSummaryDto(
                    friendUser.getId(),
                    friendUser.getDisplayName(),
                    friendUser.getAvatarUrl(),
                    ghUsername,
                    f.getCreatedAt()
            );
        }).toList();
    }

    @Transactional
    public void removeFriend(UUID currentUserId, UUID friendUserId) {
        Friendship friendship = friendshipRepository.findFriendshipBetween(currentUserId, friendUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship not found"));

        friendshipRepository.delete(friendship);

        // Also clean up any requests between them
        List<FriendRequest> requests = friendRequestRepository.findAllRequestsBetween(currentUserId, friendUserId);
        if (!requests.isEmpty()) {
            friendRequestRepository.deleteAll(requests);
        }
    }

    @Transactional(readOnly = true)
    public FriendProgressResponse getFriendProgress(UUID currentUserId, UUID targetUserId, ZoneId zone) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Privacy check: self-access allowed, non-friends blocked with 403 Forbidden
        if (!currentUserId.equals(targetUserId) && !friendshipRepository.areFriends(currentUserId, targetUserId)) {
            throw new AccessDeniedException("You can only view progress of confirmed friends");
        }

        long totalProblems = problemRepository.countByUserId(targetUserId);
        long masteredCount = problemRepository.countByUserIdAndRevisionStatus(targetUserId, RevisionStatus.MASTERED);
        long needsRevisionCount = problemRepository.countByUserIdAndRevisionStatus(targetUserId, RevisionStatus.NEEDS_REVISION);
        long notSetCount = problemRepository.countByUserIdAndRevisionStatus(targetUserId, RevisionStatus.NONE);
        long starredCount = problemRepository.countByUserIdAndFavorite(targetUserId, true);

        List<LocalDate> solvedDatesDesc = problemRepository.findDistinctSolvedDatesForUser(targetUserId);
        int currentStreak = StreakCalculator.calculateCurrentStreak(solvedDatesDesc, LocalDate.now(zone));

        double masteryPercentage = totalProblems == 0 ? 0.0 :
                Math.round(((double) masteredCount / totalProblems * 100.0) * 10.0) / 10.0;

        Map<String, Long> difficultyBreakdown = problemRepository.countByDifficultyForUser(targetUserId).stream()
                .collect(Collectors.toMap(
                        row -> row.getDifficulty().name(), row -> row.getTotal(),
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, Long> languageBreakdown = problemRepository.countByLanguageForUser(targetUserId).stream()
                .collect(Collectors.toMap(
                        row -> row.getLanguage(), row -> row.getTotal(),
                        (a, b) -> a, LinkedHashMap::new));

        String ghUsername = gitHubAccountRepository.findByUserId(targetUserId)
                .map(GitHubAccount::getGithubUsername).orElse(null);

        return new FriendProgressResponse(
                targetUser.getId(),
                targetUser.getDisplayName(),
                targetUser.getAvatarUrl(),
                ghUsername,
                totalProblems,
                masteredCount,
                needsRevisionCount,
                notSetCount,
                starredCount,
                currentStreak,
                masteryPercentage,
                difficultyBreakdown,
                languageBreakdown
        );
    }

    private PublicUserSummaryDto toPublicUserSummaryDto(User user) {
        String ghUsername = gitHubAccountRepository.findByUserId(user.getId())
                .map(GitHubAccount::getGithubUsername).orElse(null);
        return new PublicUserSummaryDto(user.getId(), user.getDisplayName(), user.getAvatarUrl(), ghUsername);
    }

    private FriendRequestDto toFriendRequestDto(FriendRequest request) {
        return new FriendRequestDto(
                request.getId(),
                toPublicUserSummaryDto(request.getRequester()),
                toPublicUserSummaryDto(request.getAddressee()),
                request.getStatus(),
                request.getCreatedAt()
        );
    }
}
