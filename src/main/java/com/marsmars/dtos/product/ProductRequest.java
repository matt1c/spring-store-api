package com.marsmars.dtos.product;

import com.marsmars.util.Category;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @Min(value = 0, message = "Quantity must be 1 or greater")
    private Integer quantity;

    @Min(value = 1/10, message = "Price can't be less than 1 cent")
    private BigDecimal price;

    @NotNull(message = "Category can't be empty")
    private Category category;
}
