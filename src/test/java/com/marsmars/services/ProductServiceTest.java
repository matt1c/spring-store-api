package com.marsmars.services;

import com.marsmars.dtos.product.ProductRequest;
import com.marsmars.dtos.product.ProductResponse;
import com.marsmars.models.Product;
import com.marsmars.repositories.ProductRepository;
import com.marsmars.util.Category;
import com.marsmars.util.exceptions.ProductNotFound;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void findAll_shouldReturnListOfProducts_whenProductsAreExisting() {
        List<Product> products =
                List.of(new Product(1L, "Pack of candies", "100 candies",
                        100, BigDecimal.valueOf(50.0), Category.GROCERIES),
                        new Product(2L, "Mouse", "game mouse logitech g102",
                                120, BigDecimal.valueOf(20), Category.GAMES));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(products, pageable, products.size());

        Mockito.when(productRepository.findAll(pageable)).thenReturn(productPage);

        Page<ProductResponse> result = productService.findAll(0, 10);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getContent().size());

        Mockito.verify(productRepository, Mockito.times(1)).findAll(pageable);
    }

    @Test
    void findAll_shouldReturnListOfProducts_whenListIsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        Mockito.when(productRepository.findAll(pageable))
                .thenReturn(productPage);

        Page<ProductResponse> result = productService.findAll(0, 10);

        Assertions.assertEquals(0, result.getContent().size());

        Mockito.verify(productRepository, Mockito.times(1)).findAll(pageable);
    }

    @Test
    void findAll_shouldThrowException_whenRepositoryFails() {
        Mockito.when(productRepository.findAll(any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error")); // Или NullPointerException, если это нужно

        Assertions.assertThrows(RuntimeException.class, () -> {
            productService.findAll(0, 10);
        });

        Mockito.verify(productRepository, Mockito.times(1)).findAll(any(Pageable.class));
    }

    @Test
    void findOne_shouldReturnProduct_whenProductIsExisting() {
        Product product = new Product(1L, "Mouse", "gaming mouse",
                10, BigDecimal.valueOf(100), Category.GAMES);

        Mockito.when(productRepository.findById(eq(product.getId())))
                .thenReturn(Optional.of(product));

        ProductResponse resp = productService.findOne(product.getId());

        Assertions.assertNotNull(resp);

        Mockito.verify(productRepository, Mockito.times(1)).findById(eq(product.getId()));
    }

    @Test
    void findOne_shouldThrowException_whenProductIsNotExisting() {
        Mockito.when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ProductNotFound ex = Assertions.assertThrows(ProductNotFound.class, () -> productService.findOne(999L));
        Assertions.assertEquals("Product not found with this id", ex.getMessage());

        Mockito.verify(productRepository, Mockito.times(1)).findById(999L);
    }

    @Test
    void save_shouldReturnProduct_whenProductIsValid() {
        ProductRequest req = new ProductRequest("Subscription", "sub for youtube premium",
                1, BigDecimal.valueOf(10), Category.OTHER);

        Mockito.when(productRepository.save(ArgumentMatchers.any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        productService.save(req);

        // Перехватываем объект, который service реально передал в repository.save()
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        Mockito.verify(productRepository).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();
        Assertions.assertEquals("Subscription", savedProduct.getName());
        Assertions.assertEquals(BigDecimal.valueOf(10), savedProduct.getPrice());
    }


    @Test
    void save_shouldThrowsException_whenRepositoryThrows() {
        ProductRequest req = new ProductRequest("", "", -1, BigDecimal.valueOf(-10), null);

        Mockito.when(productRepository.save(any(Product.class)))
                .thenThrow(new NullPointerException("Product is null"));

        Assertions.assertThrows(NullPointerException.class, () -> productService.save(req));

        Mockito.verify(productRepository).save(any(Product.class));
    }
}
