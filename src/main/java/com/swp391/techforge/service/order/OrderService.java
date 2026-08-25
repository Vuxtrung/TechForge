package com.swp391.techforge.service.order;

import com.swp391.techforge.dto.cart.CartItemDTO;
import com.swp391.techforge.dto.order.CheckoutRequest;
import com.swp391.techforge.entity.*;
import com.swp391.techforge.repository.authentication.RoleRepository;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.repository.order.*;
import com.swp391.techforge.repository.product.ProductRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final VoucherService voucherService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        PaymentRepository paymentRepository,
                        VoucherService voucherService,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.voucherService = voucherService;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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

        User orderUser = user;
        if (orderUser == null && request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            orderUser = userRepository.findByEmail(request.getEmail().trim()).orElse(null);
        }

        if (orderUser == null) {
            throw new IllegalStateException("Vui lòng đăng nhập tài khoản trước khi thực hiện đặt hàng!");
        }

        // BR-V08: voucher phải được kiểm tra lại đầy đủ (BR-V02..BR-V06, BR-V10)
        // ngay tại thời điểm tạo đơn, không tin kết quả đã check ở bước preview giỏ hàng.
        // Dùng orderUser (đã resolve) để BR-V06 (giới hạn theo khách hàng) chính xác.
        Voucher voucher = voucherService.validateForCheckout(request.getVoucherCode(), subtotal, orderUser);
        BigDecimal voucherDiscount = voucherService.calculateDiscount(voucher, subtotal);

        BigDecimal shippingFee = BigDecimal.valueOf(30000);
        if ("EXPRESS".equalsIgnoreCase(request.getShippingMethod())) {
            shippingFee = BigDecimal.valueOf(50000);
        }

        BigDecimal grandTotal = subtotal.subtract(voucherDiscount).add(shippingFee);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        Order order = new Order();
        order.setUser(orderUser);
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
        order.setDiscountAmount(voucherDiscount);

        Order savedOrder = orderRepository.save(order);

        // BR-V09: chỉ ghi nhận lượt dùng voucher SAU KHI thanh toán thành công.
        // - COD: không có bước xác nhận thanh toán online riêng trong hệ thống hiện tại,
        //   nên coi "đặt hàng COD thành công" là mốc ghi nhận.
        // - VNPAY: đơn đang ở trạng thái PENDING chờ thanh toán -> KHÔNG ghi nhận ở đây,
        //   việc ghi nhận được thực hiện ở CheckoutController#vnpayReturn khi callback báo SUCCESS.
        if (voucher != null && !"VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            voucherService.recordUsage(voucher, orderUser, savedOrder);
        }

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
        if (user == null) {
            return List.of();
        }
        return orderRepository.filterCustomerOrders(user, status, startDate, endDate, search);
    }

    @Transactional(readOnly = true)
    public Page<Order> searchForStaff(String search, String status, LocalDateTime startDate, LocalDateTime endDate,
                                     int page, int size, Sort sort) {
        OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                orderStatus = OrderStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // ignore invalid status
            }
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        return orderRepository.searchForStaff(orderStatus, startDate, endDate, search, pageable);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrdersForStaffExport(String search, String status, LocalDateTime startDate, LocalDateTime endDate) {
        OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                orderStatus = OrderStatus.valueOf(status.trim().toUpperCase());
                
            } catch (IllegalArgumentException ignored) {
                // ignore invalid status
            }
        }

        final OrderStatus finalOrderStatus = orderStatus;
        
        // Get all orders without pagination for export
        return orderRepository.findAll().stream()
                .filter(o -> finalOrderStatus == null || o.getStatus() == finalOrderStatus)
                .filter(o -> startDate == null || o.getOrderDate().isAfter(startDate) || o.getOrderDate().isEqual(startDate))
                .filter(o -> endDate == null || o.getOrderDate().isBefore(endDate) || o.getOrderDate().isEqual(endDate))
                .filter(o -> search == null || search.isBlank() ||
                        o.getOrderId().toString().contains(search) ||
                        (o.getRecipientName() != null && o.getRecipientName().toLowerCase().contains(search.toLowerCase())) ||
                        (o.getPhone() != null && o.getPhone().contains(search)))
                .sorted((a, b) -> b.getOrderDate().compareTo(a.getOrderDate()))
                .toList();
    }

    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderByIdForUser(Long orderId, User user) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getUser() != null && user != null
                        && order.getUser().getUserId().equals(user.getUserId()));
    }

        @Transactional(readOnly = true)
        public Optional<OrderItem> getOrderItemByIdForUser(Long orderItemId, User user) {
        return orderItemRepository.findById(orderItemId)
            .filter(item -> item.getOrder() != null && item.getOrder().getUser() != null
                && user != null && item.getOrder().getUser().getUserId().equals(user.getUserId()));
        }

    @Transactional
    public Order updateStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        try {
            order.setStatus(OrderStatus.valueOf(status.trim().toUpperCase()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ.");
        }
        return orderRepository.save(order);
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
            restoreStock(order.getOrderId());
            voucherService.releaseUsageForCancelledOrder(order);
            return true;
        }
        return false;
    }

    @Transactional
    public void cancelPendingOrdersForUser(User user) {
        List<Order> pendingOrders = orderRepository.findByUserAndStatusIn(user, List.of(OrderStatus.PENDING));
        for (Order order : pendingOrders) {
            cancelOrder(order.getOrderId(), "Tài khoản của bạn đã bị khoá", null);
        }
    }

    // Hoàn lại stockQuantity đã trừ lúc tạo đơn. Dùng chung cho cancelOrder()
    // và cho trường hợp đơn bị hủy do thanh toán VNPay thất bại (xem
    // CheckoutController#vnpayReturn), tránh trùng logic ở 2 nơi.
    // Nhận orderId thay vì Order entity: order.getOrderItems() là lazy, nếu
    // Order được fetch ở transaction khác (VD trong Controller không
    // @Transactional) rồi truyền vào đây thì truy cập collection sẽ ném
    // LazyInitializationException do session gốc đã đóng. Fetch lại theo id
    // để đảm bảo luôn nằm trong transaction đang mở của chính method này.
    @Transactional
    public void restoreStock(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return;

        for (OrderItem item : order.getOrderItems()) {
            if (item.getProduct() != null) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }
    }

    @Transactional
    public boolean confirmReceived(Long orderId, User user) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return false;

        Order order = orderOpt.get();
        if (user != null && order.getUser() != null && !order.getUser().getUserId().equals(user.getUserId())) {
            return false;
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean reportComplaint(Long orderId, User user) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return false;

        Order order = orderOpt.get();
        if (user != null && order.getUser() != null && !order.getUser().getUserId().equals(user.getUserId())) {
            return false;
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            order.setStatus(OrderStatus.COMPLAINT);
            orderRepository.save(order);
            return true;
        }
        return false;
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void autoCompleteDeliveredOrders() {
        List<Order> deliveredOrders = orderRepository.findByStatus(OrderStatus.DELIVERED);
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        
        for (Order order : deliveredOrders) {
            // Assuming orderDate is close enough to when it was actually delivered, 
            // or we just use orderDate + 3 days as the auto-complete limit.
            if (order.getOrderDate().isBefore(threeDaysAgo)) {
                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);
            }
        }
    }
}