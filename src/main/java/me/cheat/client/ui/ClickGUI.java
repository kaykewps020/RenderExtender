package me.cheat.client.ui;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import me.cheat.client.modules.ModuleManager;
import me.cheat.client.ui.components.Frame;
import me.cheat.client.ui.components.ModuleButton;
import me.cheat.client.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends GuiScreen {
    private static final ClickGUI INSTANCE = new ClickGUI();
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final List<Frame> frames = new ArrayList<>();
    private boolean initialized = false;
    private long openTime = 0;
    private float animationProgress = 0;

    // CS:GO inspired purple palette
    public static final int BG_DARK = 0xCC0F0A1E;
    public static final int BG_MEDIUM = 0xCC1A1233;
    public static final int BG_LIGHT = 0xCC231544;
    public static final int PURPLE_PRIMARY = 0xFF7C3AED;
    public static final int PURPLE_SECONDARY = 0xFF6D28D9;
    public static final int PURPLE_TERTIARY = 0xFF4C1D95;
    public static final int PURPLE_ACCENT = 0xFFA78BFA;
    public static final int TEXT_MAIN = 0xFFE2E8F0;
    public static final int TEXT_DIM = 0xFF94A3B8;
    public static final int TEXT_BRIGHT = 0xFFFFFFFF;
    public static final int ENABLED_GREEN = 0xFF22C55E;
    public static final int DISABLED_RED = 0xFFEF4444;
    public static final int ORANGE_ACCENT = 0xFFF59E0B;

    public static ClickGUI getInstance() {
        return INSTANCE;
    }

    @Override
    public void initGui() {
        super.initGui();
        openTime = System.currentTimeMillis();
        animationProgress = 0;

        if (!initialized) {
            frames.clear();
            int x = 10;
            int y = 10;

            for (Category category : Category.values()) {
                List<Module> modules = ModuleManager.getModulesInCategory(category);
                if (!modules.isEmpty()) {
                    Frame frame = new Frame(category, x, y, 110, 18);
                    frames.add(frame);
                    x += 120;
                    if (x + 120 > width) {
                        x = 10;
                        y += 200;
                    }
                }
            }
            initialized = true;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Animation
        if (animationProgress < 1.0f) {
            animationProgress = Math.min(1.0f, animationProgress + partialTicks * 0.05f);
        }

        float alpha = animationProgress;
        float scale = 0.9f + animationProgress * 0.1f;

        // Dark overlay
        if (alpha > 0) {
            drawRect(0, 0, width, height, 
                RenderUtils.getColorWithAlpha(0x000000, (int)(180 * alpha)));
        }

        // Apply scale transform
        GL11.glPushMatrix();
        GL11.glTranslatef(width / 2.0f, height / 2.0f, 0);
        GL11.glScalef(scale, scale, 1.0f);
        GL11.glTranslatef(-width / 2.0f, -height / 2.0f, 0);

        // Header bar
        drawHeaderBar(alpha);

        // Frames
        for (Frame frame : frames) {
            frame.render(mouseX, mouseY, partialTicks, alpha);
        }

        // Watermark
        drawWatermark(alpha);

        GL11.glPopMatrix();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawHeaderBar(float alpha) {
        int barAlpha = (int)(alpha * 220);
        drawRect(0, 0, width, 3, RenderUtils.getColorWithAlpha(PURPLE_PRIMARY, barAlpha));
        
        RenderUtils.drawGradientRect(0, 3, width, 30,
            RenderUtils.getColorWithAlpha(BG_DARK, (int)(alpha * 200)),
            RenderUtils.getColorWithAlpha(0x00000000, 0));

        String title = "RENDER EXTENDER";
        float titleX = width / 2.0f - mc.fontRendererObj.getStringWidth(title) / 2.0f;
        
        RenderUtils.drawText(title, titleX + 1, 10, 
            RenderUtils.getColorWithAlpha(0xFF000000, (int)(alpha * 150)), false);
        RenderUtils.drawText(title, titleX, 9, 
            RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(alpha * 255)), false);

        String subtitle = "ADVANCED COMBAT ENHANCEMENTS";
        float subX = width / 2.0f - mc.fontRendererObj.getStringWidth(subtitle) / 2.0f;
        RenderUtils.drawText(subtitle, subX, 21,
            RenderUtils.getColorWithAlpha(TEXT_DIM, (int)(alpha * 180)), false);
    }

    private void drawWatermark(float alpha) {
        String watermark = "RE v2.0 // CS:GO EDITION";
        int w = mc.fontRendererObj.getStringWidth(watermark);
        
        RenderUtils.drawRoundedRect(width - w - 16, height - 22, w + 12, 16, 4,
            RenderUtils.getColorWithAlpha(BG_DARK, (int)(alpha * 200)));
        RenderUtils.drawOutlinedRect(width - w - 16, height - 22, w + 12, 16,
            RenderUtils.getColorWithAlpha(PURPLE_PRIMARY, (int)(alpha * 100)), 0.5f);
        RenderUtils.drawText(watermark, width - w - 10, height - 19,
            RenderUtils.getColorWithAlpha(TEXT_DIM, (int)(alpha * 150)), false);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (Frame frame : frames) {
            if (frame.mouseClicked(mouseX, mouseY, mouseButton)) return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for (Frame frame : frames) {
            frame.mouseReleased(mouseX, mouseY, state);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        for (Frame frame : frames) {
            frame.mouseDragged(mouseX, mouseY, clickedMouseButton);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for (Frame frame : frames) {
            if (frame.keyPressed(keyCode, typedChar)) return;
        }

        if (keyCode == 0x2D || keyCode == 0x01) { // X or ESC
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        initialized = false;
    }

    public void open() {
        animationProgress = 0;
        mc.displayGuiScreen(this);
    }

    public void toggle() {
        if (mc.currentScreen == this) {
            mc.displayGuiScreen(null);
        } else {
            open();
        }
    }

    public List<Frame> getFrames() { return frames; }
}
