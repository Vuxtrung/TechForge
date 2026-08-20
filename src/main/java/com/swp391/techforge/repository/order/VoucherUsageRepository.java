package com.swp391.techforge.repository.order;

import com.swp391.techforge.entity.Order;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.Voucher;
import com.swp391.techforge.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    // BR-V05: tổng số lượt đã dùng của voucher (toàn hệ thống)
    long countByVoucher(Voucher voucher);

    // BR-V06: số lượt 1 khách hàng cụ thể đã dùng CHÍNH voucher này
    long countByVoucherAndUser(Voucher voucher, User user);

    List<VoucherUsage> findByVoucher_VoucherIdOrderByUsedAtDesc(Long voucherId);

    // Dùng khi hủy đơn: xóa lượt dùng đã ghi nhận (nếu có) để hoàn lại quota
    // (BR-V05/BR-V06) cho khách và cho voucher, tránh đơn bị hủy vẫn tính là "đã dùng".
    void deleteByOrder(Order order);
}