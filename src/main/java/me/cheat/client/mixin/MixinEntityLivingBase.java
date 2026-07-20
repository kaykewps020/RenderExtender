package me.cheat.client.mixin;

import me.cheat.client.modules.ModuleManager;
import me.cheat.client.modules.combat.HitBox;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBase {

    @Inject(method = "getEntityBoundingBox", at = @At("RETURN"), cancellable = true)
    private void onGetBoundingBox(CallbackInfoReturnable<AxisAlignedBB> cir) {
        HitBox hitBox = ModuleManager.getModule(HitBox.class);
        if (hitBox != null && hitBox.isEnabled()) {
            AxisAlignedBB expanded = hitBox.getExpandedBox(
                (EntityLivingBase)(Object)this, cir.getReturnValue());
            cir.setReturnValue(expanded);
        }
    }
}
