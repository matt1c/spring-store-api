package com.marsmars.services;

import com.marsmars.dtos.order.OrderItemRequest;
import com.marsmars.dtos.order.OrderItemResponse;
import com.marsmars.dtos.order.OrderRequest;
import com.marsmars.dtos.order.OrderResponse;
import com.marsmars.models.Order;
import com.marsmars.models.OrderItem;
import com.marsmars.models.Product;
import com.marsmars.repositories.OrderRepository;
import com.marsmars.repositories.ProductRepository;
import com.marsmars.repositories.UserRepository;
import com.marsmars.util.OrderStatus;
import com.marsmars.util.exceptions.InsufficientStockException;
import com.marsmars.util.exceptions.OrderNotFound;
import com.marsmars.util.exceptions.ProductNotFound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public Page<OrderResponse> findAll(Long ownerId, int number, int size) {
        Pageable pageable = PageRequest.of(number, size);
        return orderRepository.findAllByUserId(ownerId, pageable).map(this::toResponse);
    }

    public OrderResponse findOne(Long orderId, Long ownerId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFound("Order not found"));
        if (Objects.equals(order.getUser().getId(), ownerId))
            return toResponse(order);
        throw new OrderNotFound("Order not found");
    }

    @Transactional
    public void save(OrderRequest orderRequest) {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUser(userRepository.findById(orderRequest.getUserId()).orElseThrow());

        BigDecimal totalSum = new BigDecimal(0);
        for (OrderItemRequest itemReq : orderRequest.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ProductNotFound("Product not found"));

            if (product.getQuantity() < itemReq.getQuantity()) {
                throw new InsufficientStockException(
                        "Not enough stock for " + product.getName()
                                + ": requested " + itemReq.getQuantity()
                                + ", available " + product.getQuantity());
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setPriceAtOrder(product.getPrice());
            order.getItems().add(item);

            totalSum = totalSum.add(product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);
        }

        order.setTotalSum(totalSum);
        orderRepository.save(order);
    }

    @Transactional
    public void changeStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFound("Order not found for changing status"));

        OrderStatus oldStatus = order.getStatus();

        if (status == OrderStatus.CANCELLED && oldStatus != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setQuantity(product.getQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setStatus(status);
        if (status == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        orderRepository.save(order);
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setStatus(order.getStatus().name());
        response.setCreatedAt(order.getCreatedAt());
        response.setDeliveredAt(order.getDeliveredAt());
        response.setTotalSum(order.getTotalSum());

        if (order.getUser() != null) {
            response.setUserId(order.getUser().getId());
        }

        if (order.getItems() != null) {
            response.setItems(order.getItems().stream().map(item -> {
                OrderItemResponse itemResponse = new OrderItemResponse();
                itemResponse.setProductId(item.getProduct().getId());
                itemResponse.setProductName(item.getProduct().getName());
                itemResponse.setQuantity(item.getQuantity());
                itemResponse.setPriceAtOrder(item.getPriceAtOrder());
                return itemResponse;
            }).toList());
        }

        return response;
    }
}
