package com.hammer.ecommerce.service;

import com.hammer.ecommerce.dto.PaymentResponseDTO;
import com.hammer.ecommerce.dto.ProcessPaymentRequestDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.*;
import com.hammer.ecommerce.repositories.OrderRepository;
import com.hammer.ecommerce.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final Random random = new Random();

    @Transactional
    public PaymentResponseDTO processPayment(Long userId, ProcessPaymentRequestDTO request) {

        // Buscar pedido
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));

        // Verifica se o pedido pertence ao usuário
        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("Pedido não pertence ao usuário");
        }

        // Verifica se pedido já tem pagamento
        if (paymentRepository.existsByOrderId(order.getId())) {
            throw new BusinessException("Pedido já possui pagamento processado");
        }

        // Verificar status do pedido
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Apenas pedidos pendentes podem receber pagamento");
        }

        // Criar pagamento
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);

        // Simular processamento do pagamento
        boolean paymentApproved = simulatePaymentProcessing(request);

        if (paymentApproved) {
            payment.setStatus(PaymentStatus.APPROVED);
            payment.setPaidAt(LocalDateTime.now());

            // Atualizar status do pedido
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
        } else {
            payment.setStatus(PaymentStatus.REJECTED);

            // Manter pedido como PENDING para tentar novamente
        }

        payment = paymentRepository.save(payment);
        return convertToDTO(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO findByOrderId(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));

        // Verificar se o pedido pertence ao usuário
        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("Pedido não pertence ao usuário");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado para este pedido"));

        return convertToDTO(payment);
    }

    @Transactional
    public PaymentResponseDTO refundPayment(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));

        // Verificar se o pedido pertence ao usuário
        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("Pedido não pertence ao usuário");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));

        // Validar se pode reembolsar
        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new BusinessException("Apenas pagamentos aprovados podem ser reembolsados");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Não é possível reembolsar pedido já entregue");
        }

        // Processar reembolso
        payment.setStatus(PaymentStatus.REFUNDED);
        payment = paymentRepository.save(payment);

        // Cancelar pedido
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return convertToDTO(payment);
    }

    // Método para simular processamento de pagamento
    // Em produção, aqui seria a integração com gateway real (Stripe, PagSeguro, etc...)
    private boolean simulatePaymentProcessing(ProcessPaymentRequestDTO request) {

        // Simular processamento com 90% de aprovação
        try {
            Thread.sleep(1000); // Simular delay de processamento
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Validações básicas para simulação
        if (request.getPaymentMethod() == PaymentMethod.CREDIT_CARD ||
                request.getPaymentMethod() == PaymentMethod.DEBIT_CARD) {

            if (request.getCardNumber() == null || request.getCardNumber().length() < 16) {
                return false;
            }
        }

        // 90% de chance de aprovação
        return random.nextInt(100) < 90;
    }

    private PaymentResponseDTO convertToDTO(Payment payment) {
        PaymentResponseDTO dto = new PaymentResponseDTO();
        dto.setId(payment.getId());
        dto.setOrderId(payment.getOrder().getId());
        dto.setOrderNumber(payment.getOrder().getOrderNumber());
        dto.setPaymentMethod(payment.getPaymentMethod().name());
        dto.setStatus(payment.getStatus().name());
        dto.setTransactionId(payment.getTransactionId());
        dto.setPaidAt(payment.getPaidAt());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }
}
