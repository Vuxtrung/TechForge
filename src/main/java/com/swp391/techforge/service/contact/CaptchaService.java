package com.swp391.techforge.service.contact;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CaptchaService {

    @Value("${google.recaptcha.secret}")
    private String secretKey;

    private static final String GOOGLE_RECAPTCHA_ENDPOINT = "https://www.google.com/recaptcha/api/siteverify";

    public boolean verifyCaptcha(String responseToken) {
        if (responseToken == null || responseToken.trim().isEmpty()) {
            return false;
        }

        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> requestMap = new LinkedMultiValueMap<>();
        requestMap.add("secret", secretKey);
        requestMap.add("response", responseToken);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> apiResponse = restTemplate.postForObject(GOOGLE_RECAPTCHA_ENDPOINT, requestMap, Map.class);
            if (apiResponse != null && apiResponse.containsKey("success")) {
                return (Boolean) apiResponse.get("success");
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}