package me.cheat.client.mixin;

import me.cheat.client.CheatClient;
import me.cheat.client.events.RenderEvent;
import net.minecraft.client.renderer.entity.RenderLiving;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLiving.class)
public class MixinRenderLivingBase {

    @Inject(method = "doRender", at = @At("TAIL"))
    private void onDoRenderPost(CallbackInfo ci) {
        CheatClient.EVENT_BUS.call(new RenderEvent(0f));
    }
}
