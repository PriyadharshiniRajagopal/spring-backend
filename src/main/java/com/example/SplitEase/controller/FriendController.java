package com.example.SplitEase.controller;

import com.example.SplitEase.dto.FriendRequest;
import com.example.SplitEase.dto.FriendResponse;
import com.example.SplitEase.model.User;
import com.example.SplitEase.service.FriendService;
import com.example.SplitEase.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserService userService;

    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(@RequestBody FriendRequest request,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByEmail(userDetails.getUsername());
            FriendResponse friendResponse = friendService.sendFriendRequest(request, currentUser);
            return ResponseEntity.ok(friendResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{friendRequestId}/accept")
    public ResponseEntity<?> acceptFriendRequest(@PathVariable Long friendRequestId,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByEmail(userDetails.getUsername());
            FriendResponse friendResponse = friendService.acceptFriendRequest(friendRequestId, currentUser);
            return ResponseEntity.ok(friendResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{friendRequestId}/reject")
    public ResponseEntity<?> rejectFriendRequest(@PathVariable Long friendRequestId,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByEmail(userDetails.getUsername());
            friendService.rejectFriendRequest(friendRequestId, currentUser);
            return ResponseEntity.ok("Friend request rejected");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<?> removeFriend(@PathVariable Long friendId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByEmail(userDetails.getUsername());
            friendService.removeFriend(friendId, currentUser);
            return ResponseEntity.ok("Friend removed");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<FriendResponse>> getFriends(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByEmail(userDetails.getUsername());
        List<FriendResponse> friends = friendService.getFriends(currentUser);
        return ResponseEntity.ok(friends);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FriendResponse>> getPendingRequests(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByEmail(userDetails.getUsername());
        List<FriendResponse> pendingRequests = friendService.getPendingRequests(currentUser);
        return ResponseEntity.ok(pendingRequests);
    }

    @GetMapping("/sent")
    public ResponseEntity<List<FriendResponse>> getSentRequests(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByEmail(userDetails.getUsername());
        List<FriendResponse> sentRequests = friendService.getSentRequests(currentUser);
        return ResponseEntity.ok(sentRequests);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String q,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByEmail(userDetails.getUsername());
            List<User> users = friendService.searchUsers(q, currentUser);
            return ResponseEntity.ok(users);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}