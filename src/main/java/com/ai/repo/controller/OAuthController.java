package com.ai.repo.controller;

import com.ai.repo.dto.LoginResponse;
import com.ai.repo.entity.SocialAccount;
import com.ai.repo.entity.User;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.service.SocialAccountService;
import com.ai.repo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/oauth")
@Tag(name = "OAuth API", description = "Social login via OAuth providers")
public class OAuthController {

    @Resource
    private SocialAccountService socialAccountService;

    @Resource
    private UserService userService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @GetMapping("/{provider}")
    @Operation(summary = "Initiate OAuth login", description = "Redirect to provider's authorization page")
    public ResponseEntity<RedirectView> initiateOAuth(
            @Parameter(description = "OAuth provider (google, github)") @PathVariable String provider,
            @Parameter(description = "Redirect URL after successful login") @RequestParam(required = false) String redirect_uri) {

        if (!socialAccountService.isProviderConfigured(provider)) {
            throw new BusinessException(400, "OAuth provider not configured: " + provider);
        }

        String state = socialAccountService.generateState(redirect_uri);
        String authUrl = socialAccountService.buildAuthorizationUrl(provider, state);

        log.info("Initiating OAuth flow for provider: {}", provider);
        return ResponseEntity.status(HttpStatus.FOUND).header("Location", authUrl).build();
    }

    @GetMapping("/{provider}/callback")
    @Operation(summary = "OAuth callback", description = "Handle OAuth provider callback")
    public ResponseEntity<Void> handleOAuthCallback(
            @Parameter(description = "OAuth provider") @PathVariable String provider,
            @Parameter(description = "Authorization code") @RequestParam String code,
            @Parameter(description = "State token") @RequestParam String state) {

        String redirectUri = socialAccountService.validateAndExtractState(state);
        if (redirectUri == null) {
            throw new BusinessException(400, "Invalid state parameter");
        }

        log.info("Handling OAuth callback for provider: {}", provider);

        Map<String, Object> userInfo = socialAccountService.exchangeCodeForUserInfo(provider, code);

        if (userInfo == null) {
            log.error("Failed to get user info from provider: {}", provider);
            throw new BusinessException(400, "Failed to authenticate with " + provider);
        }

        String providerUserId = (String) userInfo.get("id");
        String email = (String) userInfo.get("email");
        String nickname = (String) userInfo.get("name");
        String avatar = (String) userInfo.get("picture");
        String socialAccessToken = (String) userInfo.get("access_token");
        String socialRefreshToken = (String) userInfo.get("refresh_token");
        Long expiresIn = userInfo.get("expires_in") != null ? ((Number) userInfo.get("expires_in")).longValue() : null;

        User user = socialAccountService.authenticateWithSocialAccount(provider, providerUserId);

        if (user == null) {
            user = socialAccountService.linkSocialAccountToNewUser(
                    provider, providerUserId, email, nickname, avatar,
                    socialAccessToken, socialRefreshToken, expiresIn
            );
            log.info("Created new user via {} social login: {}", provider, user.getUsername());
        } else {
            SocialAccount socialAccount = socialAccountService.findByUserIdAndProvider(user.getId(), provider);
            if (socialAccount != null) {
                socialAccountService.updateTokens(socialAccount.getId(), socialAccessToken, socialRefreshToken, expiresIn);
            }
            log.info("User logged in via {} social login: {}", provider, user.getUsername());
        }

        LoginResponse response = userService.generateTokens(user.getId());
        userService.updateLoginTime(user.getId());

        String frontendCallback = frontendUrl + "/oauth/" + provider + "/callback"
                + "?accessToken=" + URLEncoder.encode(response.getAccessToken(), StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(response.getRefreshToken(), StandardCharsets.UTF_8)
                + "&userId=" + response.getId()
                + "&username=" + URLEncoder.encode(response.getUsername(), StandardCharsets.UTF_8)
                + "&nickname=" + URLEncoder.encode(response.getNickname() != null ? response.getNickname() : "", StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(response.getEmail() != null ? response.getEmail() : "", StandardCharsets.UTF_8)
                + "&avatar=" + URLEncoder.encode(response.getAvatar() != null ? response.getAvatar() : "", StandardCharsets.UTF_8)
                + "&hasPassword=" + response.getHasPassword();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(frontendCallback));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
