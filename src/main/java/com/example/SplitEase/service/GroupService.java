package com.example.SplitEase.service;

import com.example.SplitEase.dto.CreateGroupRequest;
import com.example.SplitEase.dto.GroupResponse;
import com.example.SplitEase.model.Group;
import com.example.SplitEase.model.User;
import com.example.SplitEase.model.UserGroup;
import com.example.SplitEase.repository.GroupRepository;
import com.example.SplitEase.repository.UserGroupRepository;
import com.example.SplitEase.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    public Group createGroup(CreateGroupRequest request, User createdBy) {
        // Create the group
        Group group = new Group();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setCreatedBy(createdBy);

        Group savedGroup = groupRepository.save(group);

        // Add creator as member
        addUserToGroup(createdBy, savedGroup);

        // Add other members
        if (request.getMemberIds() != null) {
            for (Long memberId : request.getMemberIds()) {
                User member = userRepository.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("User not found with id: " + memberId));
                addUserToGroup(member, savedGroup);
            }
        }

        return savedGroup;
    }

    private void addUserToGroup(User user, Group group) {
        if (!userGroupRepository.existsByUserAndGroup(user, group)) {
            UserGroup userGroup = new UserGroup();
            userGroup.setUser(user);
            userGroup.setGroup(group);
            userGroupRepository.save(userGroup);
        }
    }

    public List<GroupResponse> getGroupsByUser(User user) {
        List<Group> groups = groupRepository.findByUser(user);
        return groups.stream()
                .map(this::convertToGroupResponse)
                .collect(Collectors.toList());
    }

    public GroupResponse getGroupById(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));
        return convertToGroupResponse(group);
    }

    private GroupResponse convertToGroupResponse(Group group) {
        List<UserGroup> userGroups = userGroupRepository.findByGroup(group);

        List<GroupResponse.UserDto> members = userGroups.stream()
                .map(ug -> new GroupResponse.UserDto(
                        ug.getUser().getId(),
                        ug.getUser().getName(),
                        ug.getUser().getEmail()))
                .collect(Collectors.toList());

        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                new GroupResponse.UserDto(
                        group.getCreatedBy().getId(),
                        group.getCreatedBy().getName(),
                        group.getCreatedBy().getEmail()),
                group.getCreatedAt(),
                members,
                members.size()
        );
    }

    public void addMemberToGroup(Long groupId, Long userId, User currentUser) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if current user is in the group
        if (!userGroupRepository.existsByUserAndGroup(currentUser, group)) {
            throw new RuntimeException("You are not a member of this group");
        }

        User userToAdd = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        addUserToGroup(userToAdd, group);
    }

    public void removeMemberFromGroup(Long groupId, Long userId, User currentUser) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Only group creator can remove members
        if (!group.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only group creator can remove members");
        }

        User userToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserGroup userGroup = userGroupRepository.findByUserAndGroup(userToRemove, group)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

        // Don't allow removing the creator
        if (userToRemove.getId().equals(group.getCreatedBy().getId())) {
            throw new RuntimeException("Cannot remove group creator");
        }

        userGroupRepository.delete(userGroup);
    }
}