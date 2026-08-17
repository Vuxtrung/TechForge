package com.swp391.techforge.repository.warranty;

import com.swp391.techforge.entity.WarrantyTicket;
import com.swp391.techforge.entity.WarrantyTicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarrantyTicketRepository extends JpaRepository<WarrantyTicket, Long> {

    // @Query("SELECT wt FROM WarrantyTicket wt LEFT JOIN wt.user u WHERE " +
    // "(:keyword IS NULL OR :keyword = '' OR " +
    // "LOWER(wt.imeiSerial) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
    // "LOWER(COALESCE(wt.issueDesc, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    // " +
    // "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
    // "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
    // "AND (:status IS NULL OR wt.status = :status)")
    // Page<WarrantyTicket> search(@Param("keyword") String keyword,
    // @Param("status") WarrantyTicketStatus status,
    // Pageable pageable);

    @Query("""
            SELECT wt
            FROM WarrantyTicket wt
            LEFT JOIN wt.user u
            WHERE
                (
                    :keyword IS NULL
                    OR :keyword = ''
                    OR wt.imeiSerial LIKE CONCAT('%', :keyword, '%')
                    OR COALESCE(wt.issueDesc, '') LIKE CONCAT('%', :keyword, '%')
                    OR u.fullName LIKE CONCAT('%', :keyword, '%')
                    OR u.email LIKE CONCAT('%', :keyword, '%')
                )
                AND (:status IS NULL OR wt.status = :status)
            """)
    Page<WarrantyTicket> search(
            @Param("keyword") String keyword,
            @Param("status") WarrantyTicketStatus status,
            Pageable pageable);

    Page<WarrantyTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
