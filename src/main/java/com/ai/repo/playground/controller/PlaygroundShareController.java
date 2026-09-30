package com.ai.repo.playground.controller;

import com.ai.repo.common.Result;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.playground.service.PlaygroundShareService;
import com.ai.repo.security.RequireAuth;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/api/playground")
@Validated
@ConditionalOnProperty(name="playground.enabled",havingValue="true")
public class PlaygroundShareController {
    private final PlaygroundShareService shares;
    public PlaygroundShareController(PlaygroundShareService shares) { this.shares=shares; }

    @GetMapping("/activities/{activityId}/share") @RequireAuth
    public ResponseEntity<Result<ObjectNode>> resultLink(@PathVariable @Min(1) long activityId,HttpServletRequest request) {
        return Result.ok(shares.ownerResultLink(human(request),activityId));
    }
    @GetMapping("/shares/{token}")
    public ResponseEntity<Result<JsonNode>> publicResult(@PathVariable String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Result.success(shares.publicResult(token)));
    }
    @GetMapping(value="/shares/{token}/landing",produces=MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> landing(@PathVariable String token) {
        JsonNode result=shares.publicResult(token);
        String shop=result.path("shop").path("name").asText("一间未命名的小店");
        String headline=result.path("outcome").asText().equals("UNOPENED")
                ? "他们本想开一家"+shop+"，最后没能签约" : "两只 Agent 合伙开的"+shop+"，结局如何？";
        String description=result.path("shop").path("concept").asText("看看两只 Agent 自主商量后的结局");
        if (description.length()>150) description=description.substring(0,150);
        String title=HtmlUtils.htmlEscape(headline),summary=HtmlUtils.htmlEscape(description);
        String destination="/playground/s/"+token;
        String html="<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">"
                +"<meta name=\"robots\" content=\"noindex\">"
                +"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                +"<meta property=\"og:type\" content=\"article\"><meta property=\"og:title\" content=\""+title+"\">"
                +"<meta property=\"og:description\" content=\""+summary+"\">"
                +"<meta name=\"twitter:card\" content=\"summary\">"
                +"<meta http-equiv=\"refresh\" content=\"0;url="+destination+"\">"
                +"<title>"+title+"</title></head><body><a href=\""+destination+"\">查看 Agent 的结局</a></body></html>";
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.TEXT_HTML)
                .header("X-Content-Type-Options","nosniff").body(html);
    }
    private long human(HttpServletRequest request) {
        if (request.getAttribute("agentId")!=null) throw new BusinessException(403,"HUMAN_JWT_REQUIRED");
        Object user=request.getAttribute("userId");
        if (!(user instanceof Long id) || id<=0) throw new BusinessException(401,"AUTHENTICATION_REQUIRED");
        return id;
    }
}
