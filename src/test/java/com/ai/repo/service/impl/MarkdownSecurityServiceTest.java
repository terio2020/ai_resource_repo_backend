package com.ai.repo.service.impl;

import com.ai.repo.exception.ContentModerationException;
import com.ai.repo.exception.ContentModerationException.ModerationErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownSecurityServiceTest {

    private MarkdownSecurityService markdownSecurityService;

    @BeforeEach
    void setUp() {
        markdownSecurityService = new MarkdownSecurityService();
    }

    // ===== 正常内容测试 =====

    @Test
    void moderateContent_shouldPass_whenCleanContent() {
        String content = "# Hello World\n\nThis is a clean markdown file.";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "clean.md"));
    }

    @Test
    void moderateContent_shouldPass_whenContentHasOnlyText() {
        String content = "Simple text without any special characters or patterns.";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "simple.txt"));
    }

    @Test
    void moderateContent_shouldPass_whenContentHasCodeBlocks() {
        String content = "```java\npublic class Test {\n    private int value;\n}\n```";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "code.java"));
    }

    @Test
    void moderateContent_shouldPass_whenContentHasLinks() {
        String content = "Check out [this link](https://example.com) for more info.";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "link.md"));
    }

    @Test
    void moderateContent_shouldPass_whenContentHasMultipleHeaders() {
        String content = "# Title\n## Section 1\n### Subsection 1.1\n## Section 2";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "headers.md"));
    }

    @Test
    void moderateContent_shouldPass_whenContentHasList() {
        String content = "- item 1\n- item 2\n- item 3";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "list.md"));
    }

    // ===== 图片检测测试 =====

    @Test
    void moderateContent_shouldThrow_whenMarkdownImageSyntax() {
        String content = "Here is an image: ![alt text](https://example.com/image.png)";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "image.md"));
        assertEquals(ModerationErrorType.IMAGE_NOT_ALLOWED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenMarkdownImageSyntaxWithNoAlt() {
        String content = "![](/path/to/image.jpg)";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "image-no-alt.md"));
        assertEquals(ModerationErrorType.IMAGE_NOT_ALLOWED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenMultipleImages() {
        String content = "First ![img1](url1.png) then ![img2](url2.png)";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "multi-image.md"));
        assertEquals(ModerationErrorType.IMAGE_NOT_ALLOWED, exception.getErrorType());
    }

    // ===== XSS 检测测试 =====

    @Test
    void moderateContent_shouldThrow_whenJavascriptProtocol() {
        String content = "<a href=\"javascript:alert('XSS')\">Click me</a>";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-js-protocol.html"));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenVbscriptProtocol() {
        String content = "<img src=\"vbscript:msgbox('XSS')\">";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-vbscript.html"));
        assertEquals(ModerationErrorType.IMAGE_NOT_ALLOWED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenVbscriptProtocolInAnchor() {
        String content = "<a href=\"vbscript:msgbox('XSS')\">Click</a>";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-vbscript-anchor.html"));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenDataProtocol() {
        String content = "<a href=\"data:text/html,<script>alert('XSS')</script>\">Click</a>";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-data.html"));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenEventHandlerOnClick() {
        String content = "<button onclick=\"alert('XSS')\">Click me</button>";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-onclick.html"));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenEventHandlerOnLoad() {
        String content = "<img src=\"x.jpg\" onload=\"alert('XSS')\">";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-onload.html"));
        assertEquals(ModerationErrorType.IMAGE_NOT_ALLOWED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenEventHandlerOnLoadInDiv() {
        String content = "<div onload=\"alert('XSS')\">test</div>";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-onload-div.html"));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenEventHandlerOnError() {
        String content = "<div onerror=\"alert('XSS')\">Error</div>";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-onerror.html"));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenInlineEventHandler() {
        String content = "<span onmouseover=\"alert('XSS')\">Hover me</span>";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-mouseover.html"));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenInlineScriptTag() {
        String content = "<script onclick=\"alert('XSS')\">evil</script>";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-script.html"));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenScriptTagAlone() {
        String content = "<script>alert(1)</script>";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-script-only.html"));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenIframeTag() {
        String content = "<iframe src=\"//evil.example.com\"></iframe>";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-iframe.html"));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenObjectTag() {
        String content = "<object data=\"evil.swf\"></object>";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-object.html"));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenFormTag() {
        String content = "<form action=\"//evil.com\"><button>Go</button></form>";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-form.html"));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenMetaRefreshTag() {
        String content = "<meta http-equiv=\"refresh\" content=\"0;url=//evil.com\">";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "xss-meta.html"));
        assertEquals(ModerationErrorType.XSS_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenBareLocalhost() {
        String content = "Internal API: http://localhost:8080/admin";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-localhost-bare.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenIPv6Loopback() {
        String content = "Loopback v6: http://[::1]/admin";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-ipv6.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    // ===== SSRF 检测测试 =====

    @Test
    void moderateContent_shouldThrow_whenPrivateIP_10_0_0_0() {
        String content = "Fetch data from http://10.0.0.1/internal/api";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-10.x.x.x.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenPrivateIP_10_x_x_x() {
        String content = "Data source: https://10.255.255.255/secret";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-10-255.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenPrivateIP_172_16_0_0() {
        String content = "API endpoint: http://172.16.0.100:8080/internal";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-172.16.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenPrivateIP_172_31_255_255() {
        String content = "Server at https://172.31.255.255:443/api";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-172.31.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenPrivateIP_192_168_0_0() {
        String content = "Local server: ftp://192.168.0.1/files";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-192.168.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenPrivateIP_192_168_1_1() {
        String content = "Router admin: http://192.168.1.1/admin";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-192.168.1.1.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenPrivateIP_127_0_0_1() {
        String content = "Localhost API: http://127.0.0.1:3000/api";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-localhost.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenPrivateIP_127_0_0_0() {
        String content = "Another localhost variant: http://127.0.0.0";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-127.0.0.0.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenPrivateIP_169_254_0_0() {
        String content = "Link-local: http://169.254.0.1/endpoint";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-169.254.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenPrivateDNS_localhost() {
        String content = "Internal server: https://server.localhost.internal/home";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-localhost-dns.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenPrivateDNS_internal() {
        String content = "Internal API: http://api.internal.corp/api";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-internal-dns.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenPrivateDNS_intranet() {
        String content = "Intranet portal: https://portal.intranet.company.com";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-intranet-dns.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    @Test
    void moderateContent_shouldThrow_whenPrivateDNS_local() {
        String content = "Local dev server: http://app.local/api";
        ContentModerationException exception = assertThrows(ContentModerationException.class,
                () -> markdownSecurityService.moderateContent(content, "ssrf-local-dns.md"));
        assertEquals(ModerationErrorType.SSRF_DETECTED, exception.getErrorType());
    }

    // ===== Public IP 应该通过 =====

    @Test
    void moderateContent_shouldPass_whenPublicIP() {
        String content = "Public server: https://8.8.8.8/dns-query";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "public-ip.md"));
    }

    @Test
    void moderateContent_shouldPass_whenPublicDomain() {
        String content = "External API: https://api.github.com/users";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "public-domain.md"));
    }

    @Test
    void moderateContent_shouldPass_whenGoogleDNS() {
        String content = "Google DNS check: https://dns.google/resolve";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "google-dns.md"));
    }

    // ===== 边界条件测试 =====

    @Test
    void moderateContent_shouldPass_whenEmptyContent() {
        String content = "";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "empty.md"));
    }

    @Test
    void moderateContent_shouldPass_whenOnlyWhitespace() {
        String content = "   \n\n   \n   ";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "whitespace.md"));
    }

    @Test
    void moderateContent_shouldPass_whenNormalUrlInText() {
        String content = "Visit https://www.example.com/path?query=value for details.";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "normal-url.md"));
    }

    @Test
    void moderateContent_shouldPass_whenUrlWithPortPublic() {
        String content = "Service running on public server: https://example.com:8080/api";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "url-with-port.md"));
    }

    @Test
    void moderateContent_shouldPass_whenNormalTableSyntax() {
        String content = "| Header 1 | Header 2 |\n|----------|----------|\n| Cell 1   | Cell 2   |";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "table.md"));
    }

    @Test
    void moderateContent_shouldPass_whenNormalBlockquote() {
        String content = "> This is a blockquote\n> with multiple lines";
        assertDoesNotThrow(() -> markdownSecurityService.moderateContent(content, "blockquote.md"));
    }
}