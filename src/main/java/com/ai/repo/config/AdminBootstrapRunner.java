package com.ai.repo.config;

import com.ai.repo.entity.User;
import com.ai.repo.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Order(1)
public class AdminBootstrapRunner implements CommandLineRunner {

    @Resource
    private UserMapper userMapper;

    @Value("${admin.bootstrap-emails:}")
    private String bootstrapEmails;

    @Override
    public void run(String... args) {
        try {
            List<String> emails = parseEmails(bootstrapEmails);
            if (emails.isEmpty()) {
                log.info("[BOOTSTRAP] admin.bootstrap-emails empty, skip admin bootstrap");
                return;
            }
            for (String email : emails) {
                bootstrapAdmin(email.trim());
            }
        } catch (Exception e) {
            log.warn("[BOOTSTRAP] admin bootstrap failed: {}", e.getMessage());
        }
    }

    private void bootstrapAdmin(String email) {
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            log.warn("[BOOTSTRAP] admin email not found: {}", email);
            return;
        }
        if ("ADMIN".equals(user.getRole()) && "ACTIVE".equals(user.getStatus())) {
            log.info("[BOOTSTRAP] admin already configured: {}", email);
            return;
        }
        userMapper.updateRoleAndStatus(user.getId(), "ADMIN", "ACTIVE");
        log.info("[BOOTSTRAP] promoted {} to ADMIN (ACTIVE)", email);
    }

    private List<String> parseEmails(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.asList(value.split(","));
    }
}
