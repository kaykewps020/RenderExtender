package me.cheat.client.ui;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import me.cheat.client.modules.ModuleManager;
import me.cheat.client.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RadialClickGUI - CS:GO-style circular/radial menu with vector icons.
 *
 * Layout:
 *   - Center hub: client name + player head
 *   - Category ring: each category is a segment on the ring
 *   - Module nodes: when a category is selected, modules appear around it
 *   - All drawn with GL11 circles, arcs, gradients
 */
public class ClickGUI extends GuiScreen {
    private static final ClickGUI INSTANCE = new ClickGUI();
    private static final Minecraft mc = Minecraft.getMinecraft();

    // ─── Colors ─────────────────────────────────────────────
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

    // ─── Layout Constants ───────────────────────────────────
    private static final double CENTER_RADIUS = 24;
    private static final double CATEGORY_RING_RADIUS = 90;
    private static final double CATEGORY_SEGMENT_SIZE = 60; // degrees per category
    private static final double MODULE_RING_RADIUS = 150;
    private static final double MODULE_NODE_RADIUS = 6;

    // ─── State ──────────────────────────────────────────────
    private boolean initialized = false;
    private float openAnim = 0;
    private float closeAnim = 0;
    private boolean closing = false;

    private int selectedCategory = -1;
    private float categorySelectAnim = 0;
    private int hoveredCategory = -1;
    private int hoveredModule = -1;

    private int centerX, centerY;
    private long openTime;

    // Category data
    private final List<CategoryData> categories = new ArrayList<>();

    private static class CategoryData {
        Category category;
        double angle; // center angle in degrees
        float hoverAnim = 0;
        float selectAnim = 0;
        List<ModuleData> modules = new ArrayList<>();
    }

    private static class ModuleData {
        Module module;
        double angle;
        float hoverAnim = 0;
        float toggleAnim;
        boolean expanded = false;
        List<Module.Setting> settings = new ArrayList<>();
        int expandedHeight = 0;

        ModuleData(Module module, double angle) {
            this.module = module;
            this.angle = angle;
            this.toggleAnim = module.isEnabled() ? 1.0f : 0.0f;
        }
    }

    public static ClickGUI getInstance() { return INSTANCE; }

    @Override
    public void initGui() {
        super.initGui();
        ScaledResolution sr = new ScaledResolution(mc);
        centerX = sr.getScaledWidth() / 2;
        centerY = sr.getScaledHeight() / 2;
        openTime = System.currentTimeMillis();

        if (!initialized) {
            categories.clear();
            Category[] cats = Category.values();
            double startAngle = -90; // top
            double segSize = 360.0 / cats.length;

            for (int i = 0; i < cats.length; i++) {
                CategoryData cd = new CategoryData();
                cd.category = cats[i];
                cd.angle = startAngle + segSize * i;

                List<Module> modules = ModuleManager.getModulesInCategory(cats[i]);
                double modStart = cd.angle - segSize / 2 + 10;
                double modSeg = (segSize - 20) / Math.max(1, modules.size());
                for (int j = 0; j < modules.size(); j++) {
                    ModuleData md = new ModuleData(modules.get(j), modStart + modSeg * j + modSeg / 2);
                    for (Module.Setting s : modules.get(j).getSettings()) {
                        md.settings.add(s);
                    }
                    cd.modules.add(md);
                }
                categories.add(cd);
            }
            initialized = true;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Animation
        if (!closing) {
            openAnim = Math.min(1.0f, openAnim + partialTicks * 0.08f);
        } else {
            openAnim = Math.max(0, openAnim - partialTicks * 0.12f);
            if (openAnim <= 0) {
                mc.displayGuiScreen(null);
                closing = false;
                return;
            }
        }

        float anim = openAnim;
        if (anim <= 0) return;

        ScaledResolution sr = new ScaledResolution(mc);
        centerX = sr.getScaledWidth() / 2;
        centerY = sr.getScaledHeight() / 2;

        // Dark overlay
        drawRect(0, 0, width, height, RenderUtils.getColorWithAlpha(0x000000, (int)(200 * anim)));

        // Enable GL state
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();

        // ─── Background rings ────────────────────────────────
        double scale = 0.6f + anim * 0.4f;
        double ringR = CATEGORY_RING_RADIUS * scale;

        // Outer ambient glow
        RenderUtils.drawCircleGradient(centerX, centerY, ringR + 30,
            RenderUtils.getColorWithAlpha(0x000000, 0),
            RenderUtils.getColorWithAlpha(PURPLE_TERTIARY, (int)(40 * anim)));

        // Background ring
        RenderUtils.drawCircleFilled(centerX, centerY, ringR + 4,
            RenderUtils.getColorWithAlpha(0x0A0514, (int)(180 * anim)));

        // ─── Category segments ──────────────────────────────
        hoveredCategory = -1;
        double segSize = 360.0 / categories.size();

        for (int i = 0; i < categories.size(); i++) {
            CategoryData cd = categories.get(i);
            double startA = cd.angle - segSize / 2 + 1;
            double endA = cd.angle + segSize / 2 - 1;

            // Hover detection
            boolean segHovered = isMouseInSector(mouseX, mouseY, centerX, centerY, ringR - 8, ringR + 8, startA, endA);
            if (segHovered) hoveredCategory = i;

            // Animations
            cd.hoverAnim = lerpAnim(cd.hoverAnim, segHovered ? 1 : 0, 0.12f);
            cd.selectAnim = lerpAnim(cd.selectAnim, (selectedCategory == i) ? 1 : 0, 0.1f);

            // Draw segment
            int segColor = RenderUtils.lerpColor(
                RenderUtils.getColorWithAlpha(PURPLE_TERTIARY, (int)(120 * anim)),
                RenderUtils.getColorWithAlpha(PURPLE_PRIMARY, (int)(255 * anim)),
                cd.hoverAnim
            );
            segColor = RenderUtils.lerpColor(segColor,
                RenderUtils.getColorWithAlpha(PURPLE_SECONDARY, (int)(255 * anim)),
                cd.selectAnim);

            RenderUtils.drawArc(centerX, centerY, ringR, startA, endA, segColor);

            // Segment border
            RenderUtils.drawCirclePartial(centerX, centerY, ringR, 1.5f, startA, endA,
                RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(180 * anim * cd.hoverAnim + 80 * anim)));

            // Category icon at midpoint
            double iconAngle = Math.toRadians(cd.angle);
            double iconDist = ringR;
            double iconX = centerX + Math.cos(iconAngle) * iconDist;
            double iconY = centerY + Math.sin(iconAngle) * iconDist;

            drawCategoryIcon(cd.category, iconX, iconY, anim, cd);

            // Category name
            double labelDist = ringR + 22;
            double labelX = centerX + Math.cos(iconAngle) * labelDist;
            double labelY = centerY + Math.sin(iconAngle) * labelDist;
            String name = cd.category.getName().toUpperCase();
            int nameW = mc.fontRendererObj.getStringWidth(name);
            int nameColor = RenderUtils.lerpColor(
                RenderUtils.getColorWithAlpha(TEXT_DIM, (int)(200 * anim)),
                RenderUtils.getColorWithAlpha(TEXT_BRIGHT, (int)(255 * anim)),
                cd.hoverAnim
            );
            RenderUtils.drawCenteredText(name, labelX, labelY - 4, nameColor, true);

            // Module count
            String count = cd.modules.size() + "";
            RenderUtils.drawCenteredText(count, labelX, labelY + 7,
                RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(150 * anim)), false);
        }

        // ─── Center Hub ─────────────────────────────────────
        RenderUtils.drawCircleFilled(centerX, centerY, CENTER_RADIUS + 2,
            RenderUtils.getColorWithAlpha(0x0A0514, (int)(200 * anim)));
        RenderUtils.drawCircleOutline(centerX, centerY, CENTER_RADIUS + 2, 2.0f,
            RenderUtils.getColorWithAlpha(PURPLE_PRIMARY, (int)(220 * anim)));

        // Center text
        RenderUtils.drawCenteredText("RE", centerX, centerY - 8,
            RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(255 * anim)), true);
        RenderUtils.drawCenteredText("v2.0", centerX, centerY + 3,
            RenderUtils.getColorWithAlpha(TEXT_DIM, (int)(180 * anim)), false);

        // ─── Selected category modules ──────────────────────
        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            categorySelectAnim = Math.min(1.0f, categorySelectAnim + partialTicks * 0.1f);
            drawModules(categories.get(selectedCategory), mouseX, mouseY, partialTicks, anim);
        } else {
            categorySelectAnim = Math.max(0, categorySelectAnim - partialTicks * 0.12f);
        }

        // ─── Watermark ──────────────────────────────────────
        GlStateManager.enableTexture2D();
        String wm = "RE v2.0";
        int wmW = mc.fontRendererObj.getStringWidth(wm);
        RenderUtils.drawRoundedRect(width - wmW - 16, height - 22, wmW + 12, 16, 8,
            RenderUtils.getColorWithAlpha(BG_DARK, (int)(200 * anim)));
        RenderUtils.drawCircleOutline(width - wmW - 8, height - 14, 8, 0.5f,
            RenderUtils.getColorWithAlpha(PURPLE_PRIMARY, (int)(100 * anim)));
        RenderUtils.drawText(wm, width - wmW - 10, height - 19,
            RenderUtils.getColorWithAlpha(TEXT_DIM, (int)(150 * anim)), false);

        GlStateManager.disableBlend();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawModules(CategoryData cat, int mouseX, int mouseY, float partialTicks, float anim) {
        double modAngleSpread = 80; // degrees spread for modules
        double modStartAngle = -90 - modAngleSpread / 2;
        double modStep = modAngleSpread / Math.max(1, cat.modules.size() - 1);
        if (cat.modules.size() == 1) modStep = 0;

        hoveredModule = -1;

        for (int i = 0; i < cat.modules.size(); i++) {
            ModuleData md = cat.modules.get(i);
            double ma = modStartAngle + modStep * i;
            if (cat.modules.size() == 1) ma = -90;
            md.angle = ma;

            double modAngleRad = Math.toRadians(ma);
            double modDist = MODULE_RING_RADIUS * categorySelectAnim;
            double modX = centerX + Math.cos(modAngleRad) * modDist;
            double modY = centerY + Math.sin(modAngleRad) * modDist;

            // Hover
            double distToMouse = Math.sqrt(Math.pow(mouseX - modX, 2) + Math.pow(mouseY - modY, 2));
            boolean hovered = distToMouse < MODULE_NODE_RADIUS + 6;
            if (hovered) hoveredModule = i;
            md.hoverAnim = lerpAnim(md.hoverAnim, hovered ? 1 : 0, 0.15f);

            // Toggle animation
            md.toggleAnim = lerpAnim(md.toggleAnim, md.module.isEnabled() ? 1 : 0, 0.1f);

            // Glow for enabled modules
            if (md.module.isEnabled()) {
                RenderUtils.drawGlow(modX, modY, MODULE_NODE_RADIUS + 8, PURPLE_PRIMARY, 4);
            }

            // Node background
            int nodeColor = RenderUtils.lerpColor(
                RenderUtils.getColorWithAlpha(BG_DARK, (int)(220 * anim)),
                RenderUtils.getColorWithAlpha(PURPLE_PRIMARY, (int)(255 * anim)),
                md.toggleAnim
            );
            nodeColor = RenderUtils.lerpColor(nodeColor,
                RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(255 * anim)),
                md.hoverAnim);

            RenderUtils.drawCircleFilled(modX, modY, MODULE_NODE_RADIUS + md.hoverAnim * 3, nodeColor);

            // Inner dot
            RenderUtils.drawCircleFilled(modX, modY, 3,
                RenderUtils.getColorWithAlpha(TEXT_BRIGHT, (int)(200 * anim)));

            // Module name label
            String mName = md.module.getName().toUpperCase();
            int nameW = mc.fontRendererObj.getStringWidth(mName);
            int labelColor = RenderUtils.lerpColor(
                RenderUtils.getColorWithAlpha(TEXT_DIM, (int)(200 * anim)),
                RenderUtils.getColorWithAlpha(TEXT_BRIGHT, (int)(255 * anim)),
                md.hoverAnim
            );
            labelColor = RenderUtils.lerpColor(labelColor,
                RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(255 * anim)),
                md.toggleAnim);

            RenderUtils.drawCenteredText(mName, modX, modY + MODULE_NODE_RADIUS + 8, labelColor, true);

            // Keybind indicator
            if (md.module.getKeyBind() != 0) {
                String kb = Keyboard.getKeyName(md.module.getKeyBind());
                if (kb != null) {
                    RenderUtils.drawCenteredText(kb, modX, modY - MODULE_NODE_RADIUS - 10,
                        RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(140 * anim)), false);
                }
            }

            // Expanded settings panel
            if (md.expanded && !md.settings.isEmpty()) {
                double panelX = modX - 60;
                double panelY = modY + MODULE_NODE_RADIUS + 20;
                double panelW = 120;
                double panelH = md.settings.size() * 16 + 8;

                RenderUtils.drawRoundedRect(panelX, panelY, panelW, panelH, 6,
                    RenderUtils.getColorWithAlpha(BG_DARK, (int)(230 * anim)));
                RenderUtils.drawCircleOutline(panelX + panelW / 2, panelY + panelH / 2,
                    Math.max(panelW, panelH) / 2, 0.5f,
                    RenderUtils.getColorWithAlpha(PURPLE_TERTIARY, (int)(100 * anim)));

                for (int s = 0; s < md.settings.size(); s++) {
                    Module.Setting setting = md.settings.get(s);
                    double sy = panelY + 4 + s * 16;

                    String sName = setting.getName();
                    RenderUtils.drawText(sName, panelX + 6, sy,
                        RenderUtils.getColorWithAlpha(TEXT_DIM, (int)(200 * anim)), false);

                    switch (setting.getType()) {
                        case BOOLEAN:
                            String onOff = setting.getBoolean() ? "ON" : "OFF";
                            int onOffColor = setting.getBoolean() ? ENABLED_GREEN : DISABLED_RED;
                            RenderUtils.drawText(onOff, panelX + panelW - mc.fontRendererObj.getStringWidth(onOff) - 6, sy,
                                RenderUtils.getColorWithAlpha(onOffColor, (int)(220 * anim)), false);
                            break;
                        case INTEGER:
                        case DOUBLE:
                            String val = setting.getType() == Module.Setting.Type.INTEGER ?
                                String.valueOf(setting.getInt()) : String.format("%.1f", setting.getDouble());
                            RenderUtils.drawText(val, panelX + panelW - mc.fontRendererObj.getStringWidth(val) - 6, sy,
                                RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(220 * anim)), false);
                            break;
                        case MODE:
                            String mode = setting.getMode();
                            RenderUtils.drawText(mode, panelX + panelW - mc.fontRendererObj.getStringWidth(mode) - 6, sy,
                                RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(220 * anim)), false);
                            break;
                    }
                }
            }
        }
    }

    // ─── Category Icons (Vector/SVG-style via GL11) ─────────

    private void drawCategoryIcon(Category cat, double cx, double cy, float anim, CategoryData cd) {
        double iconScale = 0.7 + cd.hoverAnim * 0.3;
        double s = 6 * iconScale;
        int color = RenderUtils.lerpColor(
            RenderUtils.getColorWithAlpha(TEXT_BRIGHT, (int)(220 * anim)),
            RenderUtils.getColorWithAlpha(TEXT_BRIGHT, (int)(255 * anim)),
            cd.hoverAnim
        );

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();

        switch (cat) {
            case COMBAT:
                // Crosshair icon
                RenderUtils.drawLine2D(cx - s, cy, cx + s, cy, color, 1.5f);
                RenderUtils.drawLine2D(cx, cy - s, cx, cy + s, color, 1.5f);
                RenderUtils.drawCircleOutline(cx, cy, s * 0.7, 1.0f, color);
                break;
            case PLAYER:
                // Person icon (head + body)
                RenderUtils.drawCircleFilled(cx, cy - s * 0.3, s * 0.35, color);
                RenderUtils.drawRoundedRect(cx - s * 0.4, cy + s * 0.1, s * 0.8, s * 0.7, 2, color);
                break;
            case MOVEMENT:
                // Arrow up (speed/movement)
                RenderUtils.drawLine2D(cx, cy - s, cx, cy + s * 0.4, color, 1.5f);
                RenderUtils.drawLine2D(cx - s * 0.5, cy + s * 0.8, cx, cy + s * 0.4, color, 1.5f);
                RenderUtils.drawLine2D(cx + s * 0.5, cy + s * 0.8, cx, cy + s * 0.4, color, 1.5f);
                break;
            case RENDER:
                // Eye icon
                RenderUtils.drawCircleOutline(cx, cy, s * 0.4, 1.5f, color);
                RenderUtils.drawCircleFilled(cx, cy, s * 0.2, color);
                break;
            case MISC:
                // Gear icon (simplified)
                RenderUtils.drawCircleOutline(cx, cy, s * 0.6, 1.5f, color);
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60);
                    double x1 = cx + Math.cos(angle) * s * 0.5;
                    double y1 = cy + Math.sin(angle) * s * 0.5;
                    double x2 = cx + Math.cos(angle) * s * 0.85;
                    double y2 = cy + Math.sin(angle) * s * 0.85;
                    RenderUtils.drawLine2D(x1, y1, x2, y2, color, 1.5f);
                }
                break;
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    // ─── Input Handling ─────────────────────────────────────

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (closing) return;

        // Click on center hub = deselect
        double distToCenter = Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2));
        if (distToCenter < CENTER_RADIUS) {
            if (selectedCategory >= 0) {
                selectedCategory = -1;
            } else {
                closing = true;
            }
            return;
        }

        // Click on category
        if (hoveredCategory >= 0) {
            if (selectedCategory == hoveredCategory) {
                selectedCategory = -1; // deselect
            } else {
                selectedCategory = hoveredCategory;
                // Reset expanded states
                for (ModuleData md : categories.get(selectedCategory).modules) {
                    md.expanded = false;
                }
            }
            return;
        }

        // Click on module
        if (hoveredModule >= 0 && selectedCategory >= 0) {
            ModuleData md = categories.get(selectedCategory).modules.get(hoveredModule);

            if (mouseButton == 0) {
                // LEFT CLICK = TOGGLE (always!)
                md.module.toggle();
                return;
            }
            if (mouseButton == 1) {
                // RIGHT CLICK = expand settings
                md.expanded = !md.expanded;
                return;
            }
        }

        // Click on setting in expanded panel
        if (selectedCategory >= 0) {
            for (ModuleData md : categories.get(selectedCategory).modules) {
                if (!md.expanded) continue;
                double modAngleRad = Math.toRadians(md.angle);
                double modDist = MODULE_RING_RADIUS * categorySelectAnim;
                double modX = centerX + Math.cos(modAngleRad) * modDist;
                double modY = centerY + Math.sin(modAngleRad) * modDist;
                double panelX = modX - 60;
                double panelY = modY + MODULE_NODE_RADIUS + 20;

                for (int s = 0; s < md.settings.size(); s++) {
                    Module.Setting setting = md.settings.get(s);
                    double sy = panelY + 4 + s * 16;
                    if (mouseX >= panelX && mouseX <= panelX + 120 && mouseY >= sy && mouseY <= sy + 14) {
                        handleSettingClick(setting, mouseX, panelX, panelY, 120);
                        return;
                    }
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void handleSettingClick(Module.Setting setting, int mouseX, double panelX, double panelY, int panelW) {
        switch (setting.getType()) {
            case BOOLEAN:
                setting.setValue(!setting.getBoolean());
                break;
            case MODE:
                List<String> opts = setting.getOptions();
                if (opts != null && !opts.isEmpty()) {
                    int idx = opts.indexOf(setting.getMode());
                    // Clicking left half = prev, right half = next
                    if (mouseX < panelX + panelW / 2) {
                        idx = (idx - 1 + opts.size()) % opts.size();
                    } else {
                        idx = (idx + 1) % opts.size();
                    }
                    setting.setValue(opts.get(idx));
                }
                break;
            case INTEGER:
            case DOUBLE:
                double min = setting.getMin();
                double max = setting.getMax();
                double progress = Math.max(0, Math.min(1, (mouseX - panelX) / (double) panelW));
                double newVal = min + (max - min) * progress;
                double step = setting.getStep();
                newVal = Math.round(newVal / step) * step;
                if (setting.getType() == Module.Setting.Type.INTEGER) {
                    setting.setValue((int) Math.max(min, Math.min(max, newVal)));
                } else {
                    setting.setValue(Math.max(min, Math.min(max, newVal)));
                }
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_X) {
            if (selectedCategory >= 0) {
                selectedCategory = -1;
            } else {
                closing = true;
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void onGuiClosed() {
        initialized = false;
        selectedCategory = -1;
        openAnim = 0;
    }

    public void open() {
        openAnim = 0;
        closing = false;
        mc.displayGuiScreen(this);
    }

    public void toggle() {
        if (mc.currentScreen == this) {
            closing = true;
        } else {
            open();
        }
    }

    // ─── Math Helpers ───────────────────────────────────────

    private boolean isMouseInSector(int mx, int my, double cx, double cy, double innerR, double outerR, double startDeg, double endDeg) {
        double dx = mx - cx, dy = my - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < innerR || dist > outerR) return false;

        double angle = Math.toDegrees(Math.atan2(dy, dx));
        return angle >= startDeg && angle <= endDeg;
    }

    private float lerpAnim(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.01f) return target;
        return current + diff * speed;
    }
}
