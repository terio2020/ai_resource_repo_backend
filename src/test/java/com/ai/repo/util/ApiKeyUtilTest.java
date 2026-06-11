package com.ai.repo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyUtilTest {

    private final ApiKeyUtil apiKeyUtil = new ApiKeyUtil();

    @Test
    void generateApiKey_shouldReturnNonNull() {
        assertNotNull(apiKeyUtil.generateApiKey());
    }

    @Test
    void generateApiKey_shouldStartWithPrefix() {
        String key = apiKeyUtil.generateApiKey();
        assertTrue(key.startsWith("logicoma-world-"));
    }

    @Test
    void generateApiKey_shouldProduceDifferentKeys() {
        String key1 = apiKeyUtil.generateApiKey();
        String key2 = apiKeyUtil.generateApiKey();
        assertNotEquals(key1, key2);
    }
}
