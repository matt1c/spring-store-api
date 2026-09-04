package com.marsmars.services;

import com.marsmars.dtos.user.UserResponse;
import com.marsmars.models.Order;
import com.marsmars.models.OrderItem;
import com.marsmars.models.Role;
import com.marsmars.models.User;
import com.marsmars.repositories.RoleRepository;
import com.marsmars.repositories.UserRepository;
import com.marsmars.util.OrderStatus;
import com.marsmars.util.exceptions.UserAlreadyBanOrUnbanned;
import com.marsmars.util.exceptions.UserNotFound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @InjectMocks
    private AdminService adminService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("John Doe");
        user.setPassword("password");
        user.setEmail("johndoe@gmail.com");
        user.setEnabled(true);
        user.setOrders(Collections.emptySet());
        user.setRoles(Set.of(new Role("ROLE_USER"), new Role("ROLE_ADMIN")));
    }

    // FIND ALL
    @Test
    void findAll_shouldReturnPage_whenPageNotEmpty() {
        // Arrange
        List<User> responses = Collections.singletonList(user);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(responses, pageable, responses.size());

        when(userRepository.findAll(pageable))
                .thenReturn(page);
        // Act
        Page<UserResponse> result = adminService.findAll(0, 10);

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageableCaptor.capture());

        Pageable pageableForAssert = pageableCaptor.getValue();
        assertEquals(0, pageableForAssert.getPageNumber());
        assertEquals(10, pageableForAssert.getPageSize());
        assertEquals(1, result.getTotalElements());
        assertEquals(user.getUsername(), result.getContent().getFirst().getUsername());
        ;
        assertEquals(user.getEmail(), result.getContent().getFirst().getEmail());
    }

    @Test
    void findAll_shouldReturnPage_whenPageIsEmpty() {
        // Arrange
        List<User> responses = Collections.emptyList();
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(responses, pageable, 0);

        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(page);
        // Act
        Page<UserResponse> result = adminService.findAll(0, 10);

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageableCaptor.capture());

        Pageable pageableForAssert = pageableCaptor.getValue();
        assertEquals(0, pageableForAssert.getPageNumber());
        assertEquals(10, pageableForAssert.getPageSize());
        assertEquals(0, result.getTotalElements());
    }

    // FIND ONE
    @Test
    void findOne_shouldReturnUser_whenUserIsExisting() {
        // Arrange
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        // Act
        UserResponse result = adminService.findOne(1L);

        // Assert
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getRoles().stream().map(Role::getName).toList(), result.getRoles());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void findOne_shouldThrowException_whenUserNotExisting() {
        // Arrange
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act && Assert
        UserNotFound ex = assertThrows(UserNotFound.class, () -> adminService.findOne(999L));
        assertEquals("User not found with this id", ex.getMessage());
        verify(userRepository, times(1)).findById(999L);
    }

    // BAN
    @Test
    void banUser_shouldBanUser_whenUserIsEnabled() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        adminService.banUser(1L);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User userForAssert = userCaptor.getValue();
        assertFalse(userForAssert.isEnabled());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void banUser_shouldThrowException_whenUserIsDisabled() {
        // Arrange
        user.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act & Assert
        UserAlreadyBanOrUnbanned ex = assertThrows(UserAlreadyBanOrUnbanned.class, () -> adminService.banUser(1L));
        assertEquals("User is already banned", ex.getMessage());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void banUser_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFound ex = assertThrows(UserNotFound.class, () -> adminService.banUser(999L));
        assertEquals("User not found for banning", ex.getMessage());
        verify(userRepository, times(1)).findById(999L);
    }

    // UNBAN
    @Test
    void unbanUser_shouldUnbanUser_whenUserIsDisabled() {
        // Arrange
        user.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        adminService.unbanUser(user.getId());

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User userForAssert = userCaptor.getValue();
        assertTrue(userForAssert.isEnabled());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void unbanUser_shouldThrowException_whenUserIsEnabled() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act & Assert
        UserAlreadyBanOrUnbanned ex = assertThrows(UserAlreadyBanOrUnbanned.class,
                () -> adminService.unbanUser(1L));
        assertEquals("User is already unbanned", ex.getMessage());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void unbanUser_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFound ex = assertThrows(UserNotFound.class, () -> adminService.unbanUser(999L));
        assertEquals("User not found for banning", ex.getMessage());
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    void bulkDiscountToUserOrders_UserNotFound_ThrowsException() {
        when(userRepository.findWithDetailsById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFound.class, () -> adminService.bulkDiscountToUserOrders(1L));
    }

    @Test
    void bulkDiscountToUserOrders_ValidOrders_AppliesDiscountCorrectly() {
        OrderItem item1 = new OrderItem();
        item1.setPriceAtOrder(new BigDecimal("150.00"));

        OrderItem item2 = new OrderItem();
        item2.setPriceAtOrder(new BigDecimal("300.00"));

        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setItems(Set.of(item1, item2));

        User user = new User();
        user.setId(1L);
        user.setOrders(Set.of(order));

        when(userRepository.findWithDetailsById(1L)).thenReturn(Optional.of(user));

        adminService.bulkDiscountToUserOrders(1L);

        assertEquals(new BigDecimal("100.00"), item1.getPriceAtOrder());
        assertEquals(new BigDecimal("200.00"), item2.getPriceAtOrder());
        assertEquals(new BigDecimal("300.00"), order.getTotalSum());
    }

    @Test
    void bulkDiscountToUserOrders_IgnoresOrdersWithNonMatchingStatus() {
        OrderItem item = new OrderItem();
        item.setPriceAtOrder(new BigDecimal("150.00"));

        Order order = new Order();
        order.setStatus(OrderStatus.CANCELLED);
        order.setItems(Set.of(item));

        User user = new User();
        user.setId(1L);
        user.setOrders(Set.of(order));

        when(userRepository.findWithDetailsById(1L)).thenReturn(Optional.of(user));

        adminService.bulkDiscountToUserOrders(1L);

        assertEquals(new BigDecimal("150.00"), item.getPriceAtOrder());
        assertNull(order.getTotalSum());
    }

    @Test
    void bulkDiscountToUserOrders_IgnoresOrdersWithDeliveredAt() {
        OrderItem item = new OrderItem();
        item.setPriceAtOrder(new BigDecimal("150.00"));

        Order order = new Order();
        order.setDeliveredAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setItems(Set.of(item));

        User user = new User();
        user.setId(1L);
        user.setOrders(Set.of(order));

        when(userRepository.findWithDetailsById(1L)).thenReturn(Optional.of(user));

        adminService.bulkDiscountToUserOrders(1L);

        assertEquals(new BigDecimal("150.00"), item.getPriceAtOrder());
        assertNull(order.getTotalSum());
    }
}
