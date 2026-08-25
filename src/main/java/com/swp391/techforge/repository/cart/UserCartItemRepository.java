package com.swp391.techforge.repository.cart;

import com.swp391.techforge.entity.Product;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.UserCartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCartItemRepository extends JpaRepository<UserCartItem, Long> {

    List<UserCartItem> findByUserOrderByCreatedAtDesc(User user);

    Optional<UserCartItem> findByUserAndProduct(User user, Product product);

    void deleteByUser(User user);

    void deleteByUserAndProduct(User user, Product product);
}