package com.swp391.techforge.repository.order;

import com.swp391.techforge.entity.Order;
import com.swp391.techforge.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder(Order order);
    Optional<Payment> findByTransactionCode(String transactionCode);
}
