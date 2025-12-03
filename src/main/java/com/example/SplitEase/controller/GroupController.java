package com.example.SplitEase.controller;

import com.example.SplitEase.dto.CreateGroupRequest;
import com.example.SplitEase.dto.GroupResponse;
import com.example.SplitEase.model.User;
import com.example.SplitEase.service.GroupService;
import com.example.SplitEase.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupRequest request,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByEmail(userDetails.getUsername());
            var group = groupService.createGroup(request, currentUser);
            return ResponseEntity.ok(group);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getUserGroups(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByEmail(userDetails.getUsername());
        List<GroupResponse> groups = groupService.getGroupsByUser(currentUser);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroup(@PathVariable Long groupId) {
        try {
            GroupResponse group = groupService.getGroupById(groupId);
            return ResponseEntity.ok(group);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> addMember(@PathVariable Long groupId,
                                       @PathVariable Long userId,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByEmail(userDetails.getUsername());
            groupService.addMemberToGroup(groupId, userId, currentUser);
            return ResponseEntity.ok("Member added successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable Long groupId,
                                          @PathVariable Long userId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByEmail(userDetails.getUsername());
            groupService.removeMemberFromGroup(groupId, userId, currentUser);
            return ResponseEntity.ok("Member removed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}