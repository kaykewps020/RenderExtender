package me.cheat.client.ui.components;

import me.cheat.client.ui.ClickGUI;
import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import me.cheat.client.modules.ModuleManager;
import me.cheat.client.utils.RenderUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class Frame {
    private final Minecraft mc = Minecraft.getMinecraft();

    private final Category category;
    private double x, y;
    private final double width;
    private final double headerHeight = 18;
    private boolean expanded = true;
    private boolean dragging = false;
    private double dragX, dragY;

    private final List<ModuleButton> moduleButtons = new ArrayList<>();
    private float hoverAnim = 0;

    public Frame(Category category, double x, double y, double width, double headerHeight) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.width = width;

        double moduleY = y + headerHeight + 2;
        for (Module module : ModuleManager.getModulesInCategory(category)) {
            ModuleButton button = new ModuleButton(module, x, moduleY, width, 16);
            moduleButtons.add(button);
            moduleY += 18;
        }
    }

    public void render(int mouseX, int mouseY, float delta, float alpha) {
        boolean hovered = isMouseOverHeader(mouseX, mouseY);
        if (hovered) {
            hoverAnim = Math.min(1.0f, hoverAnim + delta * 0.1f);
        } else {
            hoverAnim = Math.max(0, hoverAnim - delta * 0.05f);
        }

        int bgAlpha = (int)(alpha * 200);
        int headerBg = RenderUtils.lerpColor(
            RenderUtils.getColorWithAlpha(ClickGUI.BG_DARK, bgAlpha),
            RenderUtils.getColorWithAlpha(ClickGUI.BG_MEDIUM, bgAlpha),
            hoverAnim
        );

        double contentHeight = expanded ? moduleButtons.size() * 18 + 4 : 0;
        double totalHeight = headerHeight + contentHeight;

        // Shadow
        RenderUtils.drawRect(x + 2, y + 2, width, totalHeight,
            RenderUtils.getColorWithAlpha(0x000000, (int)(alpha * 80)));

        // Background
        RenderUtils.drawRoundedRect(x, y, width, totalHeight, 3, headerBg);

        // Top accent line
        RenderUtils.drawGradientRect(x + 1, y, width - 2, 2,
            RenderUtils.getColorWithAlpha(ClickGUI.PURPLE_PRIMARY, (int)(alpha * 255)),
            RenderUtils.getColorWithAlpha(ClickGUI.PURPLE_TERTIARY, (int)(alpha * 255)));

        // Category indicator
        RenderUtils.drawRect(x + 4, y + 5, 3, 9,
            RenderUtils.getColorWithAlpha(ClickGUI.PURPLE_ACCENT, (int)(alpha * 200)));

        // Title
        String displayName = category.getName().toUpperCase();
        RenderUtils.drawText(displayName, x + 12, y + 5,
            RenderUtils.getColorWithAlpha(ClickGUI.TEXT_MAIN, (int)(alpha * 255)), true);

        // Module count
        String count = String.valueOf(moduleButtons.size());
        int countWidth = mc.fontRendererObj.getStringWidth(count);
        RenderUtils.drawRoundedRect(x + width - countWidth - 12, y + 4, countWidth + 8, 11, 3,
            RenderUtils.getColorWithAlpha(ClickGUI.PURPLE_TERTIARY, (int)(alpha * 150)));
        RenderUtils.drawText(count, x + width - countWidth - 8, y + 5,
            RenderUtils.getColorWithAlpha(ClickGUI.TEXT_DIM, (int)(alpha * 150)), false);

        // Arrow
        String arrow = expanded ? "v" : ">";
        RenderUtils.drawText(arrow, x + width - 14, y + 4,
            RenderUtils.getColorWithAlpha(ClickGUI.TEXT_DIM, (int)(alpha * 150)), false);

        // Module buttons
        if (expanded) {
            double currentY = y + headerHeight + 2;
            RenderUtils.drawRect(x + 2, y + headerHeight, width - 4, 1,
                RenderUtils.getColorWithAlpha(ClickGUI.PURPLE_TERTIARY, (int)(alpha * 60)));

            for (ModuleButton button : moduleButtons) {
                button.setPosition((int)x, (int)currentY);
                button.render(mouseX, mouseY, delta, alpha);
                currentY += 18;
            }
        }

        // Outline
        RenderUtils.drawOutlinedRect(x, y, width, totalHeight,
            RenderUtils.getColorWithAlpha(ClickGUI.PURPLE_TERTIARY, (int)(alpha * 80)), 0.5f);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            if (isMouseOverHeader(mouseX, mouseY)) {
                dragging = true;
                dragX = mouseX - x;
                dragY = mouseY - y;
                return true;
            }
        }
        if (button == 1 && isMouseOverHeader(mouseX, mouseY)) {
            expanded = !expanded;
            return true;
        }
        if (expanded) {
            for (ModuleButton moduleButton : moduleButtons) {
                if (moduleButton.mouseClicked(mouseX, mouseY, button)) return true;
            }
        }
        return false;
    }

    public void mouseReleased(int mouseX, int mouseY, int button) {
        dragging = false;
        for (ModuleButton moduleButton : moduleButtons) {
            moduleButton.mouseReleased(mouseX, mouseY, button);
        }
    }

    public void mouseDragged(int mouseX, int mouseY, int button) {
        if (dragging && button == 0) {
            x = mouseX - dragX;
            y = mouseY - dragY;
            x = Math.max(0, Math.min(x, mc.currentScreen.width - width));
            y = Math.max(0, Math.min(y, mc.currentScreen.height - headerHeight - 20));

            // Update button positions
            double moduleY = y + headerHeight + 2;
            for (ModuleButton button1 : moduleButtons) {
                button1.setPosition((int)x, (int)moduleY);
                moduleY += 18;
            }
        }
    }

    public boolean keyPressed(int keyCode, char typedChar) {
        if (expanded) {
            for (ModuleButton moduleButton : moduleButtons) {
                if (moduleButton.keyPressed(keyCode, typedChar)) return true;
            }
        }
        return false;
    }

    private boolean isMouseOverHeader(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight;
    }
}
