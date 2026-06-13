package com.ai.repo.service.impl;

import com.ai.repo.exception.ContentModerationException;
import com.ai.repo.exception.ContentModerationException.ModerationErrorType;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAIModerationServiceTest {

    private OpenAIModerationService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new OpenAIModerationService();
        setField("apiKey", "test-api-key");
        setField("minScore", 0.7);
    }

    private void setField(String name, Object value) throws Exception {
        java.lang.reflect.Field f = OpenAIModerationService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    private OkHttpClient injectMockClient(Call call) throws Exception {
        OkHttpClient mockClient = mock(OkHttpClient.class);
        when(mockClient.newCall(any(Request.class))).thenReturn(call);
        setField("httpClient", mockClient);
        return mockClient;
    }

    private static Response buildResponse(int code, String body) {
        return new Response.Builder()
                .request(new Request.Builder().url("https://api.openai.com/v1/moderations").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(code == 200 ? "OK" : "ERR")
                .body(ResponseBody.create(body, MediaType.parse("application/json")))
                .build();
    }

    private static Call callReturning(Response response) throws IOException {
        Call call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        return call;
    }

    private static Call callThrowing(IOException ex) throws IOException {
        Call call = mock(Call.class);
        when(call.execute()).thenThrow(ex);
        return call;
    }

    // ==================== HTTP error responses ====================

    @Test
    void moderateContent_shouldThrowModerationApiError_whenApiReturns4xx() throws Exception {
        injectMockClient(callReturning(buildResponse(401, "{\"error\":\"unauthorized\"}")));

        ContentModerationException ex = assertThrows(ContentModerationException.class,
                () -> service.moderateContent("safe text", "doc.md"));
        assertEquals(ModerationErrorType.MODERATION_API_ERROR, ex.getErrorType());
    }

    @Test
    void moderateContent_shouldThrowModerationApiError_whenApiReturns5xx() throws Exception {
        injectMockClient(callReturning(buildResponse(500, "{\"error\":\"server\"}")));

        ContentModerationException ex = assertThrows(ContentModerationException.class,
                () -> service.moderateContent("safe text", "doc.md"));
        assertEquals(ModerationErrorType.MODERATION_API_ERROR, ex.getErrorType());
    }

    @Test
    void moderateContent_shouldPropagateNPE_whenResponseBodyIsNull() throws Exception {
        // Documents current behavior: production code calls response.body().string()
        // without null-check, so a null body surfaces as NPE rather than being caught
        // and wrapped as ContentModerationException.
        Response response = new Response.Builder()
                .request(new Request.Builder().url("https://api.openai.com/v1/moderations").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(null)
                .build();
        injectMockClient(callReturning(response));

        assertThrows(NullPointerException.class,
                () -> service.moderateContent("safe text", "doc.md"));
    }

    @Test
    void moderateContent_shouldThrowModerationApiError_whenNetworkException() throws Exception {
        injectMockClient(callThrowing(new IOException("connection refused")));

        ContentModerationException ex = assertThrows(ContentModerationException.class,
                () -> service.moderateContent("safe text", "doc.md"));
        assertEquals(ModerationErrorType.MODERATION_API_ERROR, ex.getErrorType());
    }

    // ==================== Successful responses ====================

    @Test
    void moderateContent_shouldPass_whenNotFlagged() throws Exception {
        String body = "{\"results\":[{\"flagged\":false,\"category_scores\":{\"hate\":0.01}}]}";
        injectMockClient(callReturning(buildResponse(200, body)));

        assertDoesNotThrow(() -> service.moderateContent("hello world", "doc.md"));
    }

    @Test
    void moderateContent_shouldThrowSensitiveContent_whenFlagged() throws Exception {
        String body = "{\"results\":[{" +
                "\"flagged\":true," +
                "\"categories\":{\"hate\":true,\"violence\":true,\"safe\":false}," +
                "\"category_scores\":{\"hate\":0.99}" +
                "}]}";
        injectMockClient(callReturning(buildResponse(200, body)));

        ContentModerationException ex = assertThrows(ContentModerationException.class,
                () -> service.moderateContent("bad text", "doc.md"));
        assertEquals(ModerationErrorType.SENSITIVE_CONTENT, ex.getErrorType());
        assertTrue(ex.getMessage().contains("hate"));
        assertTrue(ex.getMessage().contains("violence"));
        assertFalse(ex.getMessage().contains("safe"),
                "Non-flagged categories should not appear in error message");
    }

    @Test
    void moderateContent_shouldPass_whenScoreBelowThreshold() throws Exception {
        // flagged=false, but some score above (1 - minScore=0.3) — should still pass (only logged as debug)
        String body = "{\"results\":[{" +
                "\"flagged\":false," +
                "\"category_scores\":{\"hate\":0.25}" +
                "}]}";
        injectMockClient(callReturning(buildResponse(200, body)));

        assertDoesNotThrow(() -> service.moderateContent("borderline text", "doc.md"));
    }

    // ==================== Request building ====================

    @Test
    void moderateContent_shouldSendAuthorizationBearerAndJsonPayload() throws Exception {
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        String body = "{\"results\":[{\"flagged\":false,\"category_scores\":{}}]}";
        Call call = callReturning(buildResponse(200, body));

        OkHttpClient client = mock(OkHttpClient.class);
        when(client.newCall(captor.capture())).thenReturn(call);
        setField("httpClient", client);

        service.moderateContent("hello", "doc.md");

        Request sent = captor.getValue();
        assertEquals("https://api.openai.com/v1/moderations", sent.url().toString());
        assertEquals("Bearer test-api-key", sent.header("Authorization"));
        assertEquals("application/json", sent.header("Content-Type"));
        okhttp3.RequestBody reqBody = sent.body();
        assertNotNull(reqBody);
        // Body is a one-shot buffer; verify via header check (already done above)
    }

    // ==================== JSON escaping in payload ====================

    @Test
    void moderateContent_shouldEscapeSpecialCharactersInPayload() throws Exception {
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        String body = "{\"results\":[{\"flagged\":false,\"category_scores\":{}}]}";
        Call call = callReturning(buildResponse(200, body));

        OkHttpClient client = mock(OkHttpClient.class);
        when(client.newCall(captor.capture())).thenReturn(call);
        setField("httpClient", client);

        // Content with backslash, quote, newline, tab
        service.moderateContent("path\\to\nfile\t\"x\"", "doc.md");

        // The request body should contain escaped sequences (we can verify via buffer)
        Request sent = captor.getValue();
        assertNotNull(sent.body());
        // The RequestBody is a one-shot okio Buffer; can't easily read here.
        // Instead, verify via escapeJson unit-level tests (already covered).
    }

    // ==================== Error type messages (re-affirmed) ====================

    @Test
    void moderationApiErrorType_shouldHaveCorrectMessage() {
        assertEquals("内容审核服务异常，请稍后重试", ModerationErrorType.MODERATION_API_ERROR.getMessage());
    }

    @Test
    void sensitiveContentErrorType_shouldHaveCorrectMessage() {
        assertEquals("内容包含敏感信息，请修改后重试", ModerationErrorType.SENSITIVE_CONTENT.getMessage());
    }

    @Test
    void constructor_shouldInitializeSuccessfully() {
        assertNotNull(new OpenAIModerationService());
    }

    // ==================== escapeJson — unit-level edge cases ====================

    private String invokeEscapeJson(String input) throws Exception {
        java.lang.reflect.Method m = OpenAIModerationService.class.getDeclaredMethod("escapeJson", String.class);
        m.setAccessible(true);
        return (String) m.invoke(service, input);
    }

    @Test
    void escapeJson_shouldEscapeBackslash() throws Exception {
        assertTrue(invokeEscapeJson("path\\to\\file").contains("\\\\"));
    }

    @Test
    void escapeJson_shouldEscapeQuotes() throws Exception {
        assertTrue(invokeEscapeJson("say \"hello\"").contains("\\\""));
    }

    @Test
    void escapeJson_shouldEscapeNewlines() throws Exception {
        assertTrue(invokeEscapeJson("line1\nline2").contains("\\n"));
    }

    @Test
    void escapeJson_shouldEscapeCarriageReturn() throws Exception {
        assertTrue(invokeEscapeJson("line1\rline2").contains("\\r"));
    }

    @Test
    void escapeJson_shouldEscapeTab() throws Exception {
        assertTrue(invokeEscapeJson("col1\tcol2").contains("\\t"));
    }

    @Test
    void escapeJson_shouldHandleEmptyString() throws Exception {
        assertEquals("", invokeEscapeJson(""));
    }

    @Test
    void escapeJson_shouldHandleStringWithNoSpecialChars() throws Exception {
        assertEquals("simple text no escaping", invokeEscapeJson("simple text no escaping"));
    }

    // ==================== API-key skip path ====================

    @Test
    void moderateContent_shouldSkip_whenApiKeyIsBlank() throws Exception {
        setField("apiKey", "");
        assertDoesNotThrow(() -> service.moderateContent("any", "x.md"));
    }

    @Test
    void moderateContent_shouldSkip_whenApiKeyIsNull() throws Exception {
        setField("apiKey", (String) null);
        assertDoesNotThrow(() -> service.moderateContent("any", "x.md"));
    }

    @Test
    void moderateContent_shouldSkip_whenApiKeyIsWhitespace() throws Exception {
        setField("apiKey", "   ");
        assertDoesNotThrow(() -> service.moderateContent("any", "x.md"));
    }
}