package com.marsmars.controllers;

import com.marsmars.dtos.order.OrderRequest;
import com.marsmars.dtos.order.OrderResponse;
import com.marsmars.security.UserDetailsImpl;
import com.marsmars.services.OrderService;
import com.marsmars.util.OrderStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Page<OrderResponse> findAll(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                       @RequestParam(name = "page", defaultValue = "0") int pageNum,
                                       @RequestParam(name = "size", defaultValue = "10") int pageSize) {
        return orderService.findAll(userDetails.user().getId(), pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public OrderResponse findOne(@PathVariable("id") Long id,
                                 @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return orderService.findOne(id, Objects.requireNonNull(userDetails).user().getId());
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody @Valid OrderRequest orderRequest,
                                         @AuthenticationPrincipal UserDetailsImpl userDetails) {
        orderRequest.setUserId(userDetails.user().getId());
        orderService.save(orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("Order has been created");
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}/status/{status}")
    public ResponseEntity<String> changeStatus(@PathVariable("id") Long orderId,
                                               @PathVariable("status") String status) {
        System.out.println(OrderStatus.valueOf(status.toUpperCase()));
        orderService.changeStatus(orderId, OrderStatus.valueOf(status.toUpperCase()));
        return ResponseEntity.status(HttpStatus.OK).body("Order status changed");
    }
}
