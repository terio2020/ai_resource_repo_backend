package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.dto.CaptchaResponse;
import com.ai.repo.dto.CaptchaVerifyRequest;
import com.ai.repo.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/captcha")
@Tag(name = "Captcha API", description = "Slide puzzle captcha verification")
public class CaptchaController {

    @Resource
    private CaptchaService captchaService;

    @GetMapping("/generate")
    @Operation(summary = "Generate captcha", description = "Generate a new slide puzzle captcha")
    public Result<CaptchaResponse> generateCaptcha() {
        CaptchaService.CaptchaResult result = captchaService.generateCaptcha();

        CaptchaResponse response = new CaptchaResponse();
        response.setId(result.getId());
        response.setPuzzleImage(result.getPuzzleImage());

        return Result.success(response);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify captcha", description = "Verify the slide puzzle captcha")
    public Result<Boolean> verifyCaptcha(@RequestBody CaptchaVerifyRequest request) {
        boolean valid = captchaService.verifyCaptcha(request.getId(), request.getMoveX());
        return Result.success(valid);
    }
}
