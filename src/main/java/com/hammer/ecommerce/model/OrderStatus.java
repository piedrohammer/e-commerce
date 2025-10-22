package com.hammer.ecommerce.model;

public enum OrderStatus {
    PENDING,      // Aguardando pagamento
    PAID,         // Pago
    PROCESSING,   // Em processamento
    SHIPPED,      // Enviado
    DELIVERED,    // Entregue
    CANCELLED     // Cancelado
}