package me.cheat.client.mixin;

import me.cheat.client.CheatClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRunTick(CallbackInfo ci) {
        // Called every tick
    }

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void onClickMouse(CallbackInfo ci) {
        // Can intercept attacks
    }
}
