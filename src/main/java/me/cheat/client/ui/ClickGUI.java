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

/**
 * RadialClickGUI - CS:GO-style circular menu with interactive sliders/toggles.
 *
 * Controls:
 *   Left-click on module node  = TOGGLE module on/off
 *   Right-click on module node = expand/collapse settings
 *   Left-click on setting      = interact (toggle bool, change mode)
 *   Drag on slider track       = change integer/double value
 *   Click center hub           = deselect category / close
 *   ESC / X                    = close
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

    // ─── Layout ─────────────────────────────────────────────
    private static final double CENTER_RADIUS = 24;
    private static final double CATEGORY_RING_RADIUS = 90;
    private static final double MODULE_RING_RADIUS = 150;
    private static final double MODULE_NODE_RADIUS = 6;

    // ─── State ──────────────────────────────────────────────
    private boolean initialized = false;
    private float openAnim = 0;
    private boolean closing = false;

    private int selectedCategory = -1;
    private float categorySelectAnim = 0;
    private int hoveredCategory = -1;
    private int hoveredModule = -1;

    // Slider drag state
    private boolean draggingSlider = false;
    private Module.Setting dragSetting = null;
    private double dragPanelX = 0;
    private double dragPanelW = 0;

    private int centerX, centerY;
    private long openTime;

    // Category/module cached positions for hit testing during drag
    private final List<double[]> modulePositions = new ArrayList<>();

    // Category data
    private final List<CategoryData> categories = new ArrayList<>();

    // ─── Data Classes ───────────────────────────────────────

    private static class CategoryData {
        Category category;
        double angle;
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
            double segSize = 360.0 / cats.length;

            for (int i = 0; i < cats.length; i++) {
                CategoryData cd = new CategoryData();
                cd.category = cats[i];
                cd.angle = -90 + segSize * i;

                List<Module> modules = ModuleManager.getModulesInCategory(cats[i]);
                double modStart = cd.angle - segSize / 2 + 10;
                double modSeg = (segSize - 20) / Math.max(1, modules.size());
                for (int j = 0; j < modules.size(); j++) {
                    cd.modules.add(new ModuleData(modules.get(j), modStart + modSeg * j + modSeg / 2));
                }
                categories.add(cd);
            }
            initialized = true;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  RENDERING
    // ═══════════════════════════════════════════════════════════

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

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();

        double scale = 0.6f + anim * 0.4f;
        double ringR = CATEGORY_RING_RADIUS * scale;

        // Ambient glow
        RenderUtils.drawCircleGradient(centerX, centerY, ringR + 30,
            RenderUtils.getColorWithAlpha(0x000000, 0),
            RenderUtils.getColorWithAlpha(PURPLE_TERTIARY, (int)(40 * anim)));

        // Background ring
        RenderUtils.drawCircleFilled(centerX, centerY, ringR + 4,
            RenderUtils.getColorWithAlpha(0x0A0514, (int)(180 * anim)));

        // ─── Category Segments ──────────────────────────────
        hoveredCategory = -1;
        double segSize = 360.0 / categories.size();

        for (int i = 0; i < categories.size(); i++) {
            CategoryData cd = categories.get(i);
            double startA = cd.angle - segSize / 2 + 1;
            double endA = cd.angle + segSize / 2 - 1;

            boolean segHovered = isMouseInSector(mouseX, mouseY, centerX, centerY, ringR - 8, ringR + 8, startA, endA);
            if (segHovered) hoveredCategory = i;

            cd.hoverAnim = lerpAnim(cd.hoverAnim, segHovered ? 1 : 0, 0.12f);
            cd.selectAnim = lerpAnim(cd.selectAnim, (selectedCategory == i) ? 1 : 0, 0.1f);

            int segColor = RenderUtils.lerpColor(
                RenderUtils.getColorWithAlpha(PURPLE_TERTIARY, (int)(120 * anim)),
                RenderUtils.getColorWithAlpha(PURPLE_PRIMARY, (int)(255 * anim)),
                cd.hoverAnim);
            segColor = RenderUtils.lerpColor(segColor,
                RenderUtils.getColorWithAlpha(PURPLE_SECONDARY, (int)(255 * anim)),
                cd.selectAnim);

            RenderUtils.drawArc(centerX, centerY, ringR, startA, endA, segColor);
            RenderUtils.drawCirclePartial(centerX, centerY, ringR, 1.5f, startA, endA,
                RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(180 * anim * cd.hoverAnim + 80 * anim)));

            // Icon
            double iconAngle = Math.toRadians(cd.angle);
            double iconX = centerX + Math.cos(iconAngle) * ringR;
            double iconY = centerY + Math.sin(iconAngle) * ringR;
            drawCategoryIcon(cd.category, iconX, iconY, anim, cd);

            // Label
            double labelDist = ringR + 22;
            double labelX = centerX + Math.cos(iconAngle) * labelDist;
            double labelY = centerY + Math.sin(iconAngle) * labelDist;
            String name = cd.category.getName().toUpperCase();
            int nameColor = RenderUtils.lerpColor(
                RenderUtils.getColorWithAlpha(TEXT_DIM, (int)(200 * anim)),
                RenderUtils.getColorWithAlpha(TEXT_BRIGHT, (int)(255 * anim)),
                cd.hoverAnim);
            RenderUtils.drawCenteredText(name, labelX, labelY - 4, nameColor, true);
            RenderUtils.drawCenteredText(String.valueOf(cd.modules.size()), labelX, labelY + 7,
                RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(150 * anim)), false);
        }

        // ─── Center Hub ─────────────────────────────────────
        RenderUtils.drawCircleFilled(centerX, centerY, CENTER_RADIUS + 2,
            RenderUtils.getColorWithAlpha(0x0A0514, (int)(200 * anim)));
        RenderUtils.drawCircleOutline(centerX, centerY, CENTER_RADIUS + 2, 2.0f,
            RenderUtils.getColorWithAlpha(PURPLE_PRIMARY, (int)(220 * anim)));
        RenderUtils.drawCenteredText("RE", centerX, centerY - 8,
            RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(255 * anim)), true);
        RenderUtils.drawCenteredText("v2.0", centerX, centerY + 3,
            RenderUtils.getColorWithAlpha(TEXT_DIM, (int)(180 * anim)), false);

        // ─── Selected Category Modules ──────────────────────
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

    // ─── Module Nodes + Settings Panel ──────────────────────

    private void drawModules(CategoryData cat, int mouseX, int mouseY, float partialTicks, float anim) {
        modulePositions.clear();
        double modAngleSpread = 80;
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

            modulePositions.add(new double[]{modX, modY, i});

            // Hover
            double distToMouse = Math.sqrt(Math.pow(mouseX - modX, 2) + Math.pow(mouseY - modY, 2));
            boolean hovered = distToMouse < MODULE_NODE_RADIUS + 6;
            if (hovered) hoveredModule = i;
            md.hoverAnim = lerpAnim(md.hoverAnim, hovered ? 1 : 0, 0.15f);
            md.toggleAnim = lerpAnim(md.toggleAnim, md.module.isEnabled() ? 1 : 0, 0.1f);

            // Glow
            if (md.module.isEnabled()) {
                RenderUtils.drawGlow(modX, modY, MODULE_NODE_RADIUS + 8, PURPLE_PRIMARY, 4);
            }

            // Node
            int nodeColor = RenderUtils.lerpColor(
                RenderUtils.getColorWithAlpha(BG_DARK, (int)(220 * anim)),
                RenderUtils.getColorWithAlpha(PURPLE_PRIMARY, (int)(255 * anim)),
                md.toggleAnim);
            nodeColor = RenderUtils.lerpColor(nodeColor,
                RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(255 * anim)),
                md.hoverAnim);
            RenderUtils.drawCircleFilled(modX, modY, MODULE_NODE_RADIUS + md.hoverAnim * 3, nodeColor);
            RenderUtils.drawCircleFilled(modX, modY, 3,
                RenderUtils.getColorWithAlpha(TEXT_BRIGHT, (int)(200 * anim)));

            // Label
            String mName = md.module.getName().toUpperCase();
            int labelColor = RenderUtils.lerpColor(
                RenderUtils.getColorWithAlpha(TEXT_DIM, (int)(200 * anim)),
                RenderUtils.getColorWithAlpha(TEXT_BRIGHT, (int)(255 * anim)),
                md.hoverAnim);
            labelColor = RenderUtils.lerpColor(labelColor,
                RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(255 * anim)),
                md.toggleAnim);
            RenderUtils.drawCenteredText(mName, modX, modY + MODULE_NODE_RADIUS + 8, labelColor, true);

            // Keybind
            if (md.module.getKeyBind() != 0) {
                String kb = Keyboard.getKeyName(md.module.getKeyBind());
                if (kb != null) {
                    RenderUtils.drawCenteredText(kb, modX, modY - MODULE_NODE_RADIUS - 10,
                        RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(140 * anim)), false);
                }
            }

            // ─── Expanded Settings Panel ────────────────────
            if (md.expanded) {
                List<Module.Setting> settings = md.module.getSettings();
                if (!settings.isEmpty()) {
                    double panelW = 130;
                    double panelH = settings.size() * 22 + 10;
                    double panelX = modX - panelW / 2;
                    double panelY = modY + MODULE_NODE_RADIUS + 22;

                    // Panel background
                    RenderUtils.drawRoundedRect(panelX - 2, panelY - 2, panelW + 4, panelH + 4, 8,
                        RenderUtils.getColorWithAlpha(BG_DARK, (int)(240 * anim)));
                    RenderUtils.drawCircleOutline(panelX + panelW / 2, panelY + panelH / 2,
                        Math.max(panelW, panelH) / 2 + 4, 1.0f,
                        RenderUtils.getColorWithAlpha(PURPLE_TERTIARY, (int)(80 * anim)));

                    for (int s = 0; s < settings.size(); s++) {
                        Module.Setting setting = settings.get(s);
                        double sy = panelY + 5 + s * 22;

                        switch (setting.getType()) {
                            case BOOLEAN:
                                drawBooleanSetting(setting, panelX, sy, panelW, anim, mouseX, mouseY);
                                break;
                            case INTEGER:
                                drawSliderSetting(setting, panelX, sy, panelW, anim, true);
                                break;
                            case DOUBLE:
                                drawSliderSetting(setting, panelX, sy, panelW, anim, false);
                                break;
                            case MODE:
                                drawModeSetting(setting, panelX, sy, panelW, anim);
                                break;
                        }
                    }
                }
            }
        }
    }

    // ─── Setting Renderers ──────────────────────────────────

    private void drawBooleanSetting(Module.Setting setting, double x, double y, double w, float anim, int mouseX, int mouseY) {
        // Label
        RenderUtils.drawText(setting.getName(), x + 4, y + 2,
            RenderUtils.getColorWithAlpha(TEXT_DIM, (int)(220 * anim)), false);

        // Toggle switch
        boolean val = setting.getBoolean();
        double swX = x + w - 32;
        double swY = y + 1;

        int bgColor = val ?
            RenderUtils.getColorWithAlpha(ENABLED_GREEN, (int)(220 * anim)) :
            RenderUtils.getColorWithAlpha(0xFF3A3555, (int)(220 * anim));
        RenderUtils.drawRoundedRect(swX, swY, 28, 12, 6, bgColor);

        // Knob
        double knobX = val ? swX + 14 : swX + 2;
        RenderUtils.drawCircleFilled(knobX + 4, swY + 6, 4.5,
            RenderUtils.getColorWithAlpha(TEXT_BRIGHT, (int)(240 * anim)));

        // ON/OFF text
        String txt = val ? "ON" : "OFF";
        int txtColor = val ? ENABLED_GREEN : DISABLED_RED;
        RenderUtils.drawText(txt, swX - mc.fontRendererObj.getStringWidth(txt) - 4, y + 2,
            RenderUtils.getColorWithAlpha(txtColor, (int)(200 * anim)), false);
    }

    private void drawSliderSetting(Module.Setting setting, double x, double y, double w, float anim, boolean isInt) {
        // Label
        RenderUtils.drawText(setting.getName(), x + 4, y,
            RenderUtils.getColorWithAlpha(TEXT_DIM, (int)(220 * anim)), false);

        // Value
        double val = isInt ? setting.getInt() : setting.getDouble();
        String valStr = isInt ? String.valueOf((int) val) : String.format("%.1f", val);
        int valW = mc.fontRendererObj.getStringWidth(valStr);
        RenderUtils.drawText(valStr, x + w - valW - 4, y,
            RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(240 * anim)), false);

        // Track background
        double trackY = y + 12;
        double trackH = 4;
        RenderUtils.drawRoundedRect(x + 4, trackY, w - 8, trackH, 2,
            RenderUtils.getColorWithAlpha(0xFF1A1533, (int)(220 * anim)));

        // Filled portion
        double min = setting.getMin();
        double max = setting.getMax();
        double progress = Math.max(0, Math.min(1, (val - min) / (max - min)));
        double fillW = (w - 8) * progress;
        if (fillW > 1) {
            RenderUtils.drawRoundedRect(x + 4, trackY, fillW, trackH, 2,
                RenderUtils.getColorWithAlpha(PURPLE_PRIMARY, (int)(240 * anim)));
        }

        // Knob
        double knobX = x + 4 + fillW - 4;
        RenderUtils.drawCircleFilled(knobX + 4, trackY + 2, 5,
            RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(255 * anim)));
        RenderUtils.drawCircleFilled(knobX + 4, trackY + 2, 2.5,
            RenderUtils.getColorWithAlpha(TEXT_BRIGHT, (int)(255 * anim)));
    }

    private void drawModeSetting(Module.Setting setting, double x, double y, double w, float anim) {
        // Label
        RenderUtils.drawText(setting.getName(), x + 4, y + 2,
            RenderUtils.getColorWithAlpha(TEXT_DIM, (int)(220 * anim)), false);

        String mode = setting.getMode();
        int modeW = mc.fontRendererObj.getStringWidth(mode);

        // Mode box background
        double boxX = x + w - modeW - 16;
        RenderUtils.drawRoundedRect(boxX, y, modeW + 14, 14, 4,
            RenderUtils.getColorWithAlpha(BG_LIGHT, (int)(180 * anim)));

        // < arrow
        RenderUtils.drawText("<", boxX + 1, y + 2,
            RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(200 * anim)), false);

        // Mode text
        RenderUtils.drawText(mode, boxX + 10, y + 2,
            RenderUtils.getColorWithAlpha(TEXT_MAIN, (int)(240 * anim)), false);

        // > arrow
        RenderUtils.drawText(">", boxX + modeW + 7, y + 2,
            RenderUtils.getColorWithAlpha(PURPLE_ACCENT, (int)(200 * anim)), false);
    }

    // ═══════════════════════════════════════════════════════════
    //  CATEGORY ICONS (Vector/SVG-style)
    // ═══════════════════════════════════════════════════════════

    private void drawCategoryIcon(Category cat, double cx, double cy, float anim, CategoryData cd) {
        double iconScale = 0.7 + cd.hoverAnim * 0.3;
        double s = 6 * iconScale;
        int color = RenderUtils.lerpColor(
            RenderUtils.getColorWithAlpha(TEXT_BRIGHT, (int)(220 * anim)),
            RenderUtils.getColorWithAlpha(TEXT_BRIGHT, (int)(255 * anim)),
            cd.hoverAnim);

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();

        switch (cat) {
            case COMBAT:
                RenderUtils.drawLine2D(cx - s, cy, cx + s, cy, color, 1.5f);
                RenderUtils.drawLine2D(cx, cy - s, cx, cy + s, color, 1.5f);
                RenderUtils.drawCircleOutline(cx, cy, s * 0.7, 1.0f, color);
                break;
            case PLAYER:
                RenderUtils.drawCircleFilled(cx, cy - s * 0.3, s * 0.35, color);
                RenderUtils.drawRoundedRect(cx - s * 0.4, cy + s * 0.1, s * 0.8, s * 0.7, 2, color);
                break;
            case MOVEMENT:
                RenderUtils.drawLine2D(cx, cy - s, cx, cy + s * 0.4, color, 1.5f);
                RenderUtils.drawLine2D(cx - s * 0.5, cy + s * 0.8, cx, cy + s * 0.4, color, 1.5f);
                RenderUtils.drawLine2D(cx + s * 0.5, cy + s * 0.8, cx, cy + s * 0.4, color, 1.5f);
                break;
            case RENDER:
                RenderUtils.drawCircleOutline(cx, cy, s * 0.4, 1.5f, color);
                RenderUtils.drawCircleFilled(cx, cy, s * 0.2, color);
                break;
            case MISC:
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

    // ═══════════════════════════════════════════════════════════
    //  INPUT HANDLING
    // ═══════════════════════════════════════════════════════════

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (closing) return;

        // Center hub click
        double distToCenter = Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2));
        if (distToCenter < CENTER_RADIUS) {
            if (selectedCategory >= 0) {
                selectedCategory = -1;
            } else {
                closing = true;
            }
            return;
        }

        // Category click
        if (hoveredCategory >= 0) {
            selectedCategory = (selectedCategory == hoveredCategory) ? -1 : hoveredCategory;
            if (selectedCategory >= 0) {
                for (ModuleData md : categories.get(selectedCategory).modules) {
                    md.expanded = false;
                }
            }
            draggingSlider = false;
            return;
        }

        // Module click
        if (hoveredModule >= 0 && selectedCategory >= 0) {
            ModuleData md = categories.get(selectedCategory).modules.get(hoveredModule);
            if (mouseButton == 0) {
                md.module.toggle();
                return;
            }
            if (mouseButton == 1) {
                md.expanded = !md.expanded;
                draggingSlider = false;
                return;
            }
        }

        // Setting click in expanded panels
        if (selectedCategory >= 0) {
            if (handleSettingClickAt(mouseX, mouseY, mouseButton)) return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingSlider && dragSetting != null) {
            updateSliderFromMouse(mouseX);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingSlider = false;
        dragSetting = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    private boolean handleSettingClickAt(int mouseX, int mouseY, int mouseButton) {
        for (ModuleData md : categories.get(selectedCategory).modules) {
            if (!md.expanded) continue;
            List<Module.Setting> settings = md.module.getSettings();
            if (settings.isEmpty()) continue;

            double modAngleRad = Math.toRadians(md.angle);
            double modDist = MODULE_RING_RADIUS * categorySelectAnim;
            double modX = centerX + Math.cos(modAngleRad) * modDist;
            double modY = centerY + Math.sin(modAngleRad) * modDist;

            double panelW = 130;
            double panelX = modX - panelW / 2;
            double panelY = modY + MODULE_NODE_RADIUS + 22;

            for (int s = 0; s < settings.size(); s++) {
                Module.Setting setting = settings.get(s);
                double sy = panelY + 5 + s * 22;

                switch (setting.getType()) {
                    case BOOLEAN:
                        // Click on toggle switch area
                        double swX = panelX + panelW - 32;
                        if (mouseX >= swX - 40 && mouseX <= swX + 30 && mouseY >= sy && mouseY <= sy + 14) {
                            setting.setValue(!setting.getBoolean());
                            return true;
                        }
                        break;

                    case INTEGER:
                    case DOUBLE:
                        // Click on slider track starts drag
                        double trackY = sy + 12;
                        if (mouseX >= panelX + 4 && mouseX <= panelX + panelW - 4 &&
                            mouseY >= trackY - 4 && mouseY <= trackY + 10) {
                            draggingSlider = true;
                            dragSetting = setting;
                            dragPanelX = panelX + 4;
                            dragPanelW = panelW - 8;
                            updateSliderFromMouse(mouseX);
                            return true;
                        }
                        break;

                    case MODE:
                        // Click on mode box - left half = prev, right half = next
                        int modeW = mc.fontRendererObj.getStringWidth(setting.getMode());
                        double boxX = panelX + panelW - modeW - 16;
                        if (mouseX >= boxX && mouseX <= boxX + modeW + 14 && mouseY >= sy && mouseY <= sy + 14) {
                            List<String> opts = setting.getOptions();
                            if (opts != null && !opts.isEmpty()) {
                                int idx = opts.indexOf(setting.getMode());
                                if (mouseX < boxX + (modeW + 14) / 2) {
                                    idx = (idx - 1 + opts.size()) % opts.size();
                                } else {
                                    idx = (idx + 1) % opts.size();
                                }
                                setting.setValue(opts.get(idx));
                            }
                            return true;
                        }
                        break;
                }
            }
        }
        return false;
    }

    private void updateSliderFromMouse(int mouseX) {
        if (dragSetting == null) return;
        double progress = Math.max(0, Math.min(1, (mouseX - dragPanelX) / dragPanelW));
        double min = dragSetting.getMin();
        double max = dragSetting.getMax();
        double step = dragSetting.getStep();
        double newVal = min + (max - min) * progress;
        newVal = Math.round(newVal / step) * step;
        newVal = Math.max(min, Math.min(max, newVal));

        if (dragSetting.getType() == Module.Setting.Type.INTEGER) {
            dragSetting.setValue((int) newVal);
        } else {
            dragSetting.setValue(newVal);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_X) {
            if (draggingSlider) {
                draggingSlider = false;
            } else if (selectedCategory >= 0) {
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
        draggingSlider = false;
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

    // ─── Helpers ────────────────────────────────────────────

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
