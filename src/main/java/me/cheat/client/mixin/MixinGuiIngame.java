package me.cheat.client.mixin;

import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {

    @Inject(method = "renderGameOverlay", at = @At("HEAD"))
    private void onRenderOverlayPre(float partialTicks, CallbackInfo ci) {
        // Pre-HUD
    }

    @Inject(method = "renderGameOverlay", at = @At("TAIL"))
    private void onRenderOverlayPost(float partialTicks, CallbackInfo ci) {
        // Post-HUD
    }
}
