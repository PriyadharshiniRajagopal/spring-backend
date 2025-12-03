package com.example.SplitEase.dto;

import com.example.SplitEase.model.SplitType;
import lombok.Data;
import java.util.List;

@Data
public class CreateExpenseRequest {
    private String description;
    private Double amount;
    private Long paidById;
    private Long groupId;
    private SplitType splitType;
    private List<ExpenseShareDto> shares;

    @Data
    public static class ExpenseShareDto {
        private Long userId;
        private Double shareAmount;
    }
}