package me.cheat.client.mixin;

import me.cheat.client.CheatClient;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {

    @Inject(method = "renderGameOverlay", at = @At("HEAD"))
    private void onRenderOverlayPre(float partialTicks, boolean hasScreen, int mouseX, int mouseY, CallbackInfo ci) {
        // Pre-HUD
    }

    @Inject(method = "renderGameOverlay", at = @At("TAIL"))
    private void onRenderOverlayPost(float partialTicks, boolean hasScreen, int mouseX, int mouseY, CallbackInfo ci) {
        // Post-HUD
    }

    @Inject(method = "renderCrosshairs", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshairs(float partialTicks, CallbackInfo ci) {
        // Can modify crosshair
    }
}
