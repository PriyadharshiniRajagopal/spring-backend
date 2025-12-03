package com.example.SplitEase.repository;

import com.example.SplitEase.model.Friend;
import com.example.SplitEase.model.FriendStatus;
import com.example.SplitEase.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    // Find friendship between two users (in either direction)
    @Query("SELECT f FROM Friend f WHERE (f.user = :user1 AND f.friend = :user2) OR (f.user = :user2 AND f.friend = :user1)")
    Optional<Friend> findFriendship(@Param("user1") User user1, @Param("user2") User user2);

    // Find all friends for a user (accepted status)
    @Query("SELECT f FROM Friend f WHERE (f.user = :user OR f.friend = :user) AND f.status = 'ACCEPTED'")
    List<Friend> findFriendsByUser(@Param("user") User user);

    // Find pending friend requests for a user
    @Query("SELECT f FROM Friend f WHERE f.friend = :user AND f.status = 'PENDING'")
    List<Friend> findPendingRequests(@Param("user") User user);

    // Find sent friend requests by a user
    @Query("SELECT f FROM Friend f WHERE f.user = :user AND f.status = 'PENDING'")
    List<Friend> findSentRequests(@Param("user") User user);

    // Check if users are friends
    @Query("SELECT COUNT(f) > 0 FROM Friend f WHERE ((f.user = :user1 AND f.friend = :user2) OR (f.user = :user2 AND f.friend = :user1)) AND f.status = 'ACCEPTED'")
    boolean areFriends(@Param("user1") User user1, @Param("user2") User user2);
}