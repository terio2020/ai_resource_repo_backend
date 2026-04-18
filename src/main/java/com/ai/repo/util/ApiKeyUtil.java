package com.ai.repo.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ApiKeyUtil {

    private static final String PREFIX = "logicoma-world-";
    private static final int RANDOM_BYTES_LENGTH = 36;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateApiKey() {
        byte[] randomBytes = new byte[RANDOM_BYTES_LENGTH];
        RANDOM.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return PREFIX + randomPart.substring(0, 48);
    }
}
