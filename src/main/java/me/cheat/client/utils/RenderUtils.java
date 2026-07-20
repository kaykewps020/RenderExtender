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

    // Purple CS:GO palette
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

    // ─── Basic Primitives ───────────────────────────────────────

    public static int getColor(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int getColor(int r, int g, int b) {
        return getColor(r, g, b, 255);
    }

    public static int getColorWithAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int lerpColor(int color1, int color2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a1 = (color1 >> 24) & 0xFF, r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF, r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        int a = (int)(a1 + (a2 - a1) * t), r = (int)(r1 + (r2 - r1) * t), g = (int)(g1 + (g2 - g1) * t), b = (int)(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int getRainbowColor(int offset, float speed) {
        float hue = (System.currentTimeMillis() % 3600) / 3600.0f + offset / 100.0f;
        return Color.HSBtoRGB(hue, 0.8f, 1.0f) | 0xFF000000;
    }

    public static int getHealthColor(EntityLivingBase entity) {
        float ratio = entity.getHealth() / entity.getMaxHealth();
        return getColor((int)(255 * (1 - ratio)), (int)(255 * ratio), 50);
    }

    // ─── Rectangles ─────────────────────────────────────────────

    public static void drawRect(double x, double y, double w, double h, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION);
        wr.pos(x, y + h, 0).endVertex();
        wr.pos(x + w, y + h, 0).endVertex();
        wr.pos(x + w, y, 0).endVertex();
        wr.pos(x, y, 0).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawGradientRect(double x, double y, double w, double h, int c1, int c2) {
        float a1 = (c1 >> 24 & 255) / 255.0F, r1 = (c1 >> 16 & 255) / 255.0F, g1 = (c1 >> 8 & 255) / 255.0F, b1 = (c1 & 255) / 255.0F;
        float a2 = (c2 >> 24 & 255) / 255.0F, r2 = (c2 >> 16 & 255) / 255.0F, g2 = (c2 >> 8 & 255) / 255.0F, b2 = (c2 & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x, y + h, 0).color(r2, g2, b2, a2).endVertex();
        wr.pos(x + w, y + h, 0).color(r2, g2, b2, a2).endVertex();
        wr.pos(x + w, y, 0).color(r1, g1, b1, a1).endVertex();
        wr.pos(x, y, 0).color(r1, g1, b1, a1).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedRect(double x, double y, double w, double h, double radius, int color) {
        drawRect(x + radius, y, w - radius * 2, h, color);
        drawRect(x, y + radius, w, h - radius * 2, color);
        drawArc(x + radius, y + radius, radius, 90, 180, color);
        drawArc(x + w - radius, y + radius, radius, 0, 90, color);
        drawArc(x + radius, y + h - radius, radius, 180, 270, color);
        drawArc(x + w - radius, y + h - radius, radius, 270, 360, color);
    }

    public static void drawOutlinedRect(double x, double y, double w, double h, int color, float lineWidth) {
        float a = (color >> 24 & 255) / 255.0F, r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(lineWidth);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        wr.pos(x, y, 0).endVertex();
        wr.pos(x + w, y, 0).endVertex();
        wr.pos(x + w, y + h, 0).endVertex();
        wr.pos(x, y + h, 0).endVertex();
        wr.pos(x, y, 0).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    // ─── Circles & Arcs ─────────────────────────────────────────

    /** Draw a filled circle */
    public static void drawCircleFilled(double cx, double cy, double radius, int color) {
        float a = (color >> 24 & 255) / 255.0F, r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        wr.pos(cx, cy, 0).endVertex();
        int segments = 64;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
        }
        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /** Draw a circle outline */
    public static void drawCircleOutline(double cx, double cy, double radius, float lineWidth, int color) {
        float a = (color >> 24 & 255) / 255.0F, r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(lineWidth);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        int segments = 128;
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
        }
        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /** Draw a filled arc (pie slice) from startAngle to endAngle (degrees, 0=right, CW) */
    public static void drawArc(double cx, double cy, double radius, double startAngle, double endAngle, int color) {
        float a = (color >> 24 & 255) / 255.0F, r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        double startRad = Math.toRadians(startAngle);
        double endRad = Math.toRadians(endAngle);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        wr.pos(cx, cy, 0).endVertex();
        int segments = Math.max(4, (int)(Math.abs(endAngle - startAngle) / 2));
        for (int i = 0; i <= segments; i++) {
            double angle = startRad + (endRad - startRad) * i / segments;
            wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
        }
        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /** Draw a circle outline arc */
    public static void drawCirclePartial(double cx, double cy, double radius, float lineWidth, double startAngle, double endAngle, int color) {
        float a = (color >> 24 & 255) / 255.0F, r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(lineWidth);
        double startRad = Math.toRadians(startAngle);
        double endRad = Math.toRadians(endAngle);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        int segments = Math.max(8, (int)(Math.abs(endAngle - startAngle) / 2));
        for (int i = 0; i <= segments; i++) {
            double angle = startRad + (endRad - startRad) * i / segments;
            wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
        }
        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /** Draw a filled circle with gradient from center to edge */
    public static void drawCircleGradient(double cx, double cy, double radius, int colorCenter, int colorEdge) {
        float ac = (colorCenter >> 24 & 255) / 255.0F, rc = (colorCenter >> 16 & 255) / 255.0F, gc = (colorCenter >> 8 & 255) / 255.0F, bc = (colorCenter & 255) / 255.0F;
        float ae = (colorEdge >> 24 & 255) / 255.0F, re = (colorEdge >> 16 & 255) / 255.0F, ge = (colorEdge >> 8 & 255) / 255.0F, be = (colorEdge & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(cx, cy, 0).color(rc, gc, bc, ac).endVertex();
        int segments = 64;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).color(re, ge, be, ae).endVertex();
        }
        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /** Draw ring (donut shape) */
    public static void drawRing(double cx, double cy, double outerRadius, double innerRadius, int color) {
        float a = (color >> 24 & 255) / 255.0F, r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION);
        int segments = 128;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double cos = Math.cos(angle), sin = Math.sin(angle);
            wr.pos(cx + cos * outerRadius, cy + sin * outerRadius, 0).endVertex();
            wr.pos(cx + cos * innerRadius, cy + sin * innerRadius, 0).endVertex();
        }
        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /** Draw a partial ring (arc donut) */
    public static void drawRingPartial(double cx, double cy, double outerRadius, double innerRadius, double startDeg, double endDeg, float lineWidth, int color) {
        float a = (color >> 24 & 255) / 255.0F, r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(lineWidth);
        double midRadius = (outerRadius + innerRadius) / 2.0;
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        double startRad = Math.toRadians(startDeg);
        double endRad = Math.toRadians(endDeg);
        int segments = Math.max(8, (int)(Math.abs(endDeg - startDeg) / 2));
        for (int i = 0; i <= segments; i++) {
            double angle = startRad + (endRad - startRad) * i / segments;
            wr.pos(cx + Math.cos(angle) * midRadius, cy + Math.sin(angle) * midRadius, 0).endVertex();
        }
        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    // ─── Glow Effect ────────────────────────────────────────────

    /** Draw a soft glow circle (multi-layer transparency) */
    public static void drawGlow(double cx, double cy, double radius, int color, int layers) {
        for (int i = layers; i > 0; i--) {
            double r = radius * i / layers;
            int a = (color >> 24 & 255);
            int layerAlpha = (int)(a * 0.15f * (1.0f - (float)i / layers));
            int layerColor = getColorWithAlpha(color, layerAlpha);
            drawCircleFilled(cx, cy, r, layerColor);
        }
    }

    // ─── Lines ──────────────────────────────────────────────────

    public static void drawLine(Vec3 start, Vec3 end, int color, float lineWidth) {
        RenderManager rm = mc.getRenderManager();
        double x1 = start.xCoord - rm.viewerPosX, y1 = start.yCoord - rm.viewerPosY, z1 = start.zCoord - rm.viewerPosZ;
        double x2 = end.xCoord - rm.viewerPosX, y2 = end.yCoord - rm.viewerPosY, z2 = end.zCoord - rm.viewerPosZ;
        float a = (color >> 24 & 255) / 255.0F, r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GL11.glLineWidth(lineWidth);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        tess.draw();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawLine2D(double x1, double y1, double x2, double y2, int color, float lineWidth) {
        float a = (color >> 24 & 255) / 255.0F, r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(lineWidth);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x1, y1, 0).endVertex();
        wr.pos(x2, y2, 0).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    // ─── Entity Box ─────────────────────────────────────────────

    public static void drawEntityBox(Entity entity, int color, float lineWidth) {
        if (entity == null) return;
        float partialTicks = getRenderPartialTicks();
        RenderManager rm = mc.getRenderManager();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rm.viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rm.viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rm.viewerPosZ;
        AxisAlignedBB bb = entity.getEntityBoundingBox();
        double w = (bb.maxX - bb.minX) / 2.0, h = bb.maxY - bb.minY;
        float a = (color >> 24 & 255) / 255.0F, r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(lineWidth);
        double x1 = x - w, x2 = x + w, y1 = y, y2 = y + h, z1 = z - w, z2 = z + w;
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y1, z2).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z2).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x1, y2, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        wr.pos(x1, y2, z2).color(r, g, b, a).endVertex();
        wr.pos(x1, y2, z1).color(r, g, b, a).endVertex();
        tess.draw();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    // ─── Text ───────────────────────────────────────────────────

    public static void drawText(String text, double x, double y, int color, boolean shadow) {
        FontRenderer fr = mc.fontRendererObj;
        if (shadow) fr.drawString(text, (int)x + 1, (int)y + 1, (color & 0xFCFCFC) >> 2 | (color & 0xFF000000), false);
        fr.drawString(text, (int)x, (int)y, color, false);
    }

    public static void drawCenteredText(String text, double x, double y, int color, boolean shadow) {
        int w = mc.fontRendererObj.getStringWidth(text);
        drawText(text, x - w / 2.0, y, color, shadow);
    }

    // ─── Utility ────────────────────────────────────────────────

    private static Timer cachedTimer = null;

    private static float getRenderPartialTicks() {
        if (cachedTimer == null) {
            try {
                cachedTimer = ReflectionHelper.getPrivateValue(Minecraft.class, mc, "timer", "field_71428_T");
            } catch (Exception e) { return 1.0f; }
        }
        return cachedTimer.renderPartialTicks;
    }
}
