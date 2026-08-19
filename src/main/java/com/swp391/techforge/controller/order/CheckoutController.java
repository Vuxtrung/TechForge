package com.swp391.techforge.controller.order;

import com.swp391.techforge.dto.cart.CartItemDTO;
import com.swp391.techforge.dto.order.CheckoutRequest;
import com.swp391.techforge.entity.*;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.repository.order.PaymentRepository;
import com.swp391.techforge.repository.product.ProductRepository;
import com.swp391.techforge.service.order.OrderService;
import com.swp391.techforge.service.order.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private static final String CART_SESSION_KEY = "MY_CART_ITEMS";

    private final OrderService orderService;
    private final VNPayService vnPayService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CheckoutController(OrderService orderService,
                              VNPayService vnPayService,
                              PaymentRepository paymentRepository,
                              UserRepository userRepository,
                              ProductRepository productRepository) {
        this.orderService = orderService;
        this.vnPayService = vnPayService;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    public String viewCheckoutPage(HttpSession session, Model model, Principal principal) {
        @SuppressWarnings("unchecked")
        List<CartItemDTO> cartItems = (List<CartItemDTO>) session.getAttribute(CART_SESSION_KEY);
        if (cartItems == null || cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItemDTO item : cartItems) {
            subtotal = subtotal.add(BigDecimal.valueOf(item.getTotalPrice()));
            if (item.getProductId() != null) {
                productRepository.findById(item.getProductId())
                        .ifPresent(p -> item.setStockQuantity(p.getStockQuantity()));
            }
        }

        CheckoutRequest request = new CheckoutRequest();
        if (principal != null) {
            userRepository.findByEmail(principal.getName()).ifPresent(u -> {
                request.setRecipientName(u.getFullName());
                request.setEmail(u.getEmail());
                request.setPhone(u.getPhone());
                request.setAddressLine(u.getAddress());
            });
        }

        String appliedVoucherCode = (String) session.getAttribute("APPLIED_VOUCHER_CODE");
        if (appliedVoucherCode != null && !appliedVoucherCode.trim().isEmpty()) {
            request.setVoucherCode(appliedVoucherCode.trim());
        }

        Double voucherDiscount = (Double) session.getAttribute("APPLIED_VOUCHER_DISCOUNT");
        if (voucherDiscount == null) {
            voucherDiscount = 0.0;
        }

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("voucherDiscount", voucherDiscount);
        model.addAttribute("checkoutRequest", request);
        return "checkout";
    }

    @PostMapping("/process")
    public String processCheckout(@ModelAttribute CheckoutRequest checkoutRequest,
                                  HttpSession session,
                                  HttpServletRequest request,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {

        @SuppressWarnings("unchecked")
        List<CartItemDTO> cartItems = (List<CartItemDTO>) session.getAttribute(CART_SESSION_KEY);
        if (cartItems == null || cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống!");
            return "redirect:/cart";
        }

        try {
            User loggedInUser = null;
            if (principal != null) {
                loggedInUser = userRepository.findByEmail(principal.getName()).orElse(null);
            }

            Order order = orderService.createOrder(checkoutRequest, cartItems, loggedInUser);
            session.removeAttribute(CART_SESSION_KEY);

            if ("VNPAY".equalsIgnoreCase(checkoutRequest.getPaymentMethod())) {
                String paymentUrl = vnPayService.createPaymentUrl(order, request);
                return "redirect:" + paymentUrl;
            }

            return "redirect:/orders/success/" + order.getOrderId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                fields.put(fieldName, fieldValue);
            }
        }

        boolean checkSignature = vnPayService.verifyCallbackSignature(fields);
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");

        if (vnp_TxnRef != null && vnp_TxnRef.contains("_")) {
            Long orderId = Long.parseLong(vnp_TxnRef.split("_")[0]);
            Optional<Order> orderOpt = orderService.getOrderById(orderId);

            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                Optional<Payment> paymentOpt = paymentRepository.findByOrder(order);

                if (checkSignature && "00".equals(vnp_ResponseCode)) {
                    order.setStatus(OrderStatus.CONFIRMED);
                    if (paymentOpt.isPresent()) {
                        Payment payment = paymentOpt.get();
                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setTransactionCode(request.getParameter("vnp_TransactionNo"));
                        payment.setPaidAt(LocalDateTime.now());
                        paymentRepository.save(payment);
                    }
                    orderService.getOrderById(orderId); // refresh
                    return "redirect:/orders/success/" + order.getOrderId();
                } else {
                    order.setStatus(OrderStatus.CANCELLED);
                    order.setCancelReason("Thanh toán qua VNPay thất bại hoặc bị hủy.");
                    if (paymentOpt.isPresent()) {
                        Payment payment = paymentOpt.get();
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);
                    }
                    redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán VNPay không thành công!");
                    return "redirect:/orders/" + order.getOrderId();
                }
            }
        }

        return "redirect:/orders";
    }
}
