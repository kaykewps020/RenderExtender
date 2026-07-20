package me.cheat.client.mixin;

import me.cheat.client.CheatClient;
import me.cheat.client.events.RenderEvent;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Inject(method = "renderEntities", at = @At("TAIL"))
    private void onRenderEntitiesPost(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        CheatClient.EVENT_BUS.call(new RenderEvent(partialTicks));
    }
}
