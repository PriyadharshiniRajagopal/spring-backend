package com.example.SplitEase.dto;

import com.example.SplitEase.model.SplitType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private String description;
    private Double amount;
    private UserDto paidBy;
    private GroupDto group;
    private SplitType splitType;
    private LocalDateTime createdAt;
    private List<ExpenseShareDto> shares;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private Long id;
        private String name;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupDto {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseShareDto {
        private UserDto user;
        private Double shareAmount;
    }
}