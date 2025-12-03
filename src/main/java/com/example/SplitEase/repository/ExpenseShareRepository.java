package com.example.SplitEase.repository;

import com.example.SplitEase.model.Expense;
import com.example.SplitEase.model.ExpenseShare;
import com.example.SplitEase.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {

    @Query("SELECT es FROM ExpenseShare es JOIN FETCH es.expense e JOIN FETCH e.paidBy WHERE es.user = :user")
    List<ExpenseShare> findByUser(@Param("user") User user);

    @Query("SELECT es FROM ExpenseShare es JOIN FETCH es.user WHERE es.expense = :expense")
    List<ExpenseShare> findByExpense(@Param("expense") Expense expense);
}