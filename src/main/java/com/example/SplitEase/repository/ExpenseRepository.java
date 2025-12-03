package com.example.SplitEase.repository;

import com.example.SplitEase.model.Expense;
import com.example.SplitEase.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e JOIN FETCH e.paidBy JOIN FETCH e.group WHERE e.group = :group ORDER BY e.createdAt DESC")
    List<Expense> findByGroupOrderByCreatedAtDesc(@Param("group") Group group);

    @Query("SELECT e FROM Expense e JOIN FETCH e.paidBy JOIN FETCH e.group WHERE e.group.id = :groupId ORDER BY e.createdAt DESC")
    List<Expense> findByGroupIdOrderByCreatedAtDesc(@Param("groupId") Long groupId);

    // ADD THIS MISSING METHOD
    List<Expense> findByGroup(Group group);
}