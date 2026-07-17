package com.marsmars.controllers;

import com.marsmars.dtos.order.OrderItemRequest;
import com.marsmars.dtos.order.OrderRequest;
import com.marsmars.dtos.order.OrderResponse;
import com.marsmars.models.Role;
import com.marsmars.models.User;
import com.marsmars.security.UserDetailsImpl;
import com.marsmars.services.OrderService;
import com.marsmars.services.UserDetailsServiceImpl;
import com.marsmars.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(OrderController.class)
public class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDetailsImpl customUserDetails;

    @BeforeEach
    void setUp() {
        User mockUser = new User(1L, "Bob", "123123", "bob@gmail.com", true);
        Role role = new Role("ROLE_USER");
        mockUser.getRoles().add(role);
        this.customUserDetails = new UserDetailsImpl(mockUser);
    }

    @Test
    void findAll_shouldReturnListOfOrders() throws Exception {
        Long userId = 1L;
        OrderResponse resp = new OrderResponse();
        int pageNumber = 0;
        int pageSize = 10;

        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponse> page = new PageImpl<>(Collections.singletonList(resp), pageable, 1);

        Mockito.when(orderService.findAll(userId, pageNumber, pageSize)).thenReturn(page);

        mockMvc.perform(get("/api/orders")
                .with(user(customUserDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void findOne_shouldReturnOrder() throws Exception {
        Long userId = 1L;
        OrderResponse resp = new OrderResponse();
        Mockito.when(orderService.findOne(1L, userId)).thenReturn(resp);

        mockMvc.perform(get("/api/orders/{id}",1L)
                .with(user(customUserDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    void create_shouldReturnString_whenRequestIsValid() throws Exception {
        OrderRequest req = new OrderRequest();
        Long userId = 1L;
        req.setUserId(userId);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(1);
        req.setItems(Collections.singletonList(item));

        Mockito.doNothing().when(orderService).save(any(OrderRequest.class));

        mockMvc.perform(post("/api/orders")
                .with(user(customUserDetails))
                        .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Order has been created"));
    }

    @Test
    void create_shouldReturnStatus_whenRequestIsInvalid() throws Exception {
        OrderRequest req = new OrderRequest();

        mockMvc.perform(post("/api/orders")
                .with(user(customUserDetails))
                        .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isBadRequest());
    }
}
