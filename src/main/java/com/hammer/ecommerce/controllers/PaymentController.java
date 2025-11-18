package com.hammer.ecommerce.controllers;

import com.hammer.ecommerce.dto.payment.PaymentResponseDTO;
import com.hammer.ecommerce.dto.payment.ProcessPaymentRequestDTO;
import com.hammer.ecommerce.repositories.UserRepository;
import com.hammer.ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Endpoints para processamento de pagamentos")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @Operation(summary = "Processar pagamento",
            description = "Processa o pagamento de um pedido. Aceita múltiplos métodos: PIX, Cartão de Crédito, Cartão de Débito e Boleto. " +
                    "O sistema simula a aprovação (90% de chance). Quando aprovado, o pedido é automaticamente atualizado para status PAID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pagamento processado com sucesso",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Pedido já possui pagamento, status inválido ou dados de pagamento inválidos",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content)
    })
    @PostMapping("/process")
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados do pagamento. Para PIX e Boleto, apenas orderId e paymentMethod são necessários. " +
                            "Para Cartão, incluir dados do cartão (em produção, usar tokenização)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ProcessPaymentRequestDTO.class))
            )
            @Valid @RequestBody ProcessPaymentRequestDTO request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        PaymentResponseDTO payment = paymentService.processPayment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @Operation(summary = "Consultar pagamento do pedido",
            description = "Retorna os dados do pagamento de um pedido específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagamento encontrado",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado para este pedido", content = @Content),
            @ApiResponse(responseCode = "400", description = "Pedido não pertence ao usuário", content = @Content)
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponseDTO> findByOrderId(
            @Parameter(description = "ID do pedido") @PathVariable Long orderId,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        PaymentResponseDTO payment = paymentService.findByOrderId(orderId, userId);
        return ResponseEntity.ok(payment);
    }

    @Operation(summary = "Reembolsar pagamento",
            description = "Processa o reembolso de um pagamento aprovado. O pedido é automaticamente cancelado e o status do pagamento atualizado para REFUNDED. " +
                    "Não é possível reembolsar pedidos já entregues")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reembolso processado com sucesso",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Pagamento não está aprovado, pedido já entregue ou não pertence ao usuário",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado", content = @Content)
    })
    @PostMapping("/order/{orderId}/refund")
    public ResponseEntity<PaymentResponseDTO> refundPayment(
            @Parameter(description = "ID do pedido") @PathVariable Long orderId,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        PaymentResponseDTO payment = paymentService.refundPayment(orderId, userId);
        return ResponseEntity.ok(payment);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"))
                .getId();
    }
}
