package com.hammer.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingDTO {

    private Long productId;
    private Double averageRating;
    private Long totalReviews;
}
