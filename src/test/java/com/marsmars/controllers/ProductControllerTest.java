package com.marsmars.controllers;

import com.marsmars.dtos.product.ProductRequest;
import com.marsmars.dtos.product.ProductResponse;
import com.marsmars.models.Product;
import com.marsmars.models.Role;
import com.marsmars.models.User;
import com.marsmars.security.UserDetailsImpl;
import com.marsmars.services.ProductService;
import com.marsmars.util.JwtAuthFilter;
import com.marsmars.util.JwtUtil;
import com.marsmars.util.exceptions.ProductNotFound;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {
    @MockitoBean
    private ProductService productService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() throws Exception {
        User mockUser = new User();
        mockUser.setUsername("Joe");
        mockUser.setPassword("123123");
        mockUser.setEmail("joefraizer@gmail.com");
        mockUser.setEnabled(true);

        Role role = new Role("ROLE_USER");
        Role managerRole = new Role("ROLE_MANAGER");
        mockUser.setRoles(List.of(role, managerRole));

        Mockito.doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());

        this.userDetails = new UserDetailsImpl(mockUser);
    }

    // FIND ALL

    @Test
    void findAll_shouldReturnList() throws Exception {
        int pageNum = 0;
        int pageSize = 10;
        String sortBy = "id";

        ProductResponse resp1 = new ProductResponse(1L, "Hat", "Cowboy hat", 3, 249.9);
        ProductResponse resp2 = new ProductResponse(2L, "Candy", "Pack of candies", 200, 150.9);
        ProductResponse resp3 = new ProductResponse(3L, "Boxing gloves", "gloves for boxing, been used", 1, 20.99);

        List<ProductResponse> content = List.of(resp1, resp2, resp3);
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(sortBy).ascending());
        Page<ProductResponse> page = new PageImpl<>(content, pageable, content.size());

        Mockito.when(productService.findAll(pageNum, pageSize, sortBy))
                .thenReturn(page);

        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                .with(user(userDetails))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }

    // FIND ONE

    @Test
    void findOne_shouldReturnResponse() throws Exception {
        Long productId = 1L;

        ProductResponse resp = new ProductResponse();
        resp.setId(1L);
        resp.setPrice(2.0);
        resp.setQuantity(3);
        resp.setName("Hat");
        resp.setDescription("Cowboy hat");

        Mockito.when(productService.findOne(productId)).thenReturn(resp);

        mockMvc.perform(get("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user(userDetails))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNotEmpty());
    }

    // CREATE

    @Test
    void create_shouldReturnCreated_andString() throws Exception {
        ProductRequest req = new ProductRequest();
        req.setName("some product name");
        req.setDescription("some product description");
        req.setPrice(10.1);
        req.setQuantity(100);

        Mockito.doNothing().when(productService).save(req);

        mockMvc.perform(post("/api/products")
                .with(user(userDetails))
                        .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Product has been added in catalog"));
    }

    @Test
    void create_shouldReturnStatus_whenRequestInvalid() throws Exception {
        ProductRequest req = new ProductRequest();

        Mockito.doNothing().when(productService).save(req);

        mockMvc.perform(post("/api/products")
                .with(user(userDetails))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // UPDATE

    @Test
    void update_shouldReturnStatus_whenRequestValid() throws Exception {
        Long productId = 1L;
        ProductRequest req = new ProductRequest();
        req.setName("some product name");
        req.setDescription("some product description");
        req.setPrice(10.1);
        req.setQuantity(100);

        Mockito.doNothing().when(productService).update(req, productId);

        mockMvc.perform(put("/api/products/{id}", productId)
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Product has been updated in catalog"));
    }

    @Test
    void update_shouldReturnStatus_whenRequestInvalid() throws Exception {
        Long productId = 1L;
        ProductRequest req = new ProductRequest();

        Mockito.doNothing().when(productService).update(req, productId);

        mockMvc.perform(put("/api/products/{id}", productId)
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturnStatus_andString_whenRequestIsEdgeCase() throws Exception {
        Long productId = 1L;
        ProductRequest req = new ProductRequest();
        req.setName("some product name");
        req.setDescription("some product description");
        req.setPrice(Double.MAX_VALUE + 100.23);
        req.setQuantity(1928371821);

        Mockito.doNothing().when(productService).update(req, productId);

        mockMvc.perform(put("/api/products/{id}", productId)
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Product has been updated in catalog"));
    }

    @Test
    void delete_shouldReturnStatus_whenProductExists() throws Exception {
        Long productId = 1L;

        Product product = new Product();
        product.setQuantity(100);
        product.setName("some name");
        product.setDescription("some description");
        product.setId(1L);
        product.setPrice(123123.1233123);

        Mockito.doNothing().when(productService).delete(productId);

        mockMvc.perform(delete("/api/products/{id}", productId)
                .with(user(userDetails))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_shouldReturnNotFound_whenProductDoesNotExist() throws Exception {
        Long nonExistingId = 1L;

        Mockito.doThrow(new ProductNotFound("Product not found"))
                .when(productService).delete(nonExistingId);

        mockMvc.perform(delete("/api/products/{id}", nonExistingId)
                .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().isNotFound());

        Mockito.verify(productService).delete(nonExistingId);
    }
}
