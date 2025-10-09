package com.hammer.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private String sku;
    private Boolean active;
    private CategorySummaryDTO category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer reviewCount;
    private Double averageRating;
}
