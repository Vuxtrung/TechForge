package com.swp391.techforge.repository.authentication;

import com.swp391.techforge.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserUserId(Long userId);

    Optional<UserAddress> findByUserUserIdAndIsDefaultTrue(Long userId);

    boolean existsByUserUserIdAndIsDefaultTrue(Long userId);

    Optional<UserAddress> findByAddressIdAndUserUserId(
            Long addressId,
            Long userId
    );

    @Modifying
    @Query("""
        UPDATE UserAddress ua
        SET ua.isDefault = false
        WHERE ua.user.userId = :userId
          AND ua.isDefault = true
        """)
    int clearDefaultAddress(@Param("userId") Long userId);
}