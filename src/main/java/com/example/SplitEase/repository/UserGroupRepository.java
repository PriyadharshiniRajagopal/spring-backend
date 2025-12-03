package com.example.SplitEase.repository;

import com.example.SplitEase.model.Group;
import com.example.SplitEase.model.User;
import com.example.SplitEase.model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    List<UserGroup> findByGroup(Group group);
    List<UserGroup> findByUser(User user);
    Optional<UserGroup> findByUserAndGroup(User user, Group group);
    boolean existsByUserAndGroup(User user, Group group);
}