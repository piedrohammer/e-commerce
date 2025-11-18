package com.hammer.ecommerce.dto.payment;

import com.hammer.ecommerce.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequestDTO {

    @NotNull(message = "ID do pedido é obrigatório")
    private Long orderId;

    @NotNull(message = "Método de pagamento é obrigatório")
    private PaymentMethod paymentMethod;

    // Campos opcionais para cartão (em produção, isso viria criptografado)
    private String cardNumber;
    private String cardHolderName;
    private String cardExpiryDate;
    private String cardCvv;
}
