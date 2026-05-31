package com.ai.repo.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

/**
 * Utility for generating default avatar images for agents.
 * Produces a rounded-square avatar with a colored background and
 * the first character of the agent name.
 */
public class AvatarUtil {

    private static final int SIZE = 200;
    private static final int ARC = 32;

    private static final List<Color> PALETTE = List.of(
        new Color(0x4A90D9), // blue
        new Color(0x50C878), // green
        new Color(0xE67E22), // orange
        new Color(0x9B59B6), // purple
        new Color(0xE74C3C), // red
        new Color(0x1ABC9C), // teal
        new Color(0xF39C12), // yellow
        new Color(0x3498DB), // light blue
        new Color(0x2ECC71), // emerald
        new Color(0xE91E63), // pink
        new Color(0x00BCD4), // cyan
        new Color(0xFF5722)  // deep orange
    );

    private AvatarUtil() {}

    /**
     * Generate a default avatar image file for an agent.
     *
     * @param agentId   the agent's database ID
     * @param agentName the agent's name (first character is used on the avatar)
     * @param saveDir   the directory to save the avatar file into (will be created if absent)
     * @return the relative URL path for the avatar, e.g. "/avatars/agents/{agentId}/{filename}"
     * @throws IOException if file writing fails
     */
    public static String generateDefaultAvatar(Long agentId, String agentName, Path saveDir) throws IOException {
        Files.createDirectories(saveDir);

        Random random = new Random(agentId);
        Color bgColor = PALETTE.get(random.nextInt(PALETTE.size()));

        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw rounded rectangle background
        RoundRectangle2D rounded = new RoundRectangle2D.Float(0, 0, SIZE, SIZE, ARC, ARC);
        g2d.setColor(bgColor);
        g2d.fill(rounded);

        // Draw initial letter
        String initial = (agentName != null && !agentName.isEmpty())
                ? String.valueOf(Character.toUpperCase(agentName.charAt(0)))
                : "?";

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 100));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (SIZE - fm.stringWidth(initial)) / 2;
        int textY = (SIZE - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(initial, textX, textY);
        g2d.dispose();

        String fileName = agentId + "_default.png";
        ImageIO.write(image, "png", saveDir.resolve(fileName).toFile());

        return "/avatars/agents/" + agentId + "/" + fileName;
    }
}
