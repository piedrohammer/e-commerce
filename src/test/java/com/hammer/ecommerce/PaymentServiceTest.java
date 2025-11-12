package com.hammer.ecommerce;

import com.hammer.ecommerce.dto.PaymentResponseDTO;
import com.hammer.ecommerce.dto.ProcessPaymentRequestDTO;
import com.hammer.ecommerce.exceptions.BusinessException;
import com.hammer.ecommerce.exceptions.ResourceNotFoundException;
import com.hammer.ecommerce.model.*;
import com.hammer.ecommerce.repositories.OrderRepository;
import com.hammer.ecommerce.repositories.PaymentRepository;
import com.hammer.ecommerce.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    private User user;
    private Order order;
    private Payment payment;
    private ProcessPaymentRequestDTO paymentRequest;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setName("João Silva");

        order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-12345678");
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("300.00"));

        payment = new Payment();
        payment.setId(1L);
        payment.setOrder(order);
        payment.setPaymentMethod(PaymentMethod.PIX);
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setTransactionId("TXN-123456");

        paymentRequest = new ProcessPaymentRequestDTO();
        paymentRequest.setOrderId(1L);
        paymentRequest.setPaymentMethod(PaymentMethod.PIX);
    }

    @Test
    @DisplayName("Deve processar pagamento PIX com sucesso")
    void testProcessPayment_PIX_Success() {

        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        PaymentResponseDTO result = paymentService.processPayment(1L, paymentRequest);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTransactionId());
        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("Deve processar pagamento com cartão de crédito")
    void testProcessPayment_CreditCard_Success() {

        // Arrange
        paymentRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentRequest.setCardNumber("1234567890123456");
        paymentRequest.setCardHolderName("JOAO SILVA");
        paymentRequest.setCardExpiryDate("12/2026");
        paymentRequest.setCardCvv("123");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        PaymentResponseDTO result = paymentService.processPayment(1L, paymentRequest);

        // Assert
        assertNotNull(result);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao processar pagamento de pedido inexistente")
    void testProcessPayment_OrderNotFound() {

        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        paymentRequest.setOrderId(999L);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.processPayment(1L, paymentRequest);
        });
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao processar pagamento de pedido de outro usuário")
    void testProcessPayment_WrongUser() {

        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        order.setUser(otherUser);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            paymentService.processPayment(1L, paymentRequest);
        });

        assertEquals("Pedido não pertence ao usuário", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao processar pagamento de pedido já pago")
    void testProcessPayment_AlreadyPaid() {

        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            paymentService.processPayment(1L, paymentRequest);
        });

        assertEquals("Pedido já possui pagamento processado", exception.getMessage());
        verify(paymentRepository, times(1)).existsByOrderId(1L);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao processar pagamento de pedido com status inválido")
    void testProcessPayment_InvalidStatus() {

        // Arrange
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            paymentService.processPayment(1L, paymentRequest);
        });

        assertEquals("Apenas pedidos pendentes podem receber pagamento", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Deve consultar pagamento do pedido")
    void testFindByOrderId_Success() {

        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        // Act
        PaymentResponseDTO result = paymentService.findByOrderId(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("TXN-123456", result.getTransactionId());
        verify(paymentRepository, times(1)).findByOrderId(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao consultar pagamento inexistente")
    void testFindByOrderId_NotFound() {

        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.findByOrderId(1L, 1L);
        });
        verify(paymentRepository, times(1)).findByOrderId(1L);
    }

    @Test
    @DisplayName("Deve reembolsar pagamento com sucesso")
    void testRefundPayment_Success() {

        // Arrange
        order.setStatus(OrderStatus.PAID);
        payment.setStatus(PaymentStatus.APPROVED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        PaymentResponseDTO result = paymentService.refundPayment(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(paymentRepository, times(1)).save(payment);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("Deve lançar exceção ao reembolsar pagamento não aprovado")
    void testRefundPayment_NotApproved() {

        // Arrange
        payment.setStatus(PaymentStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            paymentService.refundPayment(1L, 1L);
        });

        assertEquals("Apenas pagamentos aprovados podem ser reembolsados", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao reembolsar pedido já entregue")
    void testRefundPayment_Delivered() {

        // Arrange
        order.setStatus(OrderStatus.DELIVERED);
        payment.setStatus(PaymentStatus.APPROVED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            paymentService.refundPayment(1L, 1L);
        });

        assertEquals("Não é possível reembolsar pedido já entregue", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao reembolsar pagamento de pedido de outro usuário")
    void testRefundPayment_WrongUser() {

        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        order.setUser(otherUser);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            paymentService.refundPayment(1L, 1L);
        });

        assertEquals("Pedido não pertence ao usuário", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
