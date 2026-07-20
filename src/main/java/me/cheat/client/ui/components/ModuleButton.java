package me.cheat.client.ui.components;

import me.cheat.client.ui.ClickGUI;
import me.cheat.client.modules.Module;
import me.cheat.client.modules.Module.Setting;
import me.cheat.client.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

public class ModuleButton {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Module module;
    private int x, y;
    private final int width;
    private final int height = 16;
    private boolean expanded = false;
    private boolean binding = false;
    private float hoverAnim = 0;
    private float enabledAnim = 0;

    private final List<Component> components = new ArrayList<>();

    public ModuleButton(Module module, double x, double y, double width, double height) {
        this.module = module;
        this.x = (int)x;
        this.y = (int)y;
        this.width = (int)width;

        for (Setting setting : module.getSettings()) {
            switch (setting.getType()) {
                case BOOLEAN:
                    components.add(new BooleanComponent(setting));
                    break;
                case INTEGER:
                    components.add(new SliderComponent(setting, true));
                    break;
                case DOUBLE:
                    components.add(new SliderComponent(setting, false));
                    break;
                case MODE:
                    components.add(new ModeComponent(setting));
                    break;
            }
        }
    }

    public void render(int mouseX, int mouseY, float delta, float alpha) {
        boolean hovered = isHovered(mouseX, mouseY);
        boolean enabled = module.isEnabled();

        if (hovered) hoverAnim = Math.min(1.0f, hoverAnim + delta * 0.12f);
        else hoverAnim = Math.max(0, hoverAnim - delta * 0.06f);

        if (enabled) enabledAnim = Math.min(1.0f, enabledAnim + delta * 0.08f);
        else enabledAnim = Math.max(0, enabledAnim - delta * 0.04f);

        int baseAlpha = (int)(alpha * 255);

        int bgColor = RenderUtils.lerpColor(
            RenderUtils.getColorWithAlpha(0xCC1E1B3A, baseAlpha),
            RenderUtils.getColorWithAlpha(0xCC2A2555, baseAlpha),
            hoverAnim
        );
        if (enabled) {
            bgColor = RenderUtils.lerpColor(bgColor,
                RenderUtils.getColorWithAlpha(0xCC2D1B69, baseAlpha), enabledAnim);
        }

        RenderUtils.drawRect(x + 2, y, width - 4, height, bgColor);

        // Accent bar
        int accentColor = RenderUtils.lerpColor(
            RenderUtils.getColorWithAlpha(ClickGUI.DISABLED_RED, (int)(alpha * 100)),
            RenderUtils.getColorWithAlpha(ClickGUI.ENABLED_GREEN, (int)(alpha * 255)),
            enabledAnim
        );
        RenderUtils.drawRect(x + 2, y, 2, height, accentColor);

        // Module name
        int textColor = RenderUtils.lerpColor(
            RenderUtils.getColorWithAlpha(ClickGUI.TEXT_DIM, baseAlpha),
            RenderUtils.getColorWithAlpha(ClickGUI.TEXT_BRIGHT, baseAlpha),
            enabledAnim
        );
        RenderUtils.drawText(module.getName(), x + 8, y + 4, textColor, false);

        // Keybind button
        if (module.getKeyBind() != 0) {
            String keyName = Keyboard.getKeyName(module.getKeyBind());
            if (keyName == null) keyName = "KEY_" + module.getKeyBind();
            int keyWidth = mc.fontRendererObj.getStringWidth(keyName);

            if (binding) {
                keyName = "...";
                RenderUtils.drawRoundedRect(x + width - keyWidth - 12, y + 3, keyWidth + 8, 11, 3,
                    RenderUtils.getColorWithAlpha(ClickGUI.ORANGE_ACCENT, (int)(alpha * 150)));
            } else {
                RenderUtils.drawRoundedRect(x + width - keyWidth - 12, y + 3, keyWidth + 8, 11, 3,
                    RenderUtils.getColorWithAlpha(ClickGUI.BG_LIGHT, (int)(alpha * 150)));
            }
            RenderUtils.drawText(keyName, x + width - keyWidth - 8, y + 4,
                RenderUtils.getColorWithAlpha(ClickGUI.TEXT_DIM, (int)(alpha * 180)), false);
        }

        // Setting components
        if (expanded) {
            double compY = y + height + 1;
            for (Component comp : components) {
                comp.setPosition((int)(x + 4), (int)compY);
                comp.render(mouseX, mouseY, delta, alpha);
                compY += comp.getHeight() + 1;
            }
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) return false;

        if (button == 0) {
            if (module.getKeyBind() != 0) {
                String keyName = Keyboard.getKeyName(module.getKeyBind());
                if (keyName == null) keyName = "";
                int keyWidth = mc.fontRendererObj.getStringWidth(keyName);
                if (mouseX >= x + width - keyWidth - 12 && mouseX <= x + width - 4 &&
                    mouseY >= y + 3 && mouseY <= y + 14) {
                    binding = !binding;
                    return true;
                }
            }

            if (!expanded && components.isEmpty()) {
                module.toggle();
            } else {
                expanded = !expanded;
            }
            return true;
        }

        if (button == 1) {
            expanded = !expanded;
            return true;
        }

        if (expanded) {
            for (Component comp : components) {
                if (comp.mouseClicked(mouseX, mouseY, button)) return true;
            }
        }
        return false;
    }

    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (expanded) {
            for (Component comp : components) {
                comp.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public boolean keyPressed(int keyCode, char typedChar) {
        if (binding) {
            if (keyCode == 0x01 || keyCode == 0x2D) { // ESC or X
                binding = false;
                return true;
            }
            module.setKeyBind(keyCode);
            binding = false;
            return true;
        }
        return false;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    private boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    // --- Setting Components ---

    public abstract static class Component {
        protected int x, y;
        protected int width = 100;
        protected int height = 14;

        public abstract void render(int mouseX, int mouseY, float delta, float alpha);
        public boolean mouseClicked(int mouseX, int mouseY, int button) { return false; }
        public void mouseReleased(int mouseX, int mouseY, int button) {}
        public void setPosition(int x, int y) { this.x = x; this.y = y; }
        public int getHeight() { return height; }
    }

    public static class BooleanComponent extends Component {
        private final Setting setting;
        private float toggleAnim = 0;

        public BooleanComponent(Setting setting) {
            this.setting = setting;
        }

        @Override
        public void render(int mouseX, int mouseY, float delta, float alpha) {
            boolean value = setting.getBoolean();
            toggleAnim = value ? Math.min(1.0f, toggleAnim + delta * 0.1f) :
                                 Math.max(0, toggleAnim - delta * 0.1f);

            int baseAlpha = (int)(alpha * 255);
            RenderUtils.drawText(setting.getName(), x, y + 2,
                RenderUtils.getColorWithAlpha(ClickGUI.TEXT_DIM, baseAlpha), false);

            int switchX = x + width - 24;
            int switchY = y + 2;

            int bgColor = RenderUtils.lerpColor(
                RenderUtils.getColorWithAlpha(0xFF3A3555, baseAlpha),
                RenderUtils.getColorWithAlpha(ClickGUI.ENABLED_GREEN, baseAlpha),
                toggleAnim
            );

            RenderUtils.drawRoundedRect(switchX, switchY, 20, 10, 5, bgColor);

            double knobPos = switchX + 2 + (toggleAnim * 10);
            RenderUtils.drawRoundedRect(knobPos, switchY + 1, 8, 8, 4,
                RenderUtils.getColorWithAlpha(ClickGUI.TEXT_BRIGHT, baseAlpha));
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button) {
            int switchX = x + width - 24;
            if (mouseX >= switchX && mouseX <= switchX + 20 &&
                mouseY >= y && mouseY <= y + height) {
                setting.setValue(!setting.getBoolean());
                return true;
            }
            return false;
        }
    }

    public static class SliderComponent extends Component {
        private final Setting setting;
        private final boolean isInt;
        private boolean dragging = false;

        public SliderComponent(Setting setting, boolean isInt) {
            this.setting = setting;
            this.isInt = isInt;
            this.height = 18;
        }

        @Override
        public void render(int mouseX, int mouseY, float delta, float alpha) {
            int baseAlpha = (int)(alpha * 255);
            double val = isInt ? setting.getInt() : setting.getDouble();
            double min = setting.getMin();
            double max = setting.getMax();
            double progress = (val - min) / (max - min);

            RenderUtils.drawText(setting.getName(), x, y + 1,
                RenderUtils.getColorWithAlpha(ClickGUI.TEXT_DIM, baseAlpha), false);

            String valStr = isInt ? String.valueOf((int)val) : String.format("%.1f", val);
            int valWidth = mc.fontRendererObj.getStringWidth(valStr);
            RenderUtils.drawText(valStr, x + width - valWidth, y + 1,
                RenderUtils.getColorWithAlpha(ClickGUI.PURPLE_ACCENT, baseAlpha), false);

            double trackY = y + 11;
            RenderUtils.drawRoundedRect(x, trackY, width, 3, 1.5,
                RenderUtils.getColorWithAlpha(0xFF2A2555, baseAlpha));

            double fillWidth = (width - 4) * progress;
            if (fillWidth > 0) {
                RenderUtils.drawRoundedRect(x + 2, trackY, fillWidth, 3, 1.5,
                    RenderUtils.getColorWithAlpha(ClickGUI.PURPLE_PRIMARY, baseAlpha));
            }

            double knobX = x + 2 + (width - 4) * progress - 3;
            RenderUtils.drawRoundedRect(knobX, trackY - 2, 6, 7, 3,
                RenderUtils.getColorWithAlpha(ClickGUI.PURPLE_ACCENT, baseAlpha));

            if (dragging) {
                double newProgress = MathHelper.clamp_double((mouseX - x) / (double)width, 0, 1);
                double newVal = min + (max - min) * newProgress;
                if (isInt) {
                    int step = (int) setting.getStep();
                    newVal = Math.round(newVal / step) * step;
                    setting.setValue((int) MathHelper.clamp_double(newVal, min, max));
                } else {
                    double step = setting.getStep();
                    newVal = Math.round(newVal / step) * step;
                    setting.setValue(MathHelper.clamp_double(newVal, min, max));
                }
            }
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y + 8 && mouseY <= y + 16) {
                dragging = true;
                return true;
            }
            return false;
        }

        @Override
        public void mouseReleased(int mouseX, int mouseY, int button) {
            dragging = false;
        }
    }

    public static class ModeComponent extends Component {
        private final Setting setting;

        public ModeComponent(Setting setting) {
            this.setting = setting;
        }

        @Override
        public void render(int mouseX, int mouseY, float delta, float alpha) {
            int baseAlpha = (int)(alpha * 255);
            String mode = setting.getMode();

            RenderUtils.drawText(setting.getName(), x, y + 2,
                RenderUtils.getColorWithAlpha(ClickGUI.TEXT_DIM, baseAlpha), false);

            int textWidth = mc.fontRendererObj.getStringWidth(mode);
            RenderUtils.drawRoundedRect(x + width - textWidth - 12, y + 1, textWidth + 10, 12, 3,
                RenderUtils.getColorWithAlpha(ClickGUI.BG_LIGHT, (int)(alpha * 150)));
            RenderUtils.drawText(mode, x + width - textWidth - 8, y + 2,
                RenderUtils.getColorWithAlpha(ClickGUI.PURPLE_ACCENT, baseAlpha), false);

            RenderUtils.drawText("<", x + width - textWidth - 16, y + 1,
                RenderUtils.getColorWithAlpha(ClickGUI.TEXT_DIM, (int)(alpha * 120)), false);
            RenderUtils.drawText(">", x + width - 5, y + 1,
                RenderUtils.getColorWithAlpha(ClickGUI.TEXT_DIM, (int)(alpha * 120)), false);
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button) {
            if (mouseY >= y && mouseY <= y + height && mouseX >= x + width - 80 && mouseX <= x + width) {
                List<String> options = setting.getOptions();
                String currentMode = setting.getMode();
                int currentIndex = options.indexOf(currentMode);

                if (mouseX > x + width - 20) {
                    currentIndex = (currentIndex + 1) % options.size();
                } else {
                    currentIndex = (currentIndex - 1 + options.size()) % options.size();
                }
                setting.setValue(options.get(currentIndex));
                return true;
            }
            return false;
        }
    }
}
