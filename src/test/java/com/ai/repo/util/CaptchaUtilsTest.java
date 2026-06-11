package com.ai.repo.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaptchaUtilsTest {

    @Test
    void generateRandomTargetX_shouldReturnWithinRange() {
        int x = CaptchaUtils.generateRandomTargetX();
        assertTrue(x >= 80, "X should be >= 80, but was " + x);
        assertTrue(x <= 247, "X should be <= 247, but was " + x);
    }

    @Test
    void generateRandomTargetX_shouldReturnDifferentValues() {
        int x1 = CaptchaUtils.generateRandomTargetX();
        int x2 = CaptchaUtils.generateRandomTargetX();
        // Very unlikely to be the same twice
        assertNotEquals(x1, x2);
    }

    @Test
    @Disabled("Requires background images in resources/captcha/")
    void generatePuzzleImage_shouldReturnBase64() {
        String image = CaptchaUtils.generatePuzzleImage(100, 50);
        assertNotNull(image);
        assertTrue(image.startsWith("data:image/png;base64,"));
    }
}
