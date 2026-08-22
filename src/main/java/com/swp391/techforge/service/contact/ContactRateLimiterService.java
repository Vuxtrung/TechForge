package com.swp391.techforge.service.contact;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContactRateLimiterService {

    private final Map<String, Long> ipLastSubmitTime = new ConcurrentHashMap<>();
    private static final long COOLDOWN_SECONDS = 60;

    public boolean isAllowed(String clientIp) {
        long now = Instant.now().getEpochSecond();
        Long lastTime = ipLastSubmitTime.get(clientIp);

        if (lastTime != null && (now - lastTime) < COOLDOWN_SECONDS) {
            return false;
        }

        ipLastSubmitTime.put(clientIp, now);
        return true;
    }
}