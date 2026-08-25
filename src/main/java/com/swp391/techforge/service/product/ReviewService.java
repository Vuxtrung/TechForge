package com.swp391.techforge.service.product;

import com.swp391.techforge.entity.OrderItem;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.entity.Review;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.repository.order.OrderItemRepository;
import com.swp391.techforge.repository.product.ProductRepository;
import com.swp391.techforge.repository.product.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository, OrderItemRepository orderItemRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    public List<Review> getReviewsByProductId(Long productId) {
        return reviewRepository.findByProduct_ProductIdOrderByCreatedAtDesc(productId);
    }

    public Double getAverageRatingByProductId(Long productId) {
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Transactional
    public void addReview(User user, Long orderItemId, Integer rating, String comment) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy OrderItem"));

        if (!orderItem.getOrder().getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("Bạn không có quyền đánh giá sản phẩm này.");
        }

        if (Boolean.TRUE.equals(orderItem.getIsReviewed())) {
            throw new IllegalStateException("Bạn đã đánh giá sản phẩm này rồi.");
        }

        if (!"COMPLETED".equals(orderItem.getOrder().getStatus().name())) {
            throw new IllegalStateException("Bạn chỉ có thể đánh giá sản phẩm sau khi đã nhận được hàng (Trạng thái Đã hoàn thành).");
        }

        Product product = orderItem.getProduct();
        if (product == null) {
            throw new IllegalStateException("Không tìm thấy sản phẩm để đánh giá.");
        }

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment);
        reviewRepository.save(review);

        orderItem.setIsReviewed(true);
        orderItemRepository.save(orderItem);
    }
}
