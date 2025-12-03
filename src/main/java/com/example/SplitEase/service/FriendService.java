package com.example.SplitEase.service;

import com.example.SplitEase.dto.FriendRequest;
import com.example.SplitEase.dto.FriendResponse;
import com.example.SplitEase.model.Friend;
import com.example.SplitEase.model.FriendStatus;
import com.example.SplitEase.model.User;
import com.example.SplitEase.repository.FriendRepository;
import com.example.SplitEase.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendService {

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private UserRepository userRepository;

    public FriendResponse sendFriendRequest(FriendRequest request, User currentUser) {
        // ... (Send Request logic remains unchanged)
        User friendUser;
        if (request.getFriendId() != null) {
            friendUser = userRepository.findById(request.getFriendId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } else if (request.getFriendEmail() != null) {
            friendUser = userRepository.findByEmail(request.getFriendEmail())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getFriendEmail()));
        } else {
            throw new RuntimeException("Either friendId or friendEmail must be provided");
        }

        if (friendUser.getId().equals(currentUser.getId())) {
            throw new RuntimeException("You cannot add yourself as a friend");
        }

        friendRepository.findFriendship(currentUser, friendUser).ifPresent(friend -> {
            throw new RuntimeException("Friend request already exists or users are already friends");
        });

        Friend friend = new Friend();
        friend.setUser(currentUser); // Sender
        friend.setFriend(friendUser); // Receiver
        friend.setStatus(FriendStatus.PENDING);

        Friend savedFriend = friendRepository.save(friend);
        // Use the new, robust conversion method
        return convertToFriendResponse(savedFriend, currentUser);
    }

    public FriendResponse acceptFriendRequest(Long friendRequestId, User currentUser) {
        // ... (Accept Request logic remains unchanged)
        Friend friendRequest = friendRepository.findById(friendRequestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!friendRequest.getFriend().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only accept friend requests sent to you");
        }

        if (friendRequest.getStatus() != FriendStatus.PENDING) {
            throw new RuntimeException("Friend request is not pending");
        }

        friendRequest.setStatus(FriendStatus.ACCEPTED);
        Friend updatedFriend = friendRepository.save(friendRequest);
        return convertToFriendResponse(updatedFriend, currentUser);
    }

    // ... (rejectFriendRequest, removeFriend, getFriends, getSentRequests remain unchanged)

    public void rejectFriendRequest(Long friendRequestId, User currentUser) {
        Friend friendRequest = friendRepository.findById(friendRequestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        // Verify the current user is the receiver of the request
        if (!friendRequest.getFriend().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only reject friend requests sent to you");
        }

        friendRepository.delete(friendRequest);
    }

    public void removeFriend(Long friendId, User currentUser) {
        User friendUser = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Friend friendship = friendRepository.findFriendship(currentUser, friendUser)
                .orElseThrow(() -> new RuntimeException("Friendship not found"));

        friendRepository.delete(friendship);
    }

    public List<FriendResponse> getFriends(User user) {
        List<Friend> friends = friendRepository.findFriendsByUser(user);
        return friends.stream()
                .map(friend -> convertToFriendResponse(friend, user))
                .collect(Collectors.toList());
    }

    public List<FriendResponse> getPendingRequests(User user) {
        List<Friend> pendingRequests = friendRepository.findPendingRequests(user);
        return pendingRequests.stream()
                .map(friend -> convertToFriendResponse(friend, user))
                .collect(Collectors.toList());
    }

    public List<FriendResponse> getSentRequests(User user) {
        List<Friend> sentRequests = friendRepository.findSentRequests(user);
        return sentRequests.stream()
                .map(friend -> convertToFriendResponse(friend, user))
                .collect(Collectors.toList());
    }

    /**
     * CORRECTED: Ensures the DTO correctly identifies the SENDER and the other party (the friend)
     * based on the context of the status.
     */
    private FriendResponse convertToFriendResponse(Friend friend, User currentUser) {
        User sender = friend.getUser();
        User receiver = friend.getFriend();

        // 1. If the status is PENDING, the receiver (currentUser) needs to see the SENDER's name.
        // We will consistently map the DTO fields 'user' and 'friend' to the SENDER and RECEIVER.
        if (friend.getStatus() == FriendStatus.PENDING) {

            // If the currentUser is the RECEIVER (looking at pending requests):
            if (currentUser.getId().equals(receiver.getId())) {
                // The DTO needs to clearly show the SENDER's details
                return new FriendResponse(
                        friend.getId(),
                        convertToUserDto(sender),    // SENDER's details are shown to the receiver
                        convertToUserDto(receiver),  // RECEIVER's details are hidden/used for context
                        friend.getStatus(),
                        friend.getCreatedAt(),
                        friend.getUpdatedAt()
                );
            }
            // If the currentUser is the SENDER (looking at sent requests):
            else if (currentUser.getId().equals(sender.getId())) {
                // The DTO needs to show the RECEIVER's details (who the request was sent to)
                return new FriendResponse(
                        friend.getId(),
                        convertToUserDto(sender),    // SENDER's details (current user)
                        convertToUserDto(receiver),  // RECEIVER's details (the pending friend)
                        friend.getStatus(),
                        friend.getCreatedAt(),
                        friend.getUpdatedAt()
                );
            }
        }

        // 2. If the status is ACCEPTED/Other, return the actual friend's details.
        // The DTO fields 'user' and 'friend' should contain the two users involved.
        User actualFriend = sender.getId().equals(currentUser.getId()) ? receiver : sender;

        return new FriendResponse(
                friend.getId(),
                convertToUserDto(currentUser),      // Current User's details
                convertToUserDto(actualFriend),     // The actual FRIEND's details
                friend.getStatus(),
                friend.getCreatedAt(),
                friend.getUpdatedAt()
        );
    }

    private FriendResponse.UserDto convertToUserDto(User user) {
        return new FriendResponse.UserDto(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    public List<User> searchUsers(String query, User currentUser) {
        // Search users by name or email, excluding current user
        return userRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(query, query)
                .stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .collect(Collectors.toList());
    }
}