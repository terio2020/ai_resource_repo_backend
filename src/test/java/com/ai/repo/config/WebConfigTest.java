package com.ai.repo.config;

import com.ai.repo.security.ApiKeyInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WebConfigTest {

    @Mock
    private ApiKeyInterceptor apiKeyInterceptor;

    private WebConfig webConfig;

    @BeforeEach
    void setUp() {
        webConfig = new WebConfig();
        ReflectionTestUtils.setField(webConfig, "apiKeyInterceptor", apiKeyInterceptor);
        ReflectionTestUtils.setField(webConfig, "frontendUrl", "https://logicomanet.com");
        ReflectionTestUtils.setField(webConfig, "storageBasePath", "/data/storage");
    }

    @Test
    void addCorsMappings_shouldAllowPatchMethod() {
        CorsConfiguration config = buildAndGetCorsConfig();
        assertTrue(config.getAllowedMethods().contains("PATCH"), "PATCH must be in allowed methods");
        assertTrue(config.getAllowedMethods().containsAll(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")));
    }

    @Test
    void addCorsMappings_shouldAllowCredentials() {
        CorsConfiguration config = buildAndGetCorsConfig();
        assertTrue(Boolean.TRUE.equals(config.getAllowCredentials()));
    }

    @Test
    void addCorsMappings_shouldAllowAllHeaders() {
        CorsConfiguration config = buildAndGetCorsConfig();
        assertTrue(config.getAllowedHeaders().contains("*"));
    }

    @Test
    void addCorsMappings_shouldAllowFrontendOrigin() {
        CorsConfiguration config = buildAndGetCorsConfig();
        assertTrue(config.getAllowedOriginPatterns().contains("https://logicomanet.com"));
    }

    @SuppressWarnings("unchecked")
    private CorsConfiguration buildAndGetCorsConfig() {
        CorsRegistry registry = new CorsRegistry();
        webConfig.addCorsMappings(registry);
        try {
            java.lang.reflect.Method getConfigs = CorsRegistry.class.getDeclaredMethod("getCorsConfigurations");
            getConfigs.setAccessible(true);
            java.util.Map<String, CorsConfiguration> configs =
                    (java.util.Map<String, CorsConfiguration>) getConfigs.invoke(registry);
            CorsConfiguration config = configs.get("/**");
            assertNotNull(config, "CORS config for /** should exist");
            return config;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}