package com.marsmars.services;

import com.marsmars.dtos.order.OrderItemRequest;
import com.marsmars.dtos.order.OrderRequest;
import com.marsmars.models.Order;
import com.marsmars.models.OrderItem;
import com.marsmars.models.Product;
import com.marsmars.models.User;
import com.marsmars.repositories.OrderRepository;
import com.marsmars.repositories.ProductRepository;
import com.marsmars.repositories.UserRepository;
import com.marsmars.util.OrderStatus;
import com.marsmars.util.exceptions.InsufficientStockException;
import com.marsmars.util.exceptions.OrderNotFound;
import com.marsmars.util.exceptions.ProductNotFound;
import com.marsmars.util.exceptions.UserNotFound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;
    private OrderRequest orderRequest;
    private OrderItemRequest itemRequest;
    private Order order;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        product = new Product();
        product.setId(10L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("100.00"));
        product.setQuantity(10);

        itemRequest = new OrderItemRequest();
        itemRequest.setProductId(10L);
        itemRequest.setQuantity(2);

        orderRequest = new OrderRequest();
        orderRequest.setUserId(1L);
        orderRequest.setItems(List.of(itemRequest));

        order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setUser(user);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalSum(BigDecimal.valueOf(200.00).setScale(2, RoundingMode.HALF_UP));
        order.setItems(Set.of(new OrderItem(1L, order, product,
                5, product.getPrice())));
    }

    @Test
    void save_shouldSaveOrder_whenOrderIsValid() {
        // Arrange
        BigDecimal oldPriceAtOrder = order.getItems().iterator().next().getPriceAtOrder();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        // Act
        orderService.save(orderRequest);

        // Assert: проверяем, что количество товара уменьшилось с 10 до 8
        assertEquals(8, product.getQuantity());
        product.setPrice(BigDecimal.valueOf(12.0));
        verify(productRepository, times(1)).save(product);

        // Assert: захватываем сохраненный заказ и проверяем его свойства
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertNotNull(savedOrder);
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
        assertEquals(user, savedOrder.getUser());
        assertEquals(1, savedOrder.getItems().size());
        assertEquals(oldPriceAtOrder, savedOrder.getItems().iterator().next().getPriceAtOrder());
    }

    @Test
    void save_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        UserNotFound ex = assertThrows(UserNotFound.class, () -> {
            orderService.save(orderRequest);
        });

        // Assert
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void save_shouldThrowException_whenProductNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        // Act
        ProductNotFound ex = assertThrows(ProductNotFound.class, () -> orderService.save(orderRequest));

        // Assert
        assertEquals("Product not found", ex.getMessage());
    }

    @Test
    void save_shouldThrowException_whenProductQuantityIsInsufficient() {
        // Arrange
        product.setQuantity(1);
        itemRequest.setQuantity(10);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        // Act
        InsufficientStockException ex = assertThrows(InsufficientStockException.class, () ->
                orderService.save(orderRequest));

        // Assert
        assertEquals("Not enough stock for Test Product: requested 10, available 1", ex.getMessage());
    }

    @Test
    void changeStatus_withCancelStatus() {
        // Arrange
        OrderItem orderItem = order.getItems().iterator().next();
        int expectedQuantity = product.getQuantity() + orderItem.getQuantity();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act
        orderService.changeStatus(1L, OrderStatus.CANCELLED);

        // Assert
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(productRepository, times(1)).save(productCaptor.capture());
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        Product productForAssert = productCaptor.getValue();
        Order orderForAssert = orderCaptor.getValue();

        assertEquals(expectedQuantity, productForAssert.getQuantity());
        assertEquals(product.getName(), productForAssert.getName());
        assertEquals(product.getDescription(), productForAssert.getDescription());
        assertEquals(product.getCategory(), productForAssert.getCategory());
        assertEquals(product.getPrice(), productForAssert.getPrice());

        assertEquals(OrderStatus.CANCELLED, orderForAssert.getStatus());
    }

    @Test
    void changeStatus_throwException_whenOrderNotExist() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        OrderNotFound ex = assertThrows(OrderNotFound.class, () ->
                orderService.changeStatus(1L, OrderStatus.DELIVERED));

        // Assert
        assertEquals("Order not found for changing status", ex.getMessage());
    }

    @Test
    void changeStatus_whenDeliveredStatus() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act
        orderService.changeStatus(1L, OrderStatus.DELIVERED);

        // Assert
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        Order orderForAssert = orderCaptor.getValue();

        assertNotNull(orderForAssert.getDeliveredAt());
        assertEquals(OrderStatus.DELIVERED, orderForAssert.getStatus());
        assertEquals(order.getCreatedAt(), orderForAssert.getCreatedAt());
        assertEquals(user, orderForAssert.getUser());
        assertEquals(order.getItems(), orderForAssert.getItems());
        assertEquals(order.getTotalSum(), orderForAssert.getTotalSum());
    }
}