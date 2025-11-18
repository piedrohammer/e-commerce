package com.hammer.ecommerce.dto.order;

import com.hammer.ecommerce.dto.address.AddressResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {

    private Long id;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private AddressResponseDTO shippingAddress;
    private List<OrderItemResponseDTO> items = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}