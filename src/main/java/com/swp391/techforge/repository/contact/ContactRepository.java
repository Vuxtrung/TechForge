package com.swp391.techforge.repository.contact;

import com.swp391.techforge.entity.Contact;
import com.swp391.techforge.entity.ContactStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    @Query("SELECT c FROM Contact c WHERE " +
            "(:keyword IS NULL OR :keyword = '' " +
            "   OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(c.subject) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR c.status = :status)")
    Page<Contact> search(@Param("keyword") String keyword,
            @Param("status") ContactStatus status,
            Pageable pageable);
}