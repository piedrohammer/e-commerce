package com.hammer.ecommerce.repositories;

import com.hammer.ecommerce.model.Payment;
import com.hammer.ecommerce.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    boolean existsByOrderId(Long orderId);

    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}