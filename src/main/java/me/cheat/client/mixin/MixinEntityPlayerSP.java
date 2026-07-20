package me.cheat.client.mixin;

import com.mojang.authlib.GameProfile;
import me.cheat.client.CheatClient;
import me.cheat.client.events.UpdateEvent;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {

    public MixinEntityPlayerSP(World world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void onUpdatePre(CallbackInfo ci) {
        UpdateEvent event = new UpdateEvent(rotationYaw, rotationPitch, onGround);
        CheatClient.EVENT_BUS.call(event);
        if (event.isRotating()) {
            rotationYaw = event.getYaw();
            rotationPitch = event.getPitch();
        }
    }

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void onUpdatePost(CallbackInfo ci) {
        // Post-update hook
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"), cancellable = true)
    private void onUpdateWalkingPlayer(CallbackInfo ci) {
        // Can intercept movement packets
    }

    @Inject(method = "sendPlayerAbilities", at = @At("HEAD"))
    private void onSendAbilities(CallbackInfo ci) {
        // Can modify abilities
    }
}
