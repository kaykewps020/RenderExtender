package me.cheat.client.mixin;

import me.cheat.client.CheatClient;
import me.cheat.client.modules.ModuleManager;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public class MixinKeyBinding {
    @Shadow private boolean pressed;

    @Inject(method = "onTick", at = @At("HEAD"))
    private static void onKeyTick(int keyCode, CallbackInfo ci) {
        // Fired when any key is ticked
    }

    @Inject(method = "setKeyBindState", at = @At("HEAD"))
    private static void onSetKeyBindState(int keyCode, boolean pressed, CallbackInfo ci) {
        if (pressed && keyCode >= 0) {
            ModuleManager.onKeyPress(keyCode);
        }
    }
}
