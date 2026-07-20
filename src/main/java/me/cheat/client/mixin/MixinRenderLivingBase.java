package me.cheat.client.mixin;

import me.cheat.client.CheatClient;
import me.cheat.client.events.RenderEvent;
import me.cheat.client.modules.ModuleManager;
import me.cheat.client.modules.combat.HitBox;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLivingBase.class)
public class MixinRenderLivingBase {

    @Inject(method = "doRender", at = @At("HEAD"))
    private void onDoRenderPre(EntityLivingBase entity, double x, double y, double z, float yaw, float partialTicks, CallbackInfo ci) {
        // Pre-render for living entities
    }

    @Inject(method = "doRender", at = @At("TAIL"))
    private void onDoRenderPost(EntityLivingBase entity, double x, double y, double z, float yaw, float partialTicks, CallbackInfo ci) {
        // Post-render - used for ESP
        if (entity != null) {
            CheatClient.EVENT_BUS.call(new RenderEvent(partialTicks));
        }
    }
}
