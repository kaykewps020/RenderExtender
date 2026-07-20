package me.cheat.client.mixin;

import me.cheat.client.CheatClient;
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
        // Pre-world render
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void onHurtCameraEffect(float partialTicks, CallbackInfo ci) {
        // Can cancel hurt cam
    }

    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true)
    private void onGetFOVModifier(float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Float> cir) {
        // Can modify FOV
    }
}
