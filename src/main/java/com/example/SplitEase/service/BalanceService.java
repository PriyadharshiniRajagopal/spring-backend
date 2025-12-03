package com.example.SplitEase.service;

import com.example.SplitEase.model.Expense;
import com.example.SplitEase.model.ExpenseShare;
import com.example.SplitEase.model.User;
import com.example.SplitEase.model.UserGroup;
import com.example.SplitEase.repository.ExpenseShareRepository;
import com.example.SplitEase.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BalanceService {

    @Autowired
    private ExpenseShareRepository expenseShareRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    private static final double EPSILON = 0.01; // Tolerance for floating-point comparisons

    // --- 1. SINGLE USER BALANCE SUMMARY (Corrected Logic) ---

    /**
     * Calculates the overall balance summary (owed, owing, net) for a single user.
     * The core logic is: Net Balance = Total Paid - Total Share.
     */
    public Map<String, Object> getUserBalanceSummary(User user) {
        Map<String, Object> summary = new HashMap<>();

        // Get all expense shares for the user
        List<ExpenseShare> userShares = expenseShareRepository.findByUser(user);

        double totalPaid = 0.0;     // Total amount user has actually paid
        double totalShare = 0.0;    // Total amount user is responsible for (their share)

        for (ExpenseShare share : userShares) {
            Expense expense = share.getExpense();
            User paidBy = expense.getPaidBy();
            double shareAmount = share.getShareAmount();

            // 1. Accumulate the user's total financial responsibility (Share)
            totalShare += shareAmount;

            // 2. Accumulate the total amount the user actually paid (Paid)
            if (paidBy.getId().equals(user.getId())) {
                // If the user paid, they paid the full expense amount
                totalPaid += expense.getAmount();
            }
        }

        // Net Balance: Positive means others owe you (Credit), Negative means you owe others (Debit)
        double netBalance = totalPaid - totalShare;

        // Populate the conventional Owed/Owing fields
        double totalOwed = netBalance > 0 ? netBalance : 0.0;
        double totalOwing = netBalance < 0 ? Math.abs(netBalance) : 0.0;

        summary.put("totalPaid", Math.round(totalPaid * 100.0) / 100.0);
        summary.put("totalShare", Math.round(totalShare * 100.0) / 100.0);
        summary.put("totalOwed", Math.round(totalOwed * 100.0) / 100.0);
        summary.put("totalOwing", Math.round(totalOwing * 100.0) / 100.0);
        summary.put("netBalance", Math.round(netBalance * 100.0) / 100.0);

        return summary;
    }

    // --- 2. MINIMUM TRANSACTION SETTLEMENT (Greedy Approach) ---

    /**
     * Calculates the minimum number of transactions needed to settle all balances among a set of users.
     * This uses a greedy approach: always settle the largest debt with the largest credit.
     * Note: In a real app, this would likely be performed for a specific Group.
     */
    public List<Map<String, Object>> getMinimumSettlements(List<User> usersInvolved) {
        // Step 1: Calculate Net Balances for all involved users
        Map<User, Double> netBalances = calculateAllUserNetBalances(usersInvolved);

        // Step 2: Separate Givers (Debtors) and Takers (Creditors) using Priority Queues

        // Takers (Creditors): Max Heap (highest credit first)
        // Stores users who are owed money (Positive Balance)
        PriorityQueue<User> takers = new PriorityQueue<>(
                Comparator.comparingDouble(netBalances::get).reversed()
        );

        // Givers (Debtors): Min Heap (largest ABSOLUTE debt first).
        // Stored as negative values, so we use a standard max heap based on the balance value.
        // Stores users who owe money (Negative Balance)
        PriorityQueue<User> givers = new PriorityQueue<>(
                Comparator.comparingDouble(netBalances::get)
        );

        for (User user : usersInvolved) {
            double balance = netBalances.getOrDefault(user, 0.0);
            if (Math.abs(balance) > EPSILON) {
                if (balance > 0) {
                    takers.add(user);
                } else {
                    givers.add(user);
                }
            }
        }

        // Step 3: Perform Settlements (Greedy)
        List<Map<String, Object>> settlements = new ArrayList<>();

        while (!givers.isEmpty() && !takers.isEmpty()) {
            User giver = givers.poll(); // Owe's money (most negative balance)
            User taker = takers.poll(); // Is owed money (most positive balance)

            double giverBalance = netBalances.get(giver);    // e.g., -50.00
            double takerBalance = netBalances.get(taker);    // e.g., 80.00

            // The transaction amount is the smaller of the debt (absolute giver balance) or credit (taker balance)
            double transactionAmount = Math.min(Math.abs(giverBalance), takerBalance);

            // Record the transaction
            Map<String, Object> transaction = new HashMap<>();
            transaction.put("from", giver.getName());
            transaction.put("to", taker.getName());
            transaction.put("amount", Math.round(transactionAmount * 100.0) / 100.0);
            settlements.add(transaction);

            // Update balances
            double newGiverBalance = giverBalance + transactionAmount; // Moves giver balance closer to 0
            double newTakerBalance = takerBalance - transactionAmount; // Moves taker balance closer to 0

            // Re-add to the queues if they still have a significant balance remaining
            if (Math.abs(newGiverBalance) > EPSILON) {
                netBalances.put(giver, newGiverBalance);
                givers.add(giver);
            }
            if (Math.abs(newTakerBalance) > EPSILON) {
                netBalances.put(taker, newTakerBalance);
                takers.add(taker);
            }
        }

        return settlements;
    }

    /**
     * Helper method to calculate the net balances for a list of users.
     */
    private Map<User, Double> calculateAllUserNetBalances(List<User> users) {
        Map<User, Double> balances = new HashMap<>();

        // Initialize balances and fetch all relevant shares
        for (User user : users) {
            balances.put(user, 0.0);
            // Assuming we only need shares where this user is involved in the expense.
            // A more robust solution might fetch all shares in a specific group.
            List<ExpenseShare> userShares = expenseShareRepository.findByUser(user);

            for (ExpenseShare share : userShares) {
                Expense expense = share.getExpense();
                User paidBy = expense.getPaidBy();
                double shareAmount = share.getShareAmount();

                // 1. Debit: Subtract the user's responsibility (share) from their balance.
                // The user's net balance decreases by the amount they are responsible for.
                balances.compute(user, (k, v) -> v - shareAmount);

                // 2. Credit: If the user paid, add the difference between the full amount and their share to the payer's balance.
                // NOTE: This logic is simpler than the first method. The key is to calculate the total credit/debit for the Payer/Sharer.

                // If the current user paid the expense, add the full amount to their balance.
                if (paidBy.getId().equals(user.getId())) {
                    balances.compute(user, (k, v) -> v + expense.getAmount());
                }
            }
        }

        // Final sanity check/cleanup
        return balances.entrySet().stream()
                .filter(entry -> Math.abs(entry.getValue()) > EPSILON)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    // --- 3. DASHBOARD STATS (Unchanged but using the corrected summary) ---

    public int getActiveGroupsCount(User user) {
        return userGroupRepository.findByUser(user).size();
    }

    public Map<String, Object> getDashboardStats(User user) {
        Map<String, Object> stats = new HashMap<>();
        // Uses the corrected balance summary
        Map<String, Object> balanceSummary = getUserBalanceSummary(user);

        stats.put("balanceSummary", balanceSummary);
        stats.put("activeGroups", getActiveGroupsCount(user));
        stats.put("totalExpenses", getTotalExpensesCount(user));

        return stats;
    }

    private int getTotalExpensesCount(User user) {
        // Count expenses where user is either payer or has shares
        List<ExpenseShare> userShares = expenseShareRepository.findByUser(user);
        return (int) userShares.stream()
                .map(ExpenseShare::getExpense)
                .distinct()
                .count();
    }
}