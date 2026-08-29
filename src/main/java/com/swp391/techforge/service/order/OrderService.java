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
    private final VoucherRepository voucherRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        PaymentRepository paymentRepository,
                        VoucherRepository voucherRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.voucherRepository = voucherRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

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

        User orderUser = user;
        if (orderUser == null && request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            orderUser = userRepository.findByEmail(request.getEmail().trim()).orElse(null);
        }

        if (orderUser == null) {
            throw new IllegalStateException("Vui lòng đăng nhập tài khoản trước khi thực hiện đặt hàng!");
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

    @Transactional(readOnly = true)
    public Page<Order> searchForCustomer(User user, OrderStatus status, LocalDateTime startDate,
                                         LocalDateTime endDate, String search, int page, int size) {
        if (user == null) {
            return Page.empty();
        }
        return orderRepository.searchForCustomer(user, status, startDate, endDate, search,
                PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "orderDate")));
    }

    public List<Order> getCustomerOrders(User user, OrderStatus status, LocalDateTime startDate,
                                         LocalDateTime endDate, String search) {
        return searchForCustomer(user, status, startDate, endDate, search, 0, 100).getContent();
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

        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), sort);
        return orderRepository.searchForStaff(orderStatus, startDate, endDate, search, pageable);
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        return size > 0 && size <= 100 ? size : 10;
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
            OrderStatus nextStatus = OrderStatus.valueOf(status.trim().toUpperCase());
            if (!isAllowedStatusChange(order.getStatus(), nextStatus)) {
                throw new IllegalArgumentException("Không thể chuyển trạng thái đơn hàng từ "
                        + order.getStatus() + " sang " + nextStatus + ".");
            }
            order.setStatus(nextStatus);
        } catch (IllegalArgumentException | NullPointerException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Không thể chuyển")) {
                throw e;
            }
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ.");
        }
        return orderRepository.save(order);
    }

    private boolean isAllowedStatusChange(OrderStatus current, OrderStatus next) {
        if (current == next) return true;
        return switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPING || next == OrderStatus.CANCEL_REQUESTED || next == OrderStatus.CANCELLED;
            case SHIPPING -> next == OrderStatus.DELIVERED;
            case CANCEL_REQUESTED -> next == OrderStatus.CANCELLED || next == OrderStatus.CONFIRMED;
            default -> false;
        };
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
