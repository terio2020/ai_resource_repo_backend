package com.ai.repo.service.impl;

import com.ai.repo.exception.ContentModerationException;
import com.ai.repo.exception.ContentModerationException.ModerationErrorType;
import com.ai.repo.service.ContentModerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MarkdownSecurityService implements ContentModerationService {

    private static final Pattern IMAGE_PATTERN =
        Pattern.compile("!\\[.*?\\]\\(.*?\\)", Pattern.DOTALL);

    private static final Pattern HTML_TAG_PATTERN =
        Pattern.compile("<[^>]+>", Pattern.DOTALL);

    private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN =
        Pattern.compile("(javascript|vbscript|data):", Pattern.CASE_INSENSITIVE);

    private static final Pattern EVENT_HANDLER_PATTERN =
        Pattern.compile("\\bon\\w+\\s*=", Pattern.CASE_INSENSITIVE);

    private static final Pattern SSRF_IP_PATTERN =
        Pattern.compile("(http|https|ftp)://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", Pattern.CASE_INSENSITIVE);

    private static final Pattern SSRF_PRIVATE_DNS_PATTERN =
        Pattern.compile("(http|https)://[^/]+\\.(local|internal|intranet|localhost)", Pattern.CASE_INSENSITIVE);

    @Override
    public void moderateContent(String content, String fileName) {
        log.debug("开始内容安全检测，文件名: {}", fileName);

        checkForImages(content, fileName);
        checkForXss(content, fileName);
        checkForSSRF(content, fileName);

        log.debug("内容安全检测通过");
    }

    private void checkForImages(String content, String fileName) {
        Matcher matcher = IMAGE_PATTERN.matcher(content);
        if (matcher.find()) {
            String found = matcher.group();
            log.warn("检测到禁止的图片格式，文件: {}, 内容: {}", fileName, found);
            throw new ContentModerationException(ModerationErrorType.IMAGE_NOT_ALLOWED, found);
        }
    }

    private void checkForXss(String content, String fileName) {
        Matcher htmlMatcher = HTML_TAG_PATTERN.matcher(content);
        if (htmlMatcher.find()) {
            String htmlTag = htmlMatcher.group();

            if (JAVASCRIPT_PROTOCOL_PATTERN.matcher(htmlTag).find()) {
                log.warn("检测到JavaScript伪协议，文件: {}", fileName);
                throw new ContentModerationException(ModerationErrorType.XSS_DETECTED, htmlTag);
            }

            if (EVENT_HANDLER_PATTERN.matcher(htmlTag).find()) {
                log.warn("检测到事件处理器，文件: {}", fileName);
                throw new ContentModerationException(ModerationErrorType.XSS_DETECTED, htmlTag);
            }
        }
    }

    private void checkForSSRF(String content, String fileName) {
        Matcher ipMatcher = SSRF_IP_PATTERN.matcher(content);
        while (ipMatcher.find()) {
            String url = ipMatcher.group();
            if (isPrivateIp(url)) {
                log.warn("检测到内网IP访问，文件: {}, URL: {}", fileName, url);
                throw new ContentModerationException(ModerationErrorType.SSRF_DETECTED, url);
            }
        }

        Matcher dnsMatcher = SSRF_PRIVATE_DNS_PATTERN.matcher(content);
        if (dnsMatcher.find()) {
            String url = dnsMatcher.group();
            log.warn("检测到内网域名访问，文件: {}, URL: {}", fileName, url);
            throw new ContentModerationException(ModerationErrorType.SSRF_DETECTED, url);
        }
    }

    private boolean isPrivateIp(String url) {
        String ip = url.replaceFirst("(http|https|ftp)://", "");
        ip = ip.split("/")[0];
        ip = ip.split(":")[0];

        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;

        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);

            if (first == 10) return true;
            if (first == 172 && second >= 16 && second <= 31) return true;
            if (first == 192 && second == 168) return true;
            if (first == 127) return true;
            if (first == 0) return true;
            if (first == 169 && second == 254) return true;

        } catch (NumberFormatException e) {
            return false;
        }

        return false;
    }
}