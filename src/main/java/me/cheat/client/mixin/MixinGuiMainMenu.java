package me.cheat.client.mixin;

import me.cheat.client.ui.CustomTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the default Minecraft main menu with our custom client title screen.
 */
@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {

    @Inject(method = "initGui", at = @At("HEAD"), cancellable = true)
    private void onInitGui(CallbackInfo ci) {
        // Replace with our custom screen
        Minecraft.getMinecraft().displayGuiScreen(new CustomTitleScreen());
        ci.cancel();
    }
}
