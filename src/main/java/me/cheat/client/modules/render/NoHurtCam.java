package me.cheat.client.modules.render;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;

/**
 * NoHurtCam - Cancels the red damage overlay/camera shake when taking damage.
 * Works via MixinEntityRenderer.hurtCameraEffect.
 */
public class NoHurtCam extends Module {

    public NoHurtCam() {
        super("NoHurtCam", Category.RENDER, 0);
    }

    // Effect is applied in MixinEntityRenderer.hurtCameraEffect
    // This module has no tick logic needed - just needs to be enabled
}
