package com.ai.repo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;

class TempTokenServiceTest {

    private TempTokenService service;

    @BeforeEach
    void setUp() {
        service = new TempTokenService();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Shutdown the cleanup executor so it doesn't keep running across tests
        Field f = TempTokenService.class.getDeclaredField("cleanupExecutor");
        f.setAccessible(true);
        ((ScheduledExecutorService) f.get(service)).shutdownNow();
    }

    // ==================== storeToken ====================

    @Test
    void storeToken_shouldReturnSessionId() {
        String returned = service.storeToken("session-1", "access-token-abc");

        assertEquals("session-1", returned);
    }

    @Test
    void storeToken_shouldStoreTokenForLaterRetrieval() {
        service.storeToken("session-1", "access-token-abc");

        assertTrue(service.hasToken("session-1"));
        assertEquals("access-token-abc", service.getAndRemoveToken("session-1"));
    }

    @Test
    void storeToken_overwriteExistingSession_shouldReplaceValue() {
        service.storeToken("session-1", "first-token");
        service.storeToken("session-1", "second-token");

        assertEquals("second-token", service.getAndRemoveToken("session-1"));
    }

    @Test
    void storeToken_shouldAllowMultipleSessions() {
        service.storeToken("session-A", "token-A");
        service.storeToken("session-B", "token-B");
        service.storeToken("session-C", "token-C");

        assertEquals("token-B", service.getAndRemoveToken("session-B"));
        assertEquals("token-C", service.getAndRemoveToken("session-C"));
        assertEquals("token-A", service.getAndRemoveToken("session-A"));
    }

    @Test
    void storeToken_shouldAllowNullAccessToken() {
        // service stores whatever value is given, no null check
        service.storeToken("session-null", null);

        assertTrue(service.hasToken("session-null"));
        assertNull(service.getAndRemoveToken("session-null"));
    }

    // ==================== getAndRemoveToken ====================

    @Test
    void getAndRemoveToken_shouldReturnNull_whenSessionNotFound() {
        assertNull(service.getAndRemoveToken("nonexistent"));
    }

    @Test
    void getAndRemoveToken_shouldRemoveTokenAfterRetrieval() {
        service.storeToken("once", "token-xyz");

        assertEquals("token-xyz", service.getAndRemoveToken("once"));
        // Second retrieval must return null — token was consumed
        assertNull(service.getAndRemoveToken("once"));
    }

    @Test
    void getAndRemoveToken_shouldReturnNull_whenTokenExpired() throws Exception {
        service.storeToken("expired", "old-token");

        // Force expiry to be in the past by manipulating internal TokenWithExpiry
        Field storeField = TempTokenService.class.getDeclaredField("tokenStore");
        storeField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> store = (Map<String, Object>) storeField.get(service);

        // TokenWithExpiry has package-private fields; rebuild via reflection
        Class<?> tkeClass = Class.forName("com.ai.repo.service.TempTokenService$TokenWithExpiry");
        Object expiredTke = tkeClass.getDeclaredConstructor(String.class, long.class)
                .newInstance("old-token", System.currentTimeMillis() - 1000L);
        store.put("expired", expiredTke);

        assertNull(service.getAndRemoveToken("expired"), "Expired token should return null");
    }

    // ==================== hasToken ====================

    @Test
    void hasToken_shouldReturnFalse_whenSessionNotFound() {
        assertFalse(service.hasToken("nonexistent"));
    }

    @Test
    void hasToken_shouldReturnTrue_forActiveToken() {
        service.storeToken("active", "token-1");
        assertTrue(service.hasToken("active"));
    }

    @Test
    void hasToken_shouldReturnFalse_andRemoveExpiredToken() throws Exception {
        service.storeToken("expiring", "token-2");

        Field storeField = TempTokenService.class.getDeclaredField("tokenStore");
        storeField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> store = (Map<String, Object>) storeField.get(service);

        Class<?> tkeClass = Class.forName("com.ai.repo.service.TempTokenService$TokenWithExpiry");
        Object expiredTke = tkeClass.getDeclaredConstructor(String.class, long.class)
                .newInstance("token-2", System.currentTimeMillis() - 1000L);
        store.put("expiring", expiredTke);

        assertFalse(service.hasToken("expiring"), "Expired token should return false");
        // Should also have been cleaned up
        assertFalse(store.containsKey("expiring"), "Expired token should be removed on hasToken check");
    }

    @Test
    void hasToken_shouldNotConsumeActiveToken() {
        service.storeToken("keep", "token-3");

        assertTrue(service.hasToken("keep"));
        assertTrue(service.hasToken("keep"));
        assertEquals("token-3", service.getAndRemoveToken("keep"));
    }

    // ==================== cleanupExpiredTokens (exercised via store internals) ====================

    @Test
    void cleanupExpiredTokens_shouldRemoveOnlyExpiredEntries() throws Exception {
        service.storeToken("alive-1", "t1");
        service.storeToken("alive-2", "t2");

        // Inject an expired token
        Field storeField = TempTokenService.class.getDeclaredField("tokenStore");
        storeField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> store = (Map<String, Object>) storeField.get(service);

        Class<?> tkeClass = Class.forName("com.ai.repo.service.TempTokenService$TokenWithExpiry");
        store.put("dead-1", tkeClass.getDeclaredConstructor(String.class, long.class)
                .newInstance("dead", System.currentTimeMillis() - 5000L));
        store.put("dead-2", tkeClass.getDeclaredConstructor(String.class, long.class)
                .newInstance("dead", System.currentTimeMillis() - 10000L));

        // Invoke private cleanupExpiredTokens via reflection
        var cleanupMethod = TempTokenService.class.getDeclaredMethod("cleanupExpiredTokens");
        cleanupMethod.setAccessible(true);
        cleanupMethod.invoke(service);

        // Active tokens remain
        assertTrue(store.containsKey("alive-1"));
        assertTrue(store.containsKey("alive-2"));
        // Expired tokens removed
        assertFalse(store.containsKey("dead-1"));
        assertFalse(store.containsKey("dead-2"));
    }

    @Test
    void cleanupExpiredTokens_shouldHandleEmptyStore() throws Exception {
        // Should not throw on empty store
        var cleanupMethod = TempTokenService.class.getDeclaredMethod("cleanupExpiredTokens");
        cleanupMethod.setAccessible(true);
        cleanupMethod.invoke(service);
        // Pass — no exception
    }
}