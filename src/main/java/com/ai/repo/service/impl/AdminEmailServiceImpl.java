package com.ai.repo.service.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ai.repo.entity.User;
import com.ai.repo.mapper.UserMapper;
import com.ai.repo.service.AdminEmailService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AdminEmailServiceImpl implements AdminEmailService {

    @Resource
    private JavaMailSender javaMailSender;
    @Resource
    private UserMapper userMapper;

    @Value("${mail.from:noreply@logicoma.ai}")
    private String fromEmail;
    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    @Async
    public void sendToActiveAdmins(String subject, String body, String actionUrl) {
        Set<String> recipients = activeAdminEmails();
        send(recipients, subject, body, actionUrl);
    }

    @Override
    @Async
    public void sendToAdminOwner(Long userId, String subject, String body, String actionUrl) {
        User owner = userMapper.selectById(userId);
        Set<String> recipients = new LinkedHashSet<>();
        if (owner != null && "ADMIN".equals(owner.getRole()) && "ACTIVE".equals(owner.getStatus())
                && owner.getEmail() != null && !owner.getEmail().isBlank()) {
            recipients.add(owner.getEmail().trim().toLowerCase());
        }
        send(recipients, subject, body, actionUrl);
    }

    private void send(Set<String> recipients, String subject, String body, String actionUrl) {
        if (recipients.isEmpty()) {
            log.warn("Admin email skipped: no ACTIVE ADMIN email, subject={}", subject);
            return;
        }
        for (String recipient : recipients) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(recipient);
                message.setSubject("[LOGICOMA.NET] " + subject);
                message.setText(buildBody(body, actionUrl));
                javaMailSender.send(message);
                log.info("Admin email sent: recipient={}, subject={}", maskEmail(recipient), subject);
            } catch (Exception exception) {
                log.error("Admin email failed: recipient={}, subject={}",
                        maskEmail(recipient), subject, exception);
            }
        }
    }

    private Set<String> activeAdminEmails() {
        List<User> admins = userMapper.selectByRole("ADMIN");
        Set<String> emails = new LinkedHashSet<>();
        for (User admin : admins) {
            if ("ACTIVE".equals(admin.getStatus()) && admin.getEmail() != null && !admin.getEmail().isBlank()) {
                emails.add(admin.getEmail().trim().toLowerCase());
            }
        }
        return emails;
    }

    private String buildBody(String body, String actionUrl) {
        if (actionUrl == null || actionUrl.isBlank()) {
            return body;
        }
        return body + "\n\n查看详情: " + absoluteUrl(actionUrl);
    }

    private String absoluteUrl(String path) {
        if (path.startsWith("https://") || path.startsWith("http://")) {
            return path;
        }
        return frontendUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }
}
