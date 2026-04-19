package com.ai.repo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Random;

@Slf4j
public class CaptchaUtils {

    // 拼图尺寸配置
    private static final int IMAGE_WIDTH = 310;
    private static final int IMAGE_HEIGHT = 155;
    private static final int BLOCK_WIDTH = 42;
    private static final int BLOCK_HEIGHT = 42;
    // 背景图列表（放到resources/captcha/目录下，支持自定义添加）
    private static final String[] BACKGROUND_IMAGES = {
            "captcha/bg1.jpg",
            "captcha/bg2.jpg",
            "captcha/bg3.jpg"
    };

    private static final Random RANDOM = new Random();

    /**
     * 生成滑动拼图验证码
     *
     * @param targetX 目标X位置（验证块的正确位置）
     * @return base64编码的拼图图片
     */
    public static String generatePuzzleImage(int targetX, int targetY) {
        try {
            // 随机选择背景图
            String bgPath = BACKGROUND_IMAGES[RANDOM.nextInt(BACKGROUND_IMAGES.length)];
            InputStream is = new ClassPathResource(bgPath).getInputStream();
            BufferedImage originalBg = ImageIO.read(is);
            
            // 缩放背景图到验证码尺寸
            BufferedImage scaledBg = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D scaleGd = scaledBg.createGraphics();
            scaleGd.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            scaleGd.drawImage(originalBg, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, null);
            scaleGd.dispose();
            is.close();

            // 从背景图抠出拼图块
            BufferedImage block = new BufferedImage(BLOCK_WIDTH, BLOCK_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D blockGd = block.createGraphics();
            blockGd.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            blockGd.drawImage(scaledBg, 0, 0, BLOCK_WIDTH, BLOCK_HEIGHT, 
                    targetX, targetY, targetX + BLOCK_WIDTH, targetY + BLOCK_HEIGHT, null);
            
            // 给拼图块加边框
            blockGd.setColor(new Color(0, 240, 255));
            blockGd.setStroke(new BasicStroke(2));
            blockGd.drawRect(0, 0, BLOCK_WIDTH - 1, BLOCK_HEIGHT - 1);
            blockGd.dispose();

            // 在背景图目标位置绘制缺口（半透明阴影）
            Graphics2D g2d = scaledBg.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawGap(g2d, targetX, targetY);

            // 把拼图块放到图片最右侧，供用户拖动
            BufferedImage finalImage = new BufferedImage(IMAGE_WIDTH + BLOCK_WIDTH + 10, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D finalGd = finalImage.createGraphics();
            finalGd.drawImage(scaledBg, 0, 0, null);
            finalGd.drawImage(block, IMAGE_WIDTH + 5, targetY, null);
            // 分隔线
            finalGd.setColor(new Color(220, 220, 220));
            finalGd.drawLine(IMAGE_WIDTH + 2, 0, IMAGE_WIDTH + 2, IMAGE_HEIGHT);
            finalGd.dispose();

            g2d.dispose();

            // 转换为Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(finalImage, "PNG", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("生成验证码失败，可能是缺少背景图，请在resources/captcha/目录下添加bg1.jpg/bg2.jpg/bg3.jpg", e);
            throw new RuntimeException("生成验证码失败", e);
        }
    }

    /**
     * 绘制缺口形状
     */
    private static void drawGap(Graphics2D g2d, int x, int y) {
        // 半透明黑色阴影缺口
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        g2d.setColor(new Color(0, 0, 0));
        g2d.fillRect(x, y, BLOCK_WIDTH, BLOCK_HEIGHT);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        // 霓虹青色边框，和拼图块边框统一
        g2d.setColor(new Color(0, 240, 255));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x, y, BLOCK_WIDTH - 1, BLOCK_HEIGHT - 1);
    }

    /**
     * 生成随机验证码位置（验证块的正确位置）
     */
    public static int generateRandomTargetX() {
        // 验证块X坐标在 80 ~ (宽度-滑块宽度-20)之间，避免太靠边
        return 80 + RANDOM.nextInt(IMAGE_WIDTH - BLOCK_WIDTH - 100);
    }
}
