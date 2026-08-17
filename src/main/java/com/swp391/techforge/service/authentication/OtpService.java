package com.swp391.techforge.service.authentication;

import com.swp391.techforge.entity.OtpPurpose;
import com.swp391.techforge.entity.OtpVerification;
import com.swp391.techforge.repository.authentication.OtpVerificationRepository;
import com.swp391.techforge.service.email.EmailService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class OtpService {

    private static final int EXPIRE_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;

    public OtpService(OtpVerificationRepository otpRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    /** Sinh OTP mới + gửi email. Trả về false nếu đang trong cooldown resend. */
    public boolean generateAndSend(String email, OtpPurpose purpose) {
        Optional<OtpVerification> lastOtp =
                otpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose);

        if (lastOtp.isPresent()) {
            long secondsSinceLast = ChronoUnit.SECONDS.between(lastOtp.get().getCreatedAt(), LocalDateTime.now());
            if (secondsSinceLast < RESEND_COOLDOWN_SECONDS) {
                return false;
            }
        }

        OtpVerification otp = new OtpVerification();
        otp.setEmail(email);
        otp.setPurpose(purpose);
        otp.setOtpCode(generateOtpCode());
        otp.setExpiredAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
        otpRepository.save(otp);

        emailService.sendOtpEmail(email, otp.getOtpCode());
        return true;
    }

    public enum VerifyResult { SUCCESS, EXPIRED, WRONG_CODE, MAX_ATTEMPTS_REACHED, NOT_FOUND }

    public VerifyResult verify(String email, OtpPurpose purpose, String inputCode) {
        Optional<OtpVerification> otpOpt =
                otpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose);

        if (otpOpt.isEmpty()) {
            return VerifyResult.NOT_FOUND;
        }

        OtpVerification otp = otpOpt.get();

        if (otp.getAttemptCount() >= MAX_ATTEMPTS) {
            return VerifyResult.MAX_ATTEMPTS_REACHED;
        }
        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            return VerifyResult.EXPIRED;
        }
        if (!otp.getOtpCode().equals(inputCode)) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpRepository.save(otp);
            return VerifyResult.WRONG_CODE;
        }

        otp.setVerified(true);
        otpRepository.save(otp);
        return VerifyResult.SUCCESS;
    }

    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1_000_000));
    }
}