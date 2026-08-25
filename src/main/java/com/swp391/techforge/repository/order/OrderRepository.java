package com.swp391.techforge.repository.order;

import com.swp391.techforge.entity.Order;
import com.swp391.techforge.entity.OrderStatus;
import com.swp391.techforge.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserOrderByOrderDateDesc(User user);

    List<Order> findByUserAndStatusIn(User user, List<OrderStatus> statuses);

       @Query("SELECT o FROM Order o WHERE o.user = :user " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:startDate IS NULL OR o.orderDate >= :startDate) " +
           "AND (:endDate IS NULL OR o.orderDate <= :endDate) " +
           "AND (:search IS NULL OR CAST(o.orderId AS string) LIKE %:search% OR LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY o.orderDate DESC")
    List<Order> filterCustomerOrders(@Param("user") User user,
                                     @Param("status") OrderStatus status,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate,
                                     @Param("search") String search);

    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) " +
           "AND (:startDate IS NULL OR o.orderDate >= :startDate) " +
           "AND (:endDate IS NULL OR o.orderDate <= :endDate) " +
           "AND (:search IS NULL OR CAST(o.orderId AS string) LIKE %:search% " +
           "OR LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(o.phone) LIKE LOWER(CONCAT('%', :search, '%'))) ")
    Page<Order> searchForStaff(@Param("status") OrderStatus status,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              @Param("search") String search,
                              Pageable pageable);
}
