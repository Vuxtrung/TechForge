package com.swp391.techforge.repository.component;

import com.swp391.techforge.entity.component.Gpu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GpuRepository extends JpaRepository<Gpu, Long> {

    // Xoá an toàn theo productId: KHÔNG ném exception nếu chưa có row
    // (khác với deleteById() mặc định của Spring Data, sẽ ném
    // EmptyResultDataAccessException nếu không tìm thấy - gây lỗi khi tạo
    // sản phẩm linh kiện mới lần đầu, vì lúc đó chưa có row nào ở bảng con).
    @Modifying
    @Query("DELETE FROM Gpu e WHERE e.productId = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}