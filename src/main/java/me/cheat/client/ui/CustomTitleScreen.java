package me.cheat.client.ui;

import me.cheat.client.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.Random;

/**
 * CustomTitleScreen - Fully custom client title screen.
 * Purple grunge cross wallpaper, name changer integrated on the page.
 */
public class CustomTitleScreen extends GuiScreen {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // ─── State ──────────────────────────────────────
    private GuiTextField nameField;
    private float animTime = 0;
    private float logoGlow = 0;
    private int particlesSeed = new Random().nextInt(10000);
    private String statusMessage = "";
    private int statusTicks = 0;

    // ─── Colors ─────────────────────────────────────
    private static final int PURPLE_PRIMARY = 0xFF7C3AED;
    private static final int PURPLE_LIGHT = 0xFFA78BFA;

    // ─── Buttons ────────────────────────────────────
    private static final int BTN_SINGLE = 0;
    private static final int BTN_MULTI = 1;
    private static final int BTN_MODS = 2;
    private static final int BTN_QUIT = 3;
    private static final int BTN_NAME_SET = 5;

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        ScaledResolution sr = new ScaledResolution(mc);
        int w = sr.getScaledWidth();
        int h = sr.getScaledHeight();

        int btnW = 180;
        int btnH = 26;
        int btnX = w / 2 - btnW / 2;
        int startY = h / 2 + 10;
        int gap = 32;

        buttonList.clear();
        buttonList.add(new GuiButton(BTN_SINGLE, btnX, startY, btnW, btnH, "\u25B6 Singleplayer"));
        buttonList.add(new GuiButton(BTN_MULTI, btnX, startY + gap, btnW, btnH, "\u25B6 Multiplayer"));
        buttonList.add(new GuiButton(BTN_MODS, btnX, startY + gap * 2, btnW, btnH, "\u2699 Mods / Settings"));
        buttonList.add(new GuiButton(BTN_QUIT, btnX, startY + gap * 3, btnW, btnH, "\u2716 Quit"));

        // ─── Name Changer: ALWAYS visible on the page ────
        // Position: above the buttons, below the logo
        int fieldW = 160;
        int fieldH = 18;
        int nameAreaY = h / 2 - 30;

        nameField = new GuiTextField(99, mc.fontRendererObj, w / 2 - fieldW / 2 - 30, nameAreaY, fieldW, fieldH);
        nameField.setMaxStringLength(16);
        nameField.setFocused(false);
        nameField.setText(mc.thePlayer != null ? mc.thePlayer.getName() : "Player");
        nameField.setEnableBackgroundDrawing(false);

        // SET button next to the text field
        buttonList.add(new GuiButton(BTN_NAME_SET, w / 2 + fieldW / 2 - 26, nameAreaY - 1, 56, fieldH + 2, "SET"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_SINGLE:
                mc.displayGuiScreen(new net.minecraft.client.gui.GuiSelectWorld(this));
                break;
            case BTN_MULTI:
                mc.displayGuiScreen(new net.minecraft.client.gui.GuiMultiplayer(this));
                break;
            case BTN_MODS:
                ClickGUI.getInstance().toggle();
                break;
            case BTN_NAME_SET:
                applyNameChange();
                break;
            case BTN_QUIT:
                mc.shutdown();
                break;
        }
    }

    private void applyNameChange() {
        String newName = nameField.getText().trim();
        if (newName.isEmpty() || newName.length() < 2 || newName.length() > 16) {
            statusMessage = "\u00A7cName must be 2-16 characters!";
            statusTicks = 120;
            return;
        }

        try {
            // Change GameProfile name via reflection
            Object player = mc.thePlayer;
            // EntityPlayerSP -> AbstractClientPlayer -> AbstractClientPlayer -> EntityPlayer
            // gameProfile is in AbstractClientPlayer
            java.lang.reflect.Field profileField = null;
            Class<?> clazz = player.getClass();
            while (clazz != null) {
                try {
                    profileField = clazz.getDeclaredField("gameProfile");
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }

            if (profileField != null) {
                profileField.setAccessible(true);
                com.mojang.authlib.GameProfile oldProfile = (com.mojang.authlib.GameProfile) profileField.get(player);
                com.mojang.authlib.GameProfile newProfile = new com.mojang.authlib.GameProfile(oldProfile.getId(), newName);
                profileField.set(player, newProfile);
                statusMessage = "\u00A7aName set to: \u00A7d" + newName;
                statusTicks = 120;
            } else {
                statusMessage = "\u00A7cFailed to change name!";
                statusTicks = 120;
            }
        } catch (Exception e) {
            statusMessage = "\u00A7cError: " + e.getMessage();
            statusTicks = 120;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        animTime += partialTicks * 0.02f;
        if (statusTicks > 0) statusTicks--;

        ScaledResolution sr = new ScaledResolution(mc);
        int w = sr.getScaledWidth();
        int h = sr.getScaledHeight();

        // ─── BACKGROUND: Purple Grunge Cross ──────
        drawGrungeCrossBackground(w, h, partialTicks);

        // ─── CLIENT NAME / LOGO ───────────────────
        drawClientLogo(w, h);

        // ─── NAME CHANGER SECTION (always on page) ─
        drawNameChangerSection(w, h);

        // ─── Status message ───────────────────────
        if (statusTicks > 0) {
            float alpha = Math.min(1.0f, statusTicks / 20.0f);
            mc.fontRendererObj.drawStringWithShadow(statusMessage, w / 2.0f - mc.fontRendererObj.getStringWidth(statusMessage) / 2.0f,
                h / 2.0f - 48, (int)(0xFFFFFFFF * alpha));
        }

        // ─── Buttons ──────────────────────────────
        for (Object obj : buttonList) {
            styleButton((GuiButton) obj);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);

        // ─── Version footer ───────────────────────
        drawCenteredString(mc.fontRendererObj, EnumChatFormatting.DARK_GRAY + "RenderExtender v2.0", w / 2, h - 14, 0xFFFFFF);
        drawCenteredString(mc.fontRendererObj, EnumChatFormatting.GRAY + "Minecraft 1.8.9 Forge", w / 2, h - 4, 0xFFFFFF);
    }

    // ═══════════════════════════════════════════════════════
    //  NAME CHANGER SECTION - Always visible on the page
    // ═══════════════════════════════════════════════════════

    private void drawNameChangerSection(int w, int h) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();

        int fieldW = 160;
        int fieldH = 18;
        int nameAreaY = h / 2 - 30;
        int fieldX = w / 2 - fieldW / 2 - 30;

        // ─── Panel background ─────────────────────
        int panelPadX = 16;
        int panelPadY = 14;
        int panelX = fieldX - panelPadX;
        int panelY = nameAreaY - panelPadY;
        int panelW = fieldW + 56 + panelPadX * 2 + 8;
        int panelH = fieldH + panelPadY * 2 + 6;

        RenderUtils.drawRoundedRect(panelX, panelY, panelW, panelH, 8,
            RenderUtils.getColorWithAlpha(0x0A0514, 210));
        RenderUtils.drawOutlinedRect(panelX, panelY, panelW, panelH,
            RenderUtils.getColorWithAlpha(0x7C3AED, 120), 1.0f);

        GlStateManager.enableTexture2D();

        // ─── Label: "NAME:" ───────────────────────
        String label = "NAME:";
        mc.fontRendererObj.drawStringWithShadow(label, fieldX - mc.fontRendererObj.getStringWidth(label) - 6,
            nameAreaY + 3, new Color(167, 139, 250).getRGB());

        // ─── Decorative line under label ───────────
        GlStateManager.disableTexture2D();
        GL11.glLineWidth(1.0f);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        double lineY = nameAreaY + fieldH + 4;
        wr.pos(fieldX - mc.fontRendererObj.getStringWidth(label) - 6, lineY, 0).color(0.49f, 0.23f, 0.93f, 0.4f).endVertex();
        wr.pos(fieldX + fieldW, lineY, 0).color(0.49f, 0.23f, 0.93f, 0.4f).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();

        // ─── Text field (custom rendered) ──────────
        // Field background
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        RenderUtils.drawRoundedRect(fieldX, nameAreaY - 1, fieldW, fieldH + 2, 4,
            RenderUtils.getColorWithAlpha(0x1A1233, 200));
        RenderUtils.drawOutlinedRect(fieldX, nameAreaY - 1, fieldW, fieldH + 2,
            RenderUtils.getColorWithAlpha(nameField.isFocused() ? 0xA78BFA : 0x4C1D95, nameField.isFocused() ? 200 : 120), 1.0f);
        GlStateManager.enableTexture2D();

        nameField.drawTextBox();
    }

    // ═══════════════════════════════════════════════════════
    //  PURPLE GRUNGE CROSS BACKGROUND
    // ═══════════════════════════════════════════════════════

    private void drawGrungeCrossBackground(int w, int h, float partialTicks) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        drawGradientBg(w, h);
        drawGrungeNoise(w, h);
        drawPurpleCross(w, h);
        drawCrossGlow(w, h);
        drawCornerAccents(w, h);
        drawScanlines(w, h);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void drawGradientBg(int w, int h) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(0, 0, 0).color(0.04f, 0.02f, 0.08f, 1.0f).endVertex();
        wr.pos(w, 0, 0).color(0.06f, 0.03f, 0.12f, 1.0f).endVertex();
        wr.pos(w, h, 0).color(0.02f, 0.01f, 0.04f, 1.0f).endVertex();
        wr.pos(0, h, 0).color(0.03f, 0.01f, 0.06f, 1.0f).endVertex();
        tess.draw();
    }

    private void drawGrungeNoise(int w, int h) {
        Random rng = new Random(particlesSeed);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 200; i++) {
            double px = rng.nextDouble() * w;
            double py = rng.nextDouble() * h;
            double size = 1 + rng.nextDouble() * 3;
            float alpha = 0.02f + rng.nextFloat() * 0.06f;
            float purple = 0.3f + rng.nextFloat() * 0.4f;
            wr.pos(px, py, 0).color(purple * 0.3f, purple * 0.1f, purple, alpha).endVertex();
            wr.pos(px + size, py, 0).color(purple * 0.3f, purple * 0.1f, purple, alpha).endVertex();
            wr.pos(px + size, py + size, 0).color(purple * 0.3f, purple * 0.1f, purple, alpha).endVertex();
            wr.pos(px, py + size, 0).color(purple * 0.3f, purple * 0.1f, purple, alpha).endVertex();
        }
        tess.draw();
    }

    private void drawPurpleCross(int w, int h) {
        float pulse = (float)(0.85 + 0.15 * Math.sin(animTime * 2.0));
        double armW = w * 0.08;
        double armV = h * 0.75;
        double armH = w * 0.75;
        double cx = w / 2.0;
        double cy = h / 2.0;

        drawCrossShape(cx, cy, armW * 1.8, armV * 1.1, armH * 1.1, 0.15f * pulse, 0.05f * pulse, 0.35f * pulse, 0.12f);
        drawCrossShape(cx, cy, armW * 1.3, armV * 1.02, armH * 1.02, 0.3f * pulse, 0.1f * pulse, 0.6f * pulse, 0.2f);
        drawCrossShape(cx, cy, armW, armV, armH, 0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.85f);
        drawCrossShape(cx, cy, armW * 0.5, armV * 0.9, armH * 0.9, 0.65f * pulse, 0.34f * pulse, 0.98f * pulse, 0.95f);
        drawCrossOutline(cx, cy, armW, armV, armH, 0.35f, 0.15f, 0.7f, 0.7f);
    }

    private void drawCrossShape(double cx, double cy, double armW, double armV, double armH, float r, float g, float b, float a) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        double hw = armW / 2, hv = armV / 2, hh = armH / 2;
        wr.pos(cx - hw, cy - hv, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + hw, cy - hv, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + hw, cy + hv, 0).color(r, g, b, a).endVertex();
        wr.pos(cx - hw, cy + hv, 0).color(r, g, b, a).endVertex();
        wr.pos(cx - hh, cy - hw, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + hh, cy - hw, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + hh, cy + hw, 0).color(r, g, b, a).endVertex();
        wr.pos(cx - hh, cy + hw, 0).color(r, g, b, a).endVertex();
        tess.draw();
    }

    private void drawCrossOutline(double cx, double cy, double armW, double armV, double armH, float r, float g, float b, float a) {
        GL11.glLineWidth(2.0f);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        double hw = armW / 2, hv = armV / 2, hh = armH / 2;
        wr.pos(cx - hw, cy + hv, 0).color(r, g, b, a).endVertex();
        wr.pos(cx - hw, cy + hw, 0).color(r, g, b, a).endVertex();
        wr.pos(cx - hh, cy + hw, 0).color(r, g, b, a).endVertex();
        wr.pos(cx - hh, cy - hw, 0).color(r, g, b, a).endVertex();
        wr.pos(cx - hw, cy - hw, 0).color(r, g, b, a).endVertex();
        wr.pos(cx - hw, cy - hv, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + hw, cy - hv, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + hw, cy - hw, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + hh, cy - hw, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + hh, cy + hw, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + hw, cy + hw, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + hw, cy + hv, 0).color(r, g, b, a).endVertex();
        tess.draw();
    }

    private void drawCrossGlow(int w, int h) {
        double cx = w / 2.0, cy = h / 2.0;
        float pulse = (float)(0.8 + 0.2 * Math.sin(animTime * 3.0));
        for (int i = 5; i > 0; i--) {
            double radius = 30 + i * 20;
            float alpha = 0.03f * pulse * (1.0f - (float)i / 5.0f);
            drawFilledCircle(cx, cy, radius, 0.49f, 0.23f, 0.93f, alpha);
        }
    }

    private void drawCornerAccents(int w, int h) {
        float pulse = (float)(0.6 + 0.4 * Math.sin(animTime * 1.5));
        GL11.glLineWidth(1.5f);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        float r = 0.49f * pulse, g = 0.23f * pulse, b = 0.93f * pulse;
        wr.pos(20, 20, 0).color(r, g, b, 0.5f).endVertex(); wr.pos(20, 60, 0).color(r, g, b, 0.5f).endVertex();
        wr.pos(20, 20, 0).color(r, g, b, 0.5f).endVertex(); wr.pos(60, 20, 0).color(r, g, b, 0.5f).endVertex();
        wr.pos(w - 20, 20, 0).color(r, g, b, 0.5f).endVertex(); wr.pos(w - 20, 60, 0).color(r, g, b, 0.5f).endVertex();
        wr.pos(w - 20, 20, 0).color(r, g, b, 0.5f).endVertex(); wr.pos(w - 60, 20, 0).color(r, g, b, 0.5f).endVertex();
        wr.pos(20, h - 20, 0).color(r, g, b, 0.5f).endVertex(); wr.pos(20, h - 60, 0).color(r, g, b, 0.5f).endVertex();
        wr.pos(20, h - 20, 0).color(r, g, b, 0.5f).endVertex(); wr.pos(60, h - 20, 0).color(r, g, b, 0.5f).endVertex();
        wr.pos(w - 20, h - 20, 0).color(r, g, b, 0.5f).endVertex(); wr.pos(w - 20, h - 60, 0).color(r, g, b, 0.5f).endVertex();
        wr.pos(w - 20, h - 20, 0).color(r, g, b, 0.5f).endVertex(); wr.pos(w - 60, h - 20, 0).color(r, g, b, 0.5f).endVertex();
        tess.draw();
    }

    private void drawScanlines(int w, int h) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int y = 0; y < h; y += 3) {
            float alpha = 0.02f + 0.01f * (float) Math.sin(y * 0.05 + animTime * 10);
            wr.pos(0, y, 0).color(0.1f, 0.05f, 0.2f, alpha).endVertex();
            wr.pos(w, y, 0).color(0.1f, 0.05f, 0.2f, alpha).endVertex();
            wr.pos(w, y + 1, 0).color(0.1f, 0.05f, 0.2f, alpha).endVertex();
            wr.pos(0, y + 1, 0).color(0.1f, 0.05f, 0.2f, alpha).endVertex();
        }
        tess.draw();
    }

    private void drawFilledCircle(double cx, double cy, double radius, float r, float g, float b, float a) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(cx, cy, 0).color(r, g, b, a).endVertex();
        for (int i = 0; i <= 48; i++) {
            double angle = 2 * Math.PI * i / 48;
            wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).color(r, g, b, 0).endVertex();
        }
        tess.draw();
    }

    // ═══════════════════════════════════════════════════════
    //  CLIENT LOGO
    // ═══════════════════════════════════════════════════════

    private void drawClientLogo(int w, int h) {
        logoGlow = (float)(0.8 + 0.2 * Math.sin(animTime * 4.0));
        String title = "RENDEXTENDER";
        int titleW = mc.fontRendererObj.getStringWidth(title);

        float ga = 0.15f * logoGlow;
        drawFilledCircle(w / 2.0, h / 2.0 - 80, 80, 0.49f, 0.23f, 0.93f, ga);

        mc.fontRendererObj.drawStringWithShadow(title, w / 2.0f - titleW / 2.0f, h / 2.0f - 88,
            new Color(167, 139, 250).getRGB());

        String sub = "v2.0 | Forge 1.8.9";
        int subW = mc.fontRendererObj.getStringWidth(sub);
        mc.fontRendererObj.drawStringWithShadow(sub, w / 2.0f - subW / 2.0f, h / 2.0f - 74,
            new Color(100, 80, 140).getRGB());

        // Decorative line
        GlStateManager.disableTexture2D();
        GL11.glLineWidth(1.5f);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        double lineY = h / 2.0 - 68;
        wr.pos(w / 2.0 - 60, lineY, 0).color(0.49f, 0.23f, 0.93f, 0.6f * logoGlow).endVertex();
        wr.pos(w / 2.0 + 60, lineY, 0).color(0.49f, 0.23f, 0.93f, 0.6f * logoGlow).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();
    }

    // ═══════════════════════════════════════════════════════
    //  BUTTON STYLING
    // ═══════════════════════════════════════════════════════

    private void styleButton(GuiButton btn) {
        if (btn.id == BTN_NAME_SET) {
            btn.packedFGColour = 0xFF22C55E;
            return;
        }
        btn.packedFGColour = 0xFFA78BFA;
    }

    // ═══════════════════════════════════════════════════════
    //  INPUT HANDLING
    // ═══════════════════════════════════════════════════════

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (nameField != null && nameField.isFocused()) {
            nameField.textboxKeyTyped(typedChar, keyCode);
            if (keyCode == Keyboard.KEY_RETURN) {
                applyNameChange();
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (nameField != null) {
            nameField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (nameField != null) {
            nameField.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
