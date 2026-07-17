package com.marsmars.controllers;

import com.marsmars.dtos.product.ProductRequest;
import com.marsmars.dtos.product.ProductResponse;
import com.marsmars.services.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Page<ProductResponse> findAll(@RequestParam(name = "page", defaultValue = "0") int pageNum,
                                         @RequestParam(name = "size", defaultValue = "10") int pageSize,
                                         @RequestParam(name = "sort", defaultValue = "id") String sortBy) {
        return productService.findAll(pageNum, pageSize, sortBy);
    }

    @GetMapping("/{id}")
    public ProductResponse findOne(@PathVariable("id") Long id) {
        return productService.findOne(id);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public ResponseEntity<String> create(@RequestBody @Valid ProductRequest request) {
        productService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Product has been added in catalog");
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<String> update(@RequestBody @Valid ProductRequest request,
                                         @PathVariable("id") Long id) {
        productService.update(request, id);
        return ResponseEntity.status(HttpStatus.OK).body("Product has been updated in catalog");
    }

    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable("id") Long id) {
        productService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body("Product has been deleted from catalog");
    }
}
