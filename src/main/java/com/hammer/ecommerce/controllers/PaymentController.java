package com.hammer.ecommerce.controllers;

import com.hammer.ecommerce.dto.PaymentResponseDTO;
import com.hammer.ecommerce.dto.ProcessPaymentRequestDTO;
import com.hammer.ecommerce.repositories.UserRepository;
import com.hammer.ecommerce.service.PaymentService;
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
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @Valid @RequestBody ProcessPaymentRequestDTO request,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        PaymentResponseDTO payment = paymentService.processPayment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponseDTO> findByOrderId(
            @PathVariable Long orderId,
            Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        PaymentResponseDTO payment = paymentService.findByOrderId(orderId, userId);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/order/{orderId}/refund")
    public ResponseEntity<PaymentResponseDTO> refundPayment(
            @PathVariable Long orderId,
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
