package com.marsmars.services;

import com.marsmars.dtos.role.RoleRequest;
import com.marsmars.dtos.user.UserResponse;
import com.marsmars.models.Order;
import com.marsmars.models.OrderItem;
import com.marsmars.models.Role;
import com.marsmars.models.User;
import com.marsmars.repositories.RoleRepository;
import com.marsmars.repositories.UserRepository;
import com.marsmars.util.OrderStatus;
import com.marsmars.util.exceptions.RoleNotFound;
import com.marsmars.util.exceptions.UserAlreadyBanOrUnbanned;
import com.marsmars.util.exceptions.UserNotFound;
import com.marsmars.util.exceptions.UserRoleAlreadyTaken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public Page<UserResponse> findAll(int num, int size) {
        Pageable pageable = PageRequest.of(num, size);
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    public UserResponse findOne(Long id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new UserNotFound("User not found with this id")));
    }

    public void banUser(Long id) {
        User userToBan = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFound("User not found for banning"));
        if (!userToBan.isEnabled())
            throw new UserAlreadyBanOrUnbanned("User is already banned");
        userToBan.setEnabled(false);
        userRepository.save(userToBan);
    }

    public void unbanUser(Long id) {
        User userToUnban = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFound("User not found for banning"));
        if (userToUnban.isEnabled())
            throw new UserAlreadyBanOrUnbanned("User is already unbanned");
        userToUnban.setEnabled(true);
        userRepository.save(userToUnban);
    }

    public void assignRole(Long userId, RoleRequest roleRequest) {
        Role role = roleRepository.findByName(roleRequest.getName())
                .orElseThrow(() -> new RoleNotFound("Role not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User not found with this id"));
        if (user.getRoles().contains(role))
            throw new UserRoleAlreadyTaken("User already have this role");
        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Transactional
    public void bulkDiscountToUserOrders(Long userId) {
        User user = userRepository.findWithDetailsById(userId)
                .orElseThrow(() -> new UserNotFound("User not found"));
        Set<Order> orders = user.getOrders();

        for (Order order : orders) {
            if (order.getDeliveredAt() == null && order.getStatus() != OrderStatus.CANCELLED) {
                BigDecimal newTotalSum = BigDecimal.ZERO;

                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        if (item.getPriceAtOrder() != null) {
                            BigDecimal newPrice = item.getPriceAtOrder()
                                    .divide(new BigDecimal("1.5"), 2, RoundingMode.HALF_UP);

                            item.setPriceAtOrder(newPrice);
                            newTotalSum = newTotalSum.add(newPrice);
                        }
                    }
                }
                order.setTotalSum(newTotalSum);
            }
        }
    }

    private UserResponse toResponse(User user) {
        UserResponse resp = new UserResponse();
        resp.setId(user.getId());
        resp.setEmail(user.getEmail());
        resp.setUsername(user.getUsername());
        resp.setRoles(user.getRoles().stream().map(Role::getName).toList());
        return resp;
    }
}
