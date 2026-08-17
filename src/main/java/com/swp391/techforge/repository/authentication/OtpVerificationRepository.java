package com.swp391.techforge.repository.authentication;

import com.swp391.techforge.entity.OtpPurpose;
import com.swp391.techforge.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);
}