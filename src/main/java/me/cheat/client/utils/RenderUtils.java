package me.cheat.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Timer;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class RenderUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static final int COLOR_PURPLE_PRIMARY = 0xFF7C3AED;
    public static final int COLOR_PURPLE_SECONDARY = 0xFF6D28D9;
    public static final int COLOR_PURPLE_TERTIARY = 0xFF4C1D95;
    public static final int COLOR_DARK_BG = 0xCC0F0A1E;
    public static final int COLOR_ACCENT = 0xFFA78BFA;
    public static final int COLOR_ACCENT2 = 0xFF8B5CF6;
    public static final int COLOR_TEXT = 0xFFE2E8F0;
    public static final int COLOR_TEXT_DIM = 0xFF94A3B8;
    public static final int COLOR_ENABLED = 0xFF22C55E;
    public static final int COLOR_DISABLED = 0xFFEF4444;

    public static int getColor(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int getColor(int r, int g, int b) {
        return getColor(r, g, b, 255);
    }

    public static void drawRect(double x, double y, double width, double height, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x, y + height, 0.0D).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).endVertex();
        worldrenderer.pos(x, y, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawGradientRect(double x, double y, double width, double height, int color1, int color2) {
        float a1 = (float) (color1 >> 24 & 255) / 255.0F;
        float r1 = (float) (color1 >> 16 & 255) / 255.0F;
        float g1 = (float) (color1 >> 8 & 255) / 255.0F;
        float b1 = (float) (color1 & 255) / 255.0F;
        float a2 = (float) (color2 >> 24 & 255) / 255.0F;
        float r2 = (float) (color2 >> 16 & 255) / 255.0F;
        float g2 = (float) (color2 >> 8 & 255) / 255.0F;
        float b2 = (float) (color2 & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(x, y + height, 0.0D).color(r2, g2, b2, a2).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).color(r2, g2, b2, a2).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).color(r1, g1, b1, a1).endVertex();
        worldrenderer.pos(x, y, 0.0D).color(r1, g1, b1, a1).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedRect(double x, double y, double width, double height, double radius, int color) {
        // CS:GO style rounded rect using corners
        drawRect(x + radius, y, width - radius * 2, height, color); // center
        drawRect(x, y + radius, width, height - radius * 2, color); // middle
        
        // Corners (simplified - just draw rects for corners)
        drawRect(x, y, radius, radius, color);
        drawRect(x + width - radius, y, radius, radius, color);
        drawRect(x, y + height - radius, radius, radius, color);
        drawRect(x + width - radius, y + height - radius, radius, radius, color);
    }

    public static void drawOutlinedRect(double x, double y, double width, double height, int color, float lineWidth) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x, y, 0.0D).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).endVertex();
        worldrenderer.pos(x, y + height, 0.0D).endVertex();
        worldrenderer.pos(x, y, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawText(String text, double x, double y, int color, boolean shadow) {
        FontRenderer fr = mc.fontRendererObj;
        if (shadow) {
            fr.drawString(text, (int)x + 1, (int)y + 1, (color & 0xFCFCFC) >> 2 | (color & 0xFF000000), false);
        }
        fr.drawString(text, (int)x, (int)y, color, false);
    }

    public static void drawCenteredText(String text, double x, double y, int color, boolean shadow) {
        int width = mc.fontRendererObj.getStringWidth(text);
        drawText(text, x - width / 2.0, y, color, shadow);
    }

    public static void drawEntityBox(Entity entity, int color, float lineWidth) {
        if (entity == null) return;

        float partialTicks = getRenderPartialTicks();
        RenderManager rm = mc.getRenderManager();
        double cameraX = rm.viewerPosX;
        double cameraY = rm.viewerPosY;
        double cameraZ = rm.viewerPosZ;
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - cameraX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - cameraY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - cameraZ;

        AxisAlignedBB bb = entity.getEntityBoundingBox();
        double w = (bb.maxX - bb.minX) / 2.0;
        double h = bb.maxY - bb.minY;

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(lineWidth);

        double x1 = x - w, x2 = x + w;
        double y1 = y, y2 = y + h;
        double z1 = z - w, z2 = z + w;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        // Wireframe box
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        // Bottom
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y1, z2).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z2).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        // Top
        wr.pos(x1, y2, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        wr.pos(x1, y2, z2).color(r, g, b, a).endVertex();
        wr.pos(x1, y2, z1).color(r, g, b, a).endVertex();
        // Vertical
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x1, y2, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y1, z2).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z2).color(r, g, b, a).endVertex();
        wr.pos(x1, y2, z2).color(r, g, b, a).endVertex();
        wr.pos(x2, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, z1).color(r, g, b, a).endVertex();
        tessellator.draw();

        // Filled box
        wr.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x1, y1, z1).color(r, g, b, a * 0.15f).endVertex();
        wr.pos(x2, y1, z1).color(r, g, b, a * 0.15f).endVertex();
        wr.pos(x1, y2, z1).color(r, g, b, a * 0.15f).endVertex();
        wr.pos(x2, y2, z1).color(r, g, b, a * 0.15f).endVertex();
        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawLine(Vec3 start, Vec3 end, int color, float lineWidth) {
        RenderManager rm = mc.getRenderManager();
        double cameraX = rm.viewerPosX;
        double cameraY = rm.viewerPosY;
        double cameraZ = rm.viewerPosZ;
        double x1 = start.xCoord - cameraX;
        double y1 = start.yCoord - cameraY;
        double z1 = start.zCoord - cameraZ;
        double x2 = end.xCoord - cameraX;
        double y2 = end.yCoord - cameraY;
        double z2 = end.zCoord - cameraZ;

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GL11.glLineWidth(lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static Timer cachedTimer = null;

    private static float getRenderPartialTicks() {
        if (cachedTimer == null) {
            cachedTimer = ReflectionHelper.getPrivateValue(Minecraft.class, mc, "timer", "field_71428_T");
        }
        return cachedTimer.renderPartialTicks;
    }

    public static int getRainbowColor(int offset, float speed) {
        long time = System.currentTimeMillis();
        float hue = (time % 3600) / 3600.0f + offset / 100.0f;
        return Color.HSBtoRGB(hue, 0.8f, 1.0f) | 0xFF000000;
    }

    public static int getColorWithAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int lerpColor(int color1, int color2, float t) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int getHealthColor(EntityLivingBase entity) {
        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float ratio = health / maxHealth;
        int r = (int) (255 * (1 - ratio));
        int g = (int) (255 * ratio);
        return getColor(r, g, 50);
    }
}
