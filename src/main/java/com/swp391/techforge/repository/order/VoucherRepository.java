package com.swp391.techforge.repository.order;

import com.swp391.techforge.entity.DiscountType;
import com.swp391.techforge.entity.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeIgnoreCaseAndIsActiveTrue(String code);

    Optional<Voucher> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndVoucherIdNot(String code, Long voucherId);

    // Lock voucher khi checkout để tránh 2 khách cùng lúc "lọt" qua kiểm tra
    // usage_limit (BR-V05) do đọc số đếm cũ trước khi đối phương ghi usage.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where v.voucherId = :id")
    Optional<Voucher> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select v from Voucher v
            where (:keyword is null or :keyword = '' or lower(v.code) like lower(concat('%', :keyword, '%'))
                   or lower(v.description) like lower(concat('%', :keyword, '%')))
              and (:discountType is null or v.discountType = :discountType)
              and (:active is null or v.isActive = :active)
            """)
    Page<Voucher> search(@Param("keyword") String keyword,
                          @Param("discountType") DiscountType discountType,
                          @Param("active") Boolean active,
                          Pageable pageable);
}