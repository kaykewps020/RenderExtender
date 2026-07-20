package me.cheat.client.modules.render;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;

/**
 * HUD - On-screen display of active modules, FPS, coordinates, etc.
 * Rendered in MixinGuiIngame.
 */
public class HUD extends Module {
    private final Setting showArray = new Setting("Array List", true);
    private final Setting showFPS = new Setting("Show FPS", false);
    private final Setting showCoords = new Setting("Show Coords", false);
    private final Setting showPing = new Setting("Show Ping", false);
    private final Setting bgColor = new Setting("Background Opacity", 180, 50, 255, 10);

    public HUD() {
        super("HUD", Category.RENDER, 0);
        addSetting(showArray);
        addSetting(showFPS);
        addSetting(showCoords);
        addSetting(showPing);
        addSetting(bgColor);
    }

    @Override
    protected void onEnable() {
        // Always enabled for rendering
    }

    @Override
    protected void onDisable() {
        // Always available
    }
}
