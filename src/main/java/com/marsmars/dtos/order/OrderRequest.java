package com.marsmars.dtos.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {

    @JsonIgnore
    private Long userId;

    @NotNull(message = "Items are required")
    private List<OrderItemRequest> items;
}
