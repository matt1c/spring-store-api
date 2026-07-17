package com.marsmars.services;

import com.marsmars.dtos.product.ProductRequest;
import com.marsmars.dtos.product.ProductResponse;
import com.marsmars.models.Product;
import com.marsmars.repositories.ProductRepository;
import com.marsmars.util.exceptions.ProductNotFound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<ProductResponse> findAll(int pageNum, int pageSize, String sortBy) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(sortBy).ascending());
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    public ProductResponse findOne(Long id) {
        return toResponse(productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFound("Product not found with this id")));
    }

    public void save(ProductRequest productRequest) {
        productRepository.save(toModel(productRequest));
    }

    public void update(ProductRequest productRequest, Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFound("Product not found"));

        product.setDescription(productRequest.getDescription());
        product.setName(productRequest.getName());
        product.setQuantity(productRequest.getQuantity());
        product.setPrice(productRequest.getPrice());

        productRepository.save(product);
    }

    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFound("Product not found"));
        productRepository.delete(product);
    }

    private Product toModel(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setQuantity(request.getQuantity());
        product.setPrice(request.getPrice());
        return product;
    }

    private ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setQuantity(product.getQuantity());
        response.setPrice(product.getPrice());
        return response;
    }
}
