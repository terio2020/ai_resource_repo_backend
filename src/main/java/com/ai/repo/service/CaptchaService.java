package com.ai.repo.service;

import com.ai.repo.util.CaptchaUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CaptchaService {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;
    private static final int CAPTCHA_TOLERANCE = 5;  // 允许5px误差

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 生成新的验证码
     *
     * @return 验证码ID和图片
     */
    public CaptchaResult generateCaptcha() {
        String id = UUID.randomUUID().toString().replace("-", "");
        int targetX = CaptchaUtils.generateRandomTargetX();
        int targetY = 20 + new Random().nextInt(60); // Y轴随机20~80之间

        String puzzleImage = CaptchaUtils.generatePuzzleImage(targetX, targetY);

        // 存储到Redis
        String redisKey = CAPTCHA_PREFIX + id;
        redisTemplate.opsForValue().set(redisKey, targetX, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("生成验证码成功, id: {}, targetX: {}, targetY: {}", id, targetX, targetY);

        return new CaptchaResult(id, puzzleImage, targetY);
    }

    /**
     * 验证验证码
     *
     * @param id 验证码ID
     * @param moveX 用户滑动的X位置
     * @return 是否验证成功
     */
    public boolean verifyCaptcha(String id, Integer moveX) {
        if (id == null || id.isEmpty()) {
            log.warn("验证码ID为空");
            return false;
        }

        if (moveX == null) {
            log.warn("滑动位置为空, id: {}", id);
            return false;
        }

        String redisKey = CAPTCHA_PREFIX + id;
        Integer targetX = (Integer) redisTemplate.opsForValue().get(redisKey);

        if (targetX == null) {
            log.warn("验证码已过期或不存在, id: {}", id);
            return false;
        }

        // 验证位置误差
        boolean valid = Math.abs(targetX - moveX) <= CAPTCHA_TOLERANCE;

        if (valid) {
            // 验证成功，删除验证码（一次性使用）
            redisTemplate.delete(redisKey);
            log.info("验证码验证成功, id: {}, moveX: {}, targetX: {}", id, moveX, targetX);
        } else {
            log.warn("验证码验证失败, id: {}, moveX: {}, targetX: {}, 差值: {}",
                    id, moveX, targetX, Math.abs(targetX - moveX));
        }

        return valid;
    }

    /**
     * 验证结果内部类
     */
    public static class CaptchaResult {
        private String id;
        private String puzzleImage;
        private Integer targetY;

        public CaptchaResult(String id, String puzzleImage, Integer targetY) {
            this.id = id;
            this.puzzleImage = puzzleImage;
            this.targetY = targetY;
        }

        public String getId() {
            return id;
        }

        public String getPuzzleImage() {
            return puzzleImage;
        }

        public Integer getTargetY() {
            return targetY;
        }
    }
}
