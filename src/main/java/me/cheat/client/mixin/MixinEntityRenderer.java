package me.cheat.client.mixin;

import me.cheat.client.CheatClient;
import me.cheat.client.modules.ModuleManager;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void onRenderWorld(float partialTicks, long nanoTime, CallbackInfo ci) {
        // Pre-world render hook (available for future ESP/render modules)
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void onHurtCameraEffect(float partialTicks, CallbackInfo ci) {
        // Cancel hurt camera shake
        me.cheat.client.modules.Module noHurtCam = ModuleManager.getModule("NoHurtCam");
        if (noHurtCam != null && noHurtCam.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true)
    private void onGetFOVModifier(float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Float> cir) {
        if (CheatClient.mc.thePlayer == null) return;

        float fov = cir.getReturnValue();

        // Zoom module
        me.cheat.client.modules.Module zoom = ModuleManager.getModule("Zoom");
        if (zoom != null && zoom.isEnabled()) {
            float zoomLevel = 1.0f;
            me.cheat.client.modules.Module.Setting zoomSetting = zoom.getSetting("Zoom Level");
            if (zoomSetting != null) {
                zoomLevel = (float) zoomSetting.getDouble();
            }
            fov /= zoomLevel;
            cir.setReturnValue(fov);
        }

        // Sprint FOV effect (from AutoSprint keep sprint)
        if (CheatClient.mc.thePlayer.isSprinting() && useFOVSetting) {
            fov += 1.0f;
        }
    }
}
