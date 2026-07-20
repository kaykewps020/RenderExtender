package me.cheat.client.mixin;

import com.mojang.authlib.GameProfile;
import me.cheat.client.CheatClient;
import me.cheat.client.events.UpdateEvent;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.stats.StatBase;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
        // Pre-update
    }

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void onUpdatePost(CallbackInfo ci) {
        // Post-update
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
