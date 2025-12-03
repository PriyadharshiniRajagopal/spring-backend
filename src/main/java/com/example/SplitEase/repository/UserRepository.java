package com.example.SplitEase.repository;

import com.example.SplitEase.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);

    // Search methods
    List<User> findByEmailContainingIgnoreCase(String email);
    List<User> findByNameContainingIgnoreCase(String name);

    // Combined search - CORRECTED METHOD NAME
    List<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(String email, String name);

    // Search excluding specific users
    @Query("SELECT u FROM User u WHERE (LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))) AND u.id NOT IN :excludeIds")
    List<User> searchUsersExcluding(@Param("query") String query, @Param("excludeIds") List<Long> excludeIds);
}