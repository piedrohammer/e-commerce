package com.hammer.ecommerce.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDTO {

    private Long id;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private LocalDateTime createdAt;
}