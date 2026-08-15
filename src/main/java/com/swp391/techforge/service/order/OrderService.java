package com.swp391.techforge.service.order;

import com.swp391.techforge.dto.cart.CartItemDTO;
import com.swp391.techforge.dto.order.CheckoutRequest;
import com.swp391.techforge.entity.*;
import com.swp391.techforge.repository.order.*;
import com.swp391.techforge.repository.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private ProductRepository productRepository;

    public Voucher validateVoucher(String code, BigDecimal subtotal) {
        if (code == null || code.trim().isEmpty()) return null;

        Optional<Voucher> voucherOpt = voucherRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim());
        if (voucherOpt.isEmpty()) return null;

        Voucher voucher = voucherOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) return null;
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) return null;
        if (voucher.getUsageLimit() != null && voucher.getUsageLimit() <= 0) return null;
        if (voucher.getMinOrderValue() != null && subtotal.compareTo(voucher.getMinOrderValue()) < 0) return null;

        return voucher;
    }

    public BigDecimal calculateVoucherDiscount(Voucher voucher, BigDecimal subtotal) {
        if (voucher == null || subtotal == null) return BigDecimal.ZERO;

        BigDecimal discount = BigDecimal.ZERO;
        if (voucher.getDiscountType() == DiscountType.PERCENT) {
            discount = subtotal.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100));
        } else if (voucher.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = voucher.getDiscountValue();
        }

        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        return discount;
    }

    @Transactional
    public Order createOrder(CheckoutRequest request, List<CartItemDTO> cartItems, User user) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng của bạn đang trống!");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItemDTO item : cartItems) {
            BigDecimal price = BigDecimal.valueOf(item.getPrice());
            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            if (item.getProductId() != null) {
                Optional<Product> productOpt = productRepository.findById(item.getProductId());
                if (productOpt.isPresent()) {
                    Product product = productOpt.get();
                    if (product.getStockQuantity() < item.getQuantity()) {
                        throw new IllegalStateException("Sản phẩm " + product.getName() + " không đủ số lượng trong kho!");
                    }
                    product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                    productRepository.save(product);
                }
            }
        }

        Voucher voucher = validateVoucher(request.getVoucherCode(), subtotal);
        BigDecimal voucherDiscount = calculateVoucherDiscount(voucher, subtotal);

        BigDecimal shippingFee = BigDecimal.valueOf(30000);
        if ("EXPRESS".equalsIgnoreCase(request.getShippingMethod())) {
            shippingFee = BigDecimal.valueOf(50000);
        }

        BigDecimal grandTotal = subtotal.subtract(voucherDiscount).add(shippingFee);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        Order order = new Order();
        order.setUser(user);
        order.setRecipientName(request.getRecipientName());
        order.setPhone(request.getPhone());
        order.setEmail(request.getEmail());
        order.setShippingAddress(request.getFullShippingAddress());
        order.setShippingMethod(request.getShippingMethod());
        order.setShippingFee(shippingFee);
        order.setOrderNote(request.getOrderNote());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(grandTotal);
        order.setVoucher(voucher);

        if (voucher != null && voucher.getUsageLimit() != null) {
            voucher.setUsageLimit(voucher.getUsageLimit() - 1);
            voucherRepository.save(voucher);
        }

        Order savedOrder = orderRepository.save(order);

        for (CartItemDTO item : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProductName(item.getProductName());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(BigDecimal.valueOf(item.getPrice()));

            if (item.getProductId() != null) {
                productRepository.findById(item.getProductId()).ifPresent(orderItem::setProduct);
            }
            orderItemRepository.save(orderItem);
        }

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setAmount(grandTotal);

        if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            payment.setMethod(PaymentMethod.VNPAY);
            payment.setStatus(PaymentStatus.PENDING);
        } else {
            payment.setMethod(PaymentMethod.COD);
            payment.setStatus(PaymentStatus.PENDING);
        }
        paymentRepository.save(payment);

        return savedOrder;
    }

    public List<Order> getCustomerOrders(User user, OrderStatus status, LocalDateTime startDate, LocalDateTime endDate, String search) {
        return orderRepository.filterCustomerOrders(user, status, startDate, endDate, search);
    }

    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    @Transactional
    public boolean cancelOrder(Long orderId, String reason, User user) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return false;

        Order order = orderOpt.get();
        if (user != null && order.getUser() != null && !order.getUser().getUserId().equals(user.getUserId())) {
            return false;
        }

        if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelReason(reason);
            orderRepository.save(order);

            for (OrderItem item : order.getOrderItems()) {
                if (item.getProduct() != null) {
                    Product product = item.getProduct();
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                    productRepository.save(product);
                }
            }
            return true;
        }
        return false;
    }
}
