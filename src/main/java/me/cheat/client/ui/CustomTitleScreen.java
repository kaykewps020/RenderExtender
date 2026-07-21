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
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.Random;

/**
 * CustomTitleScreen - Fully custom client title screen.
 * Purple grunge cross wallpaper, name changer, custom buttons.
 */
public class CustomTitleScreen extends GuiScreen {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // ─── State ──────────────────────────────────────
    private boolean showNameChanger = false;
    private GuiTextField nameField;
    private float animTime = 0;
    private float logoGlow = 0;
    private int particlesSeed = new Random().nextInt(10000);

    // ─── Colors ─────────────────────────────────────
    private static final int BG_PURPLE_DARK = 0xFF0A0514;
    private static final int BG_PURPLE_MID = 0xFF120828;
    private static final int PURPLE_PRIMARY = 0xFF7C3AED;
    private static final int PURPLE_LIGHT = 0xFFA78BFA;
    private static final int PURPLE_DARK = 0xFF4C1D95;
    private static final int TEXT_WHITE = 0xFFE2E8F0;
    private static final int TEXT_DIM = 0xFF94A3B8;
    private static final int ACCENT = 0xFF8B5CF6;

    // ─── Buttons ────────────────────────────────────
    private static final int BTN_SINGLE = 0;
    private static final int BTN_MULTI = 1;
    private static final int BTN_MODS = 2;
    private static final int BTN_NAME = 3;
    private static final int BTN_QUIT = 4;
    private static final int BTN_NAME_CONFIRM = 10;
    private static final int BTN_NAME_CANCEL = 11;

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
        int startY = h / 2 - 20;
        int gap = 32;

        buttonList.clear();
        buttonList.add(new GuiButton(BTN_SINGLE, btnX, startY, btnW, btnH, "Singleplayer"));
        buttonList.add(new GuiButton(BTN_MULTI, btnX, startY + gap, btnW, btnH, "Multiplayer"));
        buttonList.add(new GuiButton(BTN_MODS, btnX, startY + gap * 2, btnW, btnH, "Mods / Settings"));
        buttonList.add(new GuiButton(BTN_NAME, btnX, startY + gap * 3, btnW, btnH, "Name Changer"));
        buttonList.add(new GuiButton(BTN_QUIT, btnX, startY + gap * 4, btnW, btnH, "Quit"));

        // Name changer text field
        if (nameField == null) {
            nameField = new GuiTextField(0, mc.fontRendererObj, w / 2 - 100, h / 2 - 12, 200, 20);
            nameField.setMaxStringLength(16);
            nameField.setFocused(true);
            nameField.setText(mc.thePlayer != null ? mc.thePlayer.getName() : "");
        }

        if (!showNameChanger) {
            // Hide name changer buttons by default
        }
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
            case BTN_NAME:
                showNameChanger = !showNameChanger;
                if (showNameChanger) {
                    nameField.setText(mc.thePlayer != null ? mc.thePlayer.getName() : "Player");
                    nameField.setFocused(true);
                    // Add confirm/cancel buttons
                    int bw = 80;
                    int bx = mc.displayWidth / (new ScaledResolution(mc).getScaleFactor()) / 2;
                    // Use center of screen
                    ScaledResolution sr = new ScaledResolution(mc);
                    int cx = sr.getScaledWidth() / 2;
                    int cy = sr.getScaledHeight() / 2;
                    buttonList.add(new GuiButton(BTN_NAME_CONFIRM, cx - 104, cy + 16, 100, 22, "Confirm"));
                    buttonList.add(new GuiButton(BTN_NAME_CANCEL, cx + 4, cy + 16, 100, 22, "Cancel"));
                } else {
                    removeNameChangerButtons();
                }
                break;
            case BTN_NAME_CONFIRM:
                String newName = nameField.getText().trim();
                if (!newName.isEmpty() && newName.length() >= 2 && newName.length() <= 16) {
                    // Set via reflection since GameProfile name is final
                    try {
                        java.lang.reflect.Field profileField = mc.thePlayer.getClass().getSuperclass().getSuperclass().getDeclaredField("gameProfile");
                        profileField.setAccessible(true);
                        com.mojang.authlib.GameProfile oldProfile = (com.mojang.authlib.GameProfile) profileField.get(mc.thePlayer);
                        com.mojang.authlib.GameProfile newProfile = new com.mojang.authlib.GameProfile(oldProfile.getId(), newName);
                        profileField.set(mc.thePlayer, newProfile);
                    } catch (Exception e) {
                        // Fallback: just change the entity name via field
                        try {
                            java.lang.reflect.Field nameField2 = mc.thePlayer.getClass().getSuperclass().getDeclaredField("name");
                            nameField2.setAccessible(true);
                            nameField2.set(mc.thePlayer, newName);
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                }
                showNameChanger = false;
                removeNameChangerButtons();
                break;
            case BTN_NAME_CANCEL:
                showNameChanger = false;
                removeNameChangerButtons();
                break;
            case BTN_QUIT:
                mc.shutdown();
                break;
        }
    }

    private void removeNameChangerButtons() {
        buttonList.removeIf(b -> b.id == BTN_NAME_CONFIRM || b.id == BTN_NAME_CANCEL);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        animTime += partialTicks * 0.02f;

        ScaledResolution sr = new ScaledResolution(mc);
        int w = sr.getScaledWidth();
        int h = sr.getScaledHeight();

        // ─── BACKGROUND: Purple Grunge Cross ──────
        drawGrungeCrossBackground(w, h, partialTicks);

        // ─── CLIENT NAME / LOGO ───────────────────
        drawClientLogo(w, h, partialTicks);

        // ─── Default buttons ──────────────────────
        // Style buttons
        for (Object obj : buttonList) {
            GuiButton btn = (GuiButton) obj;
            if (btn.id == BTN_NAME_CONFIRM || btn.id == BTN_NAME_CANCEL) continue;
            styleButton(btn);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);

        // ─── Name changer overlay ─────────────────
        if (showNameChanger) {
            drawNameChanger(w, h, mouseX, mouseY);
        }

        // ─── Version text ─────────────────────────
        drawCenteredString(mc.fontRendererObj, EnumChatFormatting.DARK_GRAY + "RenderExtender v2.0", w / 2, h - 14, 0xFFFFFF);
        drawCenteredString(mc.fontRendererObj, EnumChatFormatting.GRAY + "Minecraft 1.8.9 Forge", w / 2, h - 4, 0xFFFFFF);
    }

    // ═══════════════════════════════════════════════════════
    //  PURPLE GRUNGE CROSS BACKGROUND
    // ═══════════════════════════════════════════════════════

    private void drawGrungeCrossBackground(int w, int h, float partialTicks) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // --- Dark gradient background ---
        drawGradientBg(w, h);

        // --- Grunge texture effect (noise particles) ---
        drawGrungeNoise(w, h);

        // --- The BIG purple cross ---
        drawPurpleCross(w, h, partialTicks);

        // --- Cross glow layers ---
        drawCrossGlow(w, h);

        // --- Corner accents ---
        drawCornerAccents(w, h);

        // --- Scanlines for grunge feel ---
        drawScanlines(w, h);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void drawGradientBg(int w, int h) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        // Dark purple gradient from top to bottom
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

    private void drawPurpleCross(int w, int h, float partialTicks) {
        float pulse = (float)(0.85 + 0.15 * Math.sin(animTime * 2.0));

        // Cross dimensions
        double crossArmW = w * 0.08;  // Width of each arm
        double crossArmL = h * 0.75;  // Length of vertical arm
        double crossArmH = w * 0.75;  // Length of horizontal arm

        double cx = w / 2.0;
        double cy = h / 2.0;

        // Glow layer 1 (large, dim)
        drawCrossShape(cx, cy, crossArmW * 1.8, crossArmL * 1.1, crossArmH * 1.1,
            0.15f * pulse, 0.05f * pulse, 0.35f * pulse, 0.12f);

        // Glow layer 2 (medium)
        drawCrossShape(cx, cy, crossArmW * 1.3, crossArmL * 1.02, crossArmH * 1.02,
            0.3f * pulse, 0.1f * pulse, 0.6f * pulse, 0.2f);

        // Main cross (solid)
        drawCrossShape(cx, cy, crossArmW, crossArmL, crossArmH,
            0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.85f);

        // Inner bright core
        drawCrossShape(cx, cy, crossArmW * 0.5, crossArmL * 0.9, crossArmH * 0.9,
            0.65f * pulse, 0.34f * pulse, 0.98f * pulse, 0.95f);

        // Cross outline
        drawCrossOutline(cx, cy, crossArmW, crossArmL, crossArmH, 0.35f, 0.15f, 0.7f, 0.7f);
    }

    private void drawCrossShape(double cx, double cy, double armW, double armV, double armH,
                                 float r, float g, float b, float a) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);

        double halfW = armW / 2.0;
        double halfV = armV / 2.0;
        double halfH = armH / 2.0;

        // Vertical arm
        wr.pos(cx - halfW, cy - halfV, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + halfW, cy - halfV, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + halfW, cy + halfV, 0).color(r, g, b, a).endVertex();
        wr.pos(cx - halfW, cy + halfV, 0).color(r, g, b, a).endVertex();

        // Horizontal arm
        wr.pos(cx - halfH, cy - halfW, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + halfH, cy - halfW, 0).color(r, g, b, a).endVertex();
        wr.pos(cx + halfH, cy + halfW, 0).color(r, g, b, a).endVertex();
        wr.pos(cx - halfH, cy + halfW, 0).color(r, g, b, a).endVertex();

        tess.draw();
    }

    private void drawCrossOutline(double cx, double cy, double armW, double armV, double armH,
                                   float r, float g, float b, float a) {
        GL11.glLineWidth(2.0f);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);

        double halfW = armW / 2.0;
        double halfV = armV / 2.0;
        double halfH = armH / 2.0;

        // Outline of the cross shape (go around the perimeter)
        // Bottom of vertical arm, left side
        wr.pos(cx - halfW, cy + halfV, 0).color(r, g, b, a).endVertex();
        // Left intersection
        wr.pos(cx - halfW, cy + halfW, 0).color(r, g, b, a).endVertex();
        // Left of horizontal arm, bottom
        wr.pos(cx - halfH, cy + halfW, 0).color(r, g, b, a).endVertex();
        // Left of horizontal arm, top
        wr.pos(cx - halfH, cy - halfW, 0).color(r, g, b, a).endVertex();
        // Left intersection top
        wr.pos(cx - halfW, cy - halfW, 0).color(r, g, b, a).endVertex();
        // Top of vertical arm, left
        wr.pos(cx - halfW, cy - halfV, 0).color(r, g, b, a).endVertex();
        // Top of vertical arm, right
        wr.pos(cx + halfW, cy - halfV, 0).color(r, g, b, a).endVertex();
        // Right intersection top
        wr.pos(cx + halfW, cy - halfW, 0).color(r, g, b, a).endVertex();
        // Right of horizontal arm, top
        wr.pos(cx + halfH, cy - halfW, 0).color(r, g, b, a).endVertex();
        // Right of horizontal arm, bottom
        wr.pos(cx + halfH, cy + halfW, 0).color(r, g, b, a).endVertex();
        // Right intersection bottom
        wr.pos(cx + halfW, cy + halfW, 0).color(r, g, b, a).endVertex();
        // Bottom of vertical arm, right
        wr.pos(cx + halfW, cy + halfV, 0).color(r, g, b, a).endVertex();

        tess.draw();
    }

    private void drawCrossGlow(int w, int h) {
        // Circular glow at cross center
        double cx = w / 2.0;
        double cy = h / 2.0;
        float pulse = (float)(0.8 + 0.2 * Math.sin(animTime * 3.0));

        for (int i = 5; i > 0; i--) {
            double radius = 30 + i * 20;
            float alpha = 0.03f * pulse * (1.0f - (float)i / 5.0f);
            drawFilledCircle(cx, cy, radius, 0.49f, 0.23f, 0.93f, alpha);
        }
    }

    private void drawCornerAccents(int w, int h) {
        float pulse = (float)(0.6 + 0.4 * Math.sin(animTime * 1.5));

        // Top-left corner line
        GL11.glLineWidth(1.5f);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        // Top-left
        wr.pos(20, 20, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        wr.pos(20, 60, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        wr.pos(20, 20, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        wr.pos(60, 20, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        // Top-right
        wr.pos(w - 20, 20, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        wr.pos(w - 20, 60, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        wr.pos(w - 20, 20, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        wr.pos(w - 60, 20, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        // Bottom-left
        wr.pos(20, h - 20, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        wr.pos(20, h - 60, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        wr.pos(20, h - 20, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        wr.pos(60, h - 20, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        // Bottom-right
        wr.pos(w - 20, h - 20, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        wr.pos(w - 20, h - 60, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        wr.pos(w - 20, h - 20, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        wr.pos(w - 60, h - 20, 0).color(0.49f * pulse, 0.23f * pulse, 0.93f * pulse, 0.5f).endVertex();
        tess.draw();
    }

    private void drawScanlines(int w, int h) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);

        for (int y = 0; y < h; y += 3) {
            float alpha = 0.02f + 0.01f * (float)Math.sin(y * 0.05 + animTime * 10);
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
        int seg = 48;
        for (int i = 0; i <= seg; i++) {
            double angle = 2 * Math.PI * i / seg;
            wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0)
               .color(r, g, b, 0).endVertex();
        }
        tess.draw();
    }

    // ═══════════════════════════════════════════════════════
    //  CLIENT LOGO
    // ═══════════════════════════════════════════════════════

    private void drawClientLogo(int w, int h, float partialTicks) {
        logoGlow = (float)(0.8 + 0.2 * Math.sin(animTime * 4.0));

        // Main title
        String title = "RENDEXTENDER";
        int titleW = mc.fontRendererObj.getStringWidth(title);

        // Glow behind text
        float ga = 0.15f * logoGlow;
        drawFilledCircle(w / 2.0, h / 2.0 - 80, 80, 0.49f, 0.23f, 0.93f, ga);

        // Title text with shadow
        mc.fontRendererObj.drawStringWithShadow(title, w / 2.0f - titleW / 2.0f, h / 2.0f - 88,
            new Color(167, 139, 250).getRGB());

        // Subtitle
        String sub = "v2.0 | Forge 1.8.9";
        int subW = mc.fontRendererObj.getStringWidth(sub);
        mc.fontRendererObj.drawStringWithShadow(sub, w / 2.0f - subW / 2.0f, h / 2.0f - 74,
            new Color(100, 80, 140).getRGB());

        // Decorative line under title
        GL11.glLineWidth(1.5f);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        double lineY = h / 2.0 - 68;
        double lineHalfW = 60;
        wr.pos(w / 2.0 - lineHalfW, lineY, 0).color(0.49f, 0.23f, 0.93f, 0.6f * logoGlow).endVertex();
        wr.pos(w / 2.0 + lineHalfW, lineY, 0).color(0.49f, 0.23f, 0.93f, 0.6f * logoGlow).endVertex();
        tess.draw();
    }

    // ═══════════════════════════════════════════════════════
    //  NAME CHANGER
    // ═══════════════════════════════════════════════════════

    private void drawNameChanger(int w, int h, int mouseX, int mouseY) {
        // Semi-transparent panel
        int panelW = 260;
        int panelH = 80;
        int panelX = w / 2 - panelW / 2;
        int panelY = h / 2 - panelH / 2;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();

        // Panel background
        RenderUtils.drawRoundedRect(panelX, panelY, panelW, panelH, 8, 0xCC0F0A1E);
        RenderUtils.drawCircleOutline(w / 2.0, panelY + panelH / 2.0, panelW / 2.0 + 4, 1.5f, 0xFF7C3AED);

        GlStateManager.enableTexture2D();

        // Title
        String title = "NAME CHANGER";
        int titleW = mc.fontRendererObj.getStringWidth(title);
        mc.fontRendererObj.drawStringWithShadow(title, w / 2.0f - titleW / 2.0f, panelY + 6, 0xFFA78BFA);

        // Text field
        nameField.drawTextBox();

        // Style confirm/cancel buttons
        for (Object obj : buttonList) {
            GuiButton btn = (GuiButton) obj;
            if (btn.id == BTN_NAME_CONFIRM) {
                btn.packedFGColour = 0xFF22C55E;
            } else if (btn.id == BTN_NAME_CANCEL) {
                btn.packedFGColour = 0xFFEF4444;
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  BUTTON STYLING
    // ═══════════════════════════════════════════════════════

    private void styleButton(GuiButton btn) {
        if (btn.id == BTN_NAME_CONFIRM || btn.id == BTN_NAME_CANCEL) return;

        // Purple-tinted button
        btn.packedFGColour = 0xFFA78BFA;
    }

    // ═══════════════════════════════════════════════════════
    //  INPUT
    // ═══════════════════════════════════════════════════════

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (showNameChanger && nameField.isFocused()) {
            nameField.textboxKeyTyped(typedChar, keyCode);
            if (keyCode == Keyboard.KEY_RETURN) {
                // Trigger confirm
                actionPerformed(new GuiButton(BTN_NAME_CONFIRM, 0, 0, 0, 0, ""));
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (showNameChanger) {
            nameField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (showNameChanger && nameField != null) {
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
