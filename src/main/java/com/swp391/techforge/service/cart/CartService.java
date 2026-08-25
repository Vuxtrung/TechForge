package com.swp391.techforge.service.cart;

import com.swp391.techforge.dto.cart.CartItemDTO;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.UserCartItem;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.repository.cart.UserCartItemRepository;
import com.swp391.techforge.repository.product.ProductRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    public static final String CART_SESSION_KEY = "MY_CART_ITEMS";

    private final UserCartItemRepository userCartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Lấy toàn bộ danh sách sản phẩm trong giỏ hàng.
     * Nếu đã đăng nhập: Tự động gộp giỏ hàng từ Session vào Database (nếu có) và trả về từ Database.
     * Nếu chưa đăng nhập: Trả về danh sách từ Session.
     */
    @Transactional
    public List<CartItemDTO> getCart(Principal principal, HttpSession session) {
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            if (user != null) {
                mergeSessionCartToDb(user, session);

                List<UserCartItem> dbItems = userCartItemRepository.findByUserOrderByCreatedAtDesc(user);
                List<CartItemDTO> result = new ArrayList<>();
                for (UserCartItem item : dbItems) {
                    Product p = item.getProduct();
                    if (p != null) {
                        double price = p.getBasePrice() != null ? p.getBasePrice().doubleValue() : 0.0;
                        String img = p.getPrimaryImageUrl() != null ? p.getPrimaryImageUrl() : "";
                        int stock = p.isDeleted() ? 0 : p.getStockQuantity();
                        result.add(new CartItemDTO(p.getProductId(), p.getName(), img, price, item.getQuantity(), stock));
                    }
                }
                return result;
            }
        }

        List<CartItemDTO> sessionItems = getCartFromSession(session);
        for (CartItemDTO item : sessionItems) {
            if (item.getProductId() != null) {
                productRepository.findById(item.getProductId())
                        .ifPresent(p -> item.setStockQuantity(p.isDeleted() ? 0 : p.getStockQuantity()));
            }
        }
        return sessionItems;
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     */
    @Transactional
    public void addToCart(Long productId, String productName, Double price, String imageUrl, Integer quantity,
                          Principal principal, HttpSession session) {
        int qty = (quantity != null && quantity > 0) ? quantity : 1;

        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            Product product = productRepository.findById(productId).orElse(null);

            if (user != null && product != null) {
                Optional<UserCartItem> existingOpt = userCartItemRepository.findByUserAndProduct(user, product);
                if (existingOpt.isPresent()) {
                    UserCartItem item = existingOpt.get();
                    item.setQuantity(item.getQuantity() + qty);
                    userCartItemRepository.save(item);
                } else {
                    UserCartItem newItem = new UserCartItem(user, product, qty);
                    userCartItemRepository.save(newItem);
                }
                return;
            }
        }

        List<CartItemDTO> cartItems = getCartFromSession(session);
        boolean found = false;
        for (CartItemDTO item : cartItems) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(item.getQuantity() + qty);
                found = true;
                break;
            }
        }
        if (!found) {
            cartItems.add(new CartItemDTO(productId, productName, imageUrl, price, qty));
        }
        session.setAttribute(CART_SESSION_KEY, cartItems);
    }

    /**
     * Thêm hàng loạt sản phẩm vào giỏ hàng (Dùng cho tính năng Build PC)
     */
    @Transactional
    public void addMultipleToCart(List<CartItemDTO> items, Principal principal, HttpSession session) {
        if (items == null || items.isEmpty()) return;

        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            if (user != null) {
                for (CartItemDTO dto : items) {
                    if (dto.getProductId() == null) continue;
                    Product product = productRepository.findById(dto.getProductId()).orElse(null);
                    if (product != null) {
                        int qty = (dto.getQuantity() != null && dto.getQuantity() > 0) ? dto.getQuantity() : 1;
                        Optional<UserCartItem> existingOpt = userCartItemRepository.findByUserAndProduct(user, product);
                        if (existingOpt.isPresent()) {
                            UserCartItem existing = existingOpt.get();
                            existing.setQuantity(existing.getQuantity() + qty);
                            userCartItemRepository.save(existing);
                        } else {
                            UserCartItem newItem = new UserCartItem(user, product, qty);
                            userCartItemRepository.save(newItem);
                        }
                    }
                }
                return;
            }
        }

        List<CartItemDTO> cartItems = getCartFromSession(session);
        for (CartItemDTO newItem : items) {
            if (newItem.getProductId() == null) continue;
            boolean found = false;
            for (CartItemDTO existing : cartItems) {
                if (existing.getProductId() != null && existing.getProductId().equals(newItem.getProductId())) {
                    existing.setQuantity(existing.getQuantity() + (newItem.getQuantity() != null ? newItem.getQuantity() : 1));
                    found = true;
                    break;
                }
            }
            if (!found) {
                productRepository.findById(newItem.getProductId()).ifPresent(p -> {
                    if (newItem.getProductName() == null || newItem.getProductName().isEmpty()) {
                        newItem.setProductName(p.getName());
                    }
                    if (newItem.getPrice() == null) {
                        newItem.setPrice(p.getBasePrice() != null ? p.getBasePrice().doubleValue() : 0.0);
                    }
                    if (newItem.getImageUrl() == null || newItem.getImageUrl().isEmpty()) {
                        newItem.setImageUrl(p.getPrimaryImageUrl() != null ? p.getPrimaryImageUrl() : "");
                    }
                });
                if (newItem.getQuantity() == null || newItem.getQuantity() <= 0) {
                    newItem.setQuantity(1);
                }
                cartItems.add(newItem);
            }
        }
        session.setAttribute(CART_SESSION_KEY, cartItems);
    }

    /**
     * Cập nhật số lượng sản phẩm trong giỏ hàng
     */
    @Transactional
    public void updateQuantity(Long productId, Integer quantity, Principal principal, HttpSession session) {
        if (quantity == null || quantity <= 0) {
            removeFromCart(productId, principal, session);
            return;
        }

        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            Product product = productRepository.findById(productId).orElse(null);
            if (user != null && product != null) {
                userCartItemRepository.findByUserAndProduct(user, product).ifPresent(item -> {
                    item.setQuantity(quantity);
                    userCartItemRepository.save(item);
                });
                return;
            }
        }

        List<CartItemDTO> cartItems = getCartFromSession(session);
        for (CartItemDTO item : cartItems) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(quantity);
                break;
            }
        }
        session.setAttribute(CART_SESSION_KEY, cartItems);
    }

    /**
     * Xóa sản phẩm khỏi giỏ hàng
     */
    @Transactional
    public void removeFromCart(Long productId, Principal principal, HttpSession session) {
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            Product product = productRepository.findById(productId).orElse(null);
            if (user != null && product != null) {
                userCartItemRepository.deleteByUserAndProduct(user, product);
                return;
            }
        }

        List<CartItemDTO> cartItems = getCartFromSession(session);
        cartItems.removeIf(item -> item.getProductId().equals(productId));
        session.setAttribute(CART_SESSION_KEY, cartItems);
    }

    /**
     * Xóa toàn bộ giỏ hàng (Gọi sau khi đặt hàng thành công)
     */
    @Transactional
    public void clearCart(User user, HttpSession session) {
        if (user != null) {
            userCartItemRepository.deleteByUser(user);
        }
        if (session != null) {
            session.removeAttribute(CART_SESSION_KEY);
        }
    }

    /**
     * Tính tổng số lượng tất cả các sản phẩm đang có trong giỏ
     */
    public int getCartCount(Principal principal, HttpSession session) {
        List<CartItemDTO> items = getCart(principal, session);
        int total = 0;
        for (CartItemDTO item : items) {
            total += (item.getQuantity() != null ? item.getQuantity() : 0);
        }
        return total;
    }

    /**
     * Tự động gộp giỏ hàng từ Session vào Database của User khi đăng nhập
     */
    @Transactional
    public void mergeSessionCartToDb(User user, HttpSession session) {
        if (user == null || session == null) return;

        List<CartItemDTO> sessionItems = getCartFromSession(session);
        if (sessionItems.isEmpty()) return;

        for (CartItemDTO guestItem : sessionItems) {
            if (guestItem.getProductId() == null) continue;
            Product product = productRepository.findById(guestItem.getProductId()).orElse(null);
            if (product != null) {
                int qty = (guestItem.getQuantity() != null && guestItem.getQuantity() > 0) ? guestItem.getQuantity() : 1;
                Optional<UserCartItem> existingOpt = userCartItemRepository.findByUserAndProduct(user, product);
                if (existingOpt.isPresent()) {
                    UserCartItem existing = existingOpt.get();
                    existing.setQuantity(existing.getQuantity() + qty);
                    userCartItemRepository.save(existing);
                } else {
                    UserCartItem newItem = new UserCartItem(user, product, qty);
                    userCartItemRepository.save(newItem);
                }
            }
        }

        session.removeAttribute(CART_SESSION_KEY);
    }

    @SuppressWarnings("unchecked")
    private List<CartItemDTO> getCartFromSession(HttpSession session) {
        if (session == null) return new ArrayList<>();
        List<CartItemDTO> cart = (List<CartItemDTO>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
        }
        return cart;
    }
}