package com.swp391.techforge.repository.authentication;

import com.swp391.techforge.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import com.swp391.techforge.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmail(String email);

    /**
     * Tìm kiếm người dùng với các bộ lọc
     * @param keyword tìm kiếm theo tên, email hoặc số điện thoại
     * @param roleId lọc theo role (null = tất cả)
     * @param status lọc theo trạng thái ACTIVE/LOCKED (null = tất cả)
     * @param pageable phân trang và sắp xếp
     * @return trang kết quả tìm kiếm
     */
    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:roleId IS NULL OR u.role.roleId = :roleId) " +
            "AND (:status IS NULL OR u.status = :status)")
    Page<User> search(@Param("keyword") String keyword,
                      @Param("roleId") Integer roleId,
                      @Param("status") UserStatus status,
                      Pageable pageable);

    /**
     * Tìm kiếm người dùng với nhiều Role (phục vụ danh sách nhân viên).
     * 
     * @param keyword tìm kiếm theo tên, email hoặc số điện thoại
     * @param roleIds danh sách ID các vai trò nhân viên cần lọc
     * @param status lọc theo trạng thái ACTIVE/LOCKED (null = tất cả)
     * @param pageable đối tượng phân trang và sắp xếp
     * @return trang kết quả tìm kiếm danh sách nhân viên
     */
    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (u.role.roleId IN :roleIds) " +
            "AND (:status IS NULL OR u.status = :status)")
    Page<User> searchByRoles(@Param("keyword") String keyword,
                             @Param("roleIds") java.util.List<Integer> roleIds,
                             @Param("status") UserStatus status,
                             Pageable pageable);
}