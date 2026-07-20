package me.cheat.client.mixin;

import me.cheat.client.modules.ModuleManager;
import me.cheat.client.modules.combat.HitBox;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
public class MixinRenderManager {

    @Inject(method = "getBoundingBoxForDebug", at = @At("RETURN"), cancellable = true)
    private void onGetBoundingBox(Entity entity, CallbackInfoReturnable<AxisAlignedBB> cir) {
        HitBox hitBox = ModuleManager.getModule(HitBox.class);
        if (hitBox != null && hitBox.isEnabled()) {
            AxisAlignedBB expanded = hitBox.getExpandedBox(entity, cir.getReturnValue());
            cir.setReturnValue(expanded);
        }
    }
}
