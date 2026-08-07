package com.marsmars.dtos.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private Long userId;
    private List<OrderItemResponse> items;
    private BigDecimal totalSum;
}
