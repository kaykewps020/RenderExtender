package me.cheat.client.modules.render;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import org.lwjgl.input.Keyboard;

/**
 * Zoom - OptiFine-style zoom. Reduces FOV when holding the zoom key.
 * Works via MixinEntityRenderer.getFOVModifier.
 */
public class Zoom extends Module {
    private final Setting zoomLevel = new Setting("Zoom Level", 4.0, 2.0, 20.0, 0.5);
    private final Setting smooth = new Setting("Smooth", true);
    private final Setting zoomSpeed = new Setting("Zoom Speed", 0.3, 0.05, 1.0, 0.05);

    private float currentZoom = 1.0f;

    public Zoom() {
        super("Zoom", Category.RENDER, Keyboard.KEY_C);
        addSetting(zoomLevel);
        addSetting(smooth);
        addSetting(zoomSpeed);
    }

    @Override
    public void onTick() {
        // Zoom is applied in MixinEntityRenderer.getFOVModifier
        // This just tracks smooth interpolation
        if (smooth.getBoolean()) {
            float targetZoom = zoomLevel.getDouble();
            float diff = targetZoom - currentZoom;
            if (Math.abs(diff) > 0.01f) {
                currentZoom += diff * zoomSpeed.getDouble();
            } else {
                currentZoom = targetZoom;
            }
        }
    }

    @Override
    protected void onEnable() {
        if (!smooth.getBoolean()) {
            currentZoom = (float) zoomLevel.getDouble();
        }
    }

    @Override
    protected void onDisable() {
        currentZoom = 1.0f;
    }

    public float getZoomLevel() {
        return smooth.getBoolean() ? currentZoom : (float) zoomLevel.getDouble();
    }
}
