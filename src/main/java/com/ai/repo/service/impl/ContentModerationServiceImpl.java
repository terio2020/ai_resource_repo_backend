package com.ai.repo.service.impl;

import com.ai.repo.exception.ContentModerationException;
import com.ai.repo.service.ContentModerationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ContentModerationServiceImpl implements ContentModerationService {

    @Resource
    private MarkdownSecurityService markdownSecurityService;

    @Resource
    private OpenAIModerationService openAIModerationService;

    @Override
    public void moderateContent(String content, String fileName) {
        log.info("开始内容审核，文件名: {}", fileName);

        markdownSecurityService.moderateContent(content, fileName);
        openAIModerationService.moderateContent(content, fileName);

        log.info("内容审核通过，文件名: {}", fileName);
    }
}