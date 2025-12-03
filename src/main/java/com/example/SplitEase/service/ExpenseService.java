package com.example.SplitEase.service;

import com.example.SplitEase.dto.CreateExpenseRequest;
import com.example.SplitEase.dto.ExpenseResponse;
import com.example.SplitEase.model.*;
import com.example.SplitEase.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseShareRepository expenseShareRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    public ExpenseResponse createExpense(CreateExpenseRequest request, User currentUser) {
        // Validate group exists and user is member
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!userGroupRepository.existsByUserAndGroup(currentUser, group)) {
            throw new RuntimeException("You are not a member of this group");
        }

        // Validate paid by user exists and is in group
        User paidBy = userRepository.findById(request.getPaidById())
                .orElseThrow(() -> new RuntimeException("Payer not found"));

        if (!userGroupRepository.existsByUserAndGroup(paidBy, group)) {
            throw new RuntimeException("Payer is not a member of this group");
        }

        // Create expense
        Expense expense = new Expense();
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setPaidBy(paidBy);
        expense.setGroup(group);
        expense.setSplitType(request.getSplitType());

        Expense savedExpense = expenseRepository.save(expense);

        // Handle expense shares based on split type
        if (request.getSplitType() == SplitType.EQUAL) {
            createEqualShares(savedExpense, group);
        } else if (request.getSplitType() == SplitType.CUSTOM) {
            createCustomShares(savedExpense, request.getShares());
        }

        // Return the complete expense response
        return convertToExpenseResponse(savedExpense);
    }

    private void createEqualShares(Expense expense, Group group) {
        List<UserGroup> groupMembers = userGroupRepository.findByGroup(group);
        double shareAmount = expense.getAmount() / groupMembers.size();

        for (UserGroup userGroup : groupMembers) {
            ExpenseShare share = new ExpenseShare();
            share.setExpense(expense);
            share.setUser(userGroup.getUser());
            share.setShareAmount(Math.round(shareAmount * 100.0) / 100.0); // Round to 2 decimal places
            expenseShareRepository.save(share);
        }
    }

    private void createCustomShares(Expense expense, List<CreateExpenseRequest.ExpenseShareDto> shareDtos) {
        if (shareDtos == null || shareDtos.isEmpty()) {
            throw new RuntimeException("Custom split requires share information");
        }

        double totalShares = shareDtos.stream().mapToDouble(CreateExpenseRequest.ExpenseShareDto::getShareAmount).sum();
        if (Math.abs(totalShares - expense.getAmount()) > 0.01) {
            throw new RuntimeException("Total shares must equal expense amount");
        }

        for (CreateExpenseRequest.ExpenseShareDto shareDto : shareDtos) {
            User user = userRepository.findById(shareDto.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + shareDto.getUserId()));

            ExpenseShare share = new ExpenseShare();
            share.setExpense(expense);
            share.setUser(user);
            share.setShareAmount(shareDto.getShareAmount());
            expenseShareRepository.save(share);
        }
    }

    public List<ExpenseResponse> getExpensesByGroup(Long groupId, User currentUser) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!userGroupRepository.existsByUserAndGroup(currentUser, group)) {
            throw new RuntimeException("You are not a member of this group");
        }

        List<Expense> expenses = expenseRepository.findByGroupOrderByCreatedAtDesc(group);
        return expenses.stream()
                .map(this::convertToExpenseResponse)
                .collect(Collectors.toList());
    }

    // ADD BALANCE CALCULATION METHOD
    public Map<String, Object> calculateGroupBalances(Long groupId, User currentUser) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!userGroupRepository.existsByUserAndGroup(currentUser, group)) {
            throw new RuntimeException("You are not a member of this group");
        }

        List<UserGroup> groupMembers = userGroupRepository.findByGroup(group);
        Map<User, Double> netBalances = new HashMap<>();

        // Initialize all members with zero balance
        for (UserGroup userGroup : groupMembers) {
            netBalances.put(userGroup.getUser(), 0.0);
        }

        // Calculate net balances from all expenses in the group
        List<Expense> expenses = expenseRepository.findByGroup(group);
        for (Expense expense : expenses) {
            User paidBy = expense.getPaidBy();
            List<ExpenseShare> shares = expenseShareRepository.findByExpense(expense);

            // Add the full amount to payer (they get money back)
            netBalances.put(paidBy, netBalances.get(paidBy) + expense.getAmount());

            // Subtract each share from the participants
            for (ExpenseShare share : shares) {
                User participant = share.getUser();
                netBalances.put(participant, netBalances.get(participant) - share.getShareAmount());
            }
        }

        // Simplify balances (this fixes the bug)
        List<Map<String, Object>> simplifiedBalances = simplifyBalances(netBalances);

        // Format net balances for response
        List<Map<String, Object>> formattedNetBalances = netBalances.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> balance = new HashMap<>();
                    balance.put("user", Map.of(
                            "id", entry.getKey().getId(),
                            "name", entry.getKey().getName(),
                            "email", entry.getKey().getEmail()
                    ));
                    balance.put("balance", Math.round(entry.getValue() * 100.0) / 100.0);
                    return balance;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("netBalances", formattedNetBalances);
        result.put("simplifiedBalances", simplifiedBalances);

        return result;
    }

    // ADD SIMPLIFY BALANCES METHOD
    private List<Map<String, Object>> simplifyBalances(Map<User, Double> netBalances) {
        List<Map<String, Object>> simplified = new ArrayList<>();

        // Separate creditors and debtors
        List<Map.Entry<User, Double>> creditors = netBalances.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.01)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        List<Map.Entry<User, Double>> debtors = netBalances.entrySet().stream()
                .filter(entry -> entry.getValue() < -0.01)
                .sorted((a, b) -> Double.compare(a.getValue(), b.getValue()))
                .collect(Collectors.toList());

        // Distribute debts to creditors
        int creditorIndex = 0;
        for (Map.Entry<User, Double> debtorEntry : debtors) {
            User debtor = debtorEntry.getKey();
            double debt = -debtorEntry.getValue(); // Convert to positive

            while (debt > 0.01 && creditorIndex < creditors.size()) {
                Map.Entry<User, Double> creditorEntry = creditors.get(creditorIndex);
                User creditor = creditorEntry.getKey();
                double credit = creditorEntry.getValue();

                double amount = Math.min(debt, credit);

                // Create simplified balance entry
                Map<String, Object> balance = new HashMap<>();
                balance.put("from", Map.of(
                        "id", debtor.getId(),
                        "name", debtor.getName(),
                        "email", debtor.getEmail()
                ));
                balance.put("to", Map.of(
                        "id", creditor.getId(),
                        "name", creditor.getName(),
                        "email", creditor.getEmail()
                ));
                balance.put("amount", Math.round(amount * 100.0) / 100.0);
                simplified.add(balance);

                // Update amounts
                debt -= amount;
                creditors.set(creditorIndex, Map.entry(creditor, credit - amount));

                // Move to next creditor if current one is fully used
                if (credit - amount <= 0.01) {
                    creditorIndex++;
                }
            }
        }

        return simplified;
    }

    private ExpenseResponse convertToExpenseResponse(Expense expense) {
        if (expense == null) {
            return null;
        }

        List<ExpenseShare> shares = expenseShareRepository.findByExpense(expense);

        List<ExpenseResponse.ExpenseShareDto> shareDtos = shares.stream()
                .map(share -> {
                    if (share == null || share.getUser() == null) {
                        return null;
                    }
                    return new ExpenseResponse.ExpenseShareDto(
                            new ExpenseResponse.UserDto(
                                    share.getUser().getId(),
                                    share.getUser().getName(),
                                    share.getUser().getEmail()
                            ),
                            share.getShareAmount()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Ensure paidBy is not null
        User paidBy = expense.getPaidBy();
        if (paidBy == null) {
            throw new RuntimeException("Expense paidBy user is null for expense: " + expense.getId());
        }

        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                new ExpenseResponse.UserDto(
                        paidBy.getId(),
                        paidBy.getName(),
                        paidBy.getEmail()
                ),
                new ExpenseResponse.GroupDto(
                        expense.getGroup().getId(),
                        expense.getGroup().getName()
                ),
                expense.getSplitType(),
                expense.getCreatedAt(),
                shareDtos
        );
    }

    public void deleteExpense(Long expenseId, User currentUser) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        // Only allow deletion by expense creator or group admin
        if (!expense.getPaidBy().getId().equals(currentUser.getId()) &&
                !expense.getGroup().getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only delete your own expenses");
        }

        // Delete associated shares first
        List<ExpenseShare> shares = expenseShareRepository.findByExpense(expense);
        expenseShareRepository.deleteAll(shares);

        // Delete expense
        expenseRepository.delete(expense);
    }
}