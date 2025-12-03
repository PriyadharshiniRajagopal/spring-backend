package com.example.SplitEase.controller;

import com.example.SplitEase.dto.CreateExpenseRequest;
import com.example.SplitEase.dto.ExpenseResponse;
import com.example.SplitEase.model.User;
import com.example.SplitEase.service.ExpenseService;
import com.example.SplitEase.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createExpense(@RequestBody CreateExpenseRequest request,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByEmail(userDetails.getUsername());
            ExpenseResponse expense = expenseService.createExpense(request, currentUser);
            return ResponseEntity.ok(expense);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getGroupExpenses(@PathVariable Long groupId,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByEmail(userDetails.getUsername());
            List<ExpenseResponse> expenses = expenseService.getExpensesByGroup(groupId, currentUser);

            // Add debug logging
            System.out.println("Returning expenses for group " + groupId + ": " + expenses.size());
            expenses.forEach(expense -> {
                System.out.println("Expense: " + expense.getDescription() + ", PaidBy: " +
                        (expense.getPaidBy() != null ? expense.getPaidBy().getName() : "NULL"));
            });

            return ResponseEntity.ok(expenses);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ADD THIS NEW ENDPOINT FOR BALANCE CALCULATION
    @GetMapping("/groups/{groupId}/balances")
    public ResponseEntity<?> getGroupBalances(@PathVariable Long groupId,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByEmail(userDetails.getUsername());
            Map<String, Object> balances = expenseService.calculateGroupBalances(groupId, currentUser);
            return ResponseEntity.ok(balances);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<?> deleteExpense(@PathVariable Long expenseId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.getUserByEmail(userDetails.getUsername());
            expenseService.deleteExpense(expenseId, currentUser);
            return ResponseEntity.ok("Expense deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}