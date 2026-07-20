package me.cheat.client.mixin;

import io.netty.channel.ChannelHandlerContext;
import me.cheat.client.CheatClient;
import me.cheat.client.events.PacketEvent;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    @Inject(method = "sendPacketNoEvent", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        PacketEvent event = new PacketEvent(packet);
        CheatClient.EVENT_BUS.call(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void onChannelRead(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        PacketEvent event = new PacketEvent(packet);
        CheatClient.EVENT_BUS.call(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
