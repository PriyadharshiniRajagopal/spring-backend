package com.example.SplitEase.repository;

import com.example.SplitEase.model.Group;
import com.example.SplitEase.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g JOIN g.userGroups ug WHERE ug.user = :user")
    List<Group> findByUser(@Param("user") User user);

    List<Group> findByCreatedBy(User createdBy);
}