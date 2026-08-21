package com.marsmars.controllers;

import com.marsmars.dtos.order.OrderItemRequest;
import com.marsmars.dtos.order.OrderItemResponse;
import com.marsmars.dtos.order.OrderRequest;
import com.marsmars.dtos.order.OrderResponse;
import com.marsmars.models.Role;
import com.marsmars.models.User;
import com.marsmars.security.UserDetailsImpl;
import com.marsmars.services.OrderService;
import com.marsmars.services.UserDetailsServiceImpl;
import com.marsmars.util.JwtUtil;
import com.marsmars.util.OrderStatus;
import com.marsmars.util.exceptions.OrderNotFound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    // FIND ALL
    @Test
    void findAll_shouldReturnPage_whenOrdersAreExisting() throws Exception {
        OrderItemResponse itemResponse = new OrderItemResponse(1L, "sponge", 3, BigDecimal.valueOf(0.5));
        List<OrderResponse> responses = List.of(new OrderResponse(5L, OrderStatus.IN_PROGRESS.name(),
                LocalDateTime.now(), null, 1L, Collections.singletonList(itemResponse), BigDecimal.valueOf(0.5)));
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponse> page = new PageImpl<>(responses, pageable, responses.size());

        when(orderService.findAll(1L, 0, 10))
                .thenReturn(page);

        mockMvc.perform(get("/api/orders")
                        .with(user(customUserDetails))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.content[0].userId").value(1))
                .andExpect(jsonPath("$.content[0].totalSum").value(0.5));

        verify(orderService, times(1)).findAll(1L, 0, 10);
    }

    @Test
    void findAll_shouldReturnPage_whenOrdersAreEmpty() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponse> page = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(orderService.findAll(1L, 0, 10))
                .thenReturn(page);

        mockMvc.perform(get("/api/orders")
                        .with(user(customUserDetails))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isEmpty());

        verify(orderService, times(1)).findAll(1L, 0, 10);
    }

    @Test
    void findAll_shouldReturnUnauthorized_whenUserNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    // FIND ONE
    @Test
    void findOne_shouldReturnOrder_whenOrderIsExisting() throws Exception {
        OrderResponse resp = new OrderResponse(1L, OrderStatus.PENDING.name(), LocalDateTime.now(), null,
                1L, Collections.emptyList(), BigDecimal.ZERO);
        when(orderService.findOne(5L, 1L))
                .thenReturn(resp);

        mockMvc.perform(get("/api/orders/{id}", 5L)
                        .with(user(customUserDetails))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING.name()))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalSum").value(resp.getTotalSum()));

        verify(orderService, times(1)).findOne(5L, 1L);
    }

    @Test
    void findOne_shouldThrowException_whenOrderNotExisting() throws Exception {
        when(orderService.findOne(eq(123L), eq(1L)))
                .thenThrow(OrderNotFound.class);

        mockMvc.perform(get("/api/orders/{id}", 123L)
                        .with(user(customUserDetails))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertInstanceOf(OrderNotFound.class, res.getResolvedException()));

        verify(orderService, times(1)).findOne(eq(123L), eq(1L));
    }

    @Test
    void findOne_shouldThrowException_withInvalidId() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", "abc")
                        .with(user(customUserDetails))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertInstanceOf(MethodArgumentTypeMismatchException.class, res.getResolvedException()));

        verifyNoInteractions(orderService);
    }

    // CREATE
    @Test
    void create_shouldReturnOk_withValidRequest() throws Exception {
        OrderItemRequest itemReq = new OrderItemRequest(5L, 3);
        OrderRequest req = new OrderRequest(1L, List.of(itemReq));

        mockMvc.perform(post("/api/orders")
                        .with(user(customUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string("Order has been created"));

        ArgumentCaptor<OrderRequest> captor = ArgumentCaptor.forClass(OrderRequest.class);
        verify(orderService).save(captor.capture());
        OrderRequest reqForAssert = captor.getValue();

        assertEquals(req.getUserId(), reqForAssert.getUserId());
        assertEquals(req.getItems(), reqForAssert.getItems());
    }

    @Test
    void create_shouldThrowException_withInvalidRequest() throws Exception {
        OrderRequest req = new OrderRequest(1L, null);
        String jsonRequest = objectMapper.writeValueAsString(req);

        mockMvc.perform(post("/api/orders")
                        .with(user(customUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));

        verifyNoInteractions(orderService);
    }

    // CHANGE STATUS
    @Test
    void changeStatus_shouldReturnOk_whenStatusIsExisting() throws Exception {
        doNothing().when(orderService).changeStatus(1L, OrderStatus.COMING);

        mockMvc.perform(put("/api/orders/{id}/status/{status}", 1L, "COMING")
                        .with(user(customUserDetails))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string("Order status changed"));

        verify(orderService, times(1)).changeStatus(1L, OrderStatus.COMING);
    }

    @Test
    void changeStatus_shouldThrowException_whenStatusIsNotExisting() throws Exception {
        mockMvc.perform(put("/api/orders/{id}/status/{status}", 1L, "STRING")
                        .with(user(customUserDetails))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }
}
