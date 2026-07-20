package me.cheat.client.modules.movement;

import me.cheat.client.CheatClient;
import me.cheat.client.events.EventTarget;
import me.cheat.client.events.PacketEvent;
import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import net.minecraft.network.play.client.C03PacketPlayer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.Packet;

/**
 * Blink - Stores all position packets and releases them at once.
 * Uses CheatClient.EVENT_BUS (custom) for PacketEvent.
 */
public class Blink extends Module {
    private final List<Packet<?>> packets = new ArrayList<>();
    private final Setting maxPackets = new Setting("Max Packets", 100, 10, 500, 10);
    private final Setting autoDisable = new Setting("Auto Disable", true);

    public Blink() {
        super("Blink", Category.MOVEMENT, 0);
        addSetting(maxPackets);
        addSetting(autoDisable);
    }

    @Override
    protected void onEnable() {
        CheatClient.EVENT_BUS.register(this);
        packets.clear();
    }

    @Override
    protected void onDisable() {
        CheatClient.EVENT_BUS.unregister(this);
        sendAll();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.thePlayer == null) return;
        if (!(event.getPacket() instanceof C03PacketPlayer)) return;

        C03PacketPlayer packet = (C03PacketPlayer) event.getPacket();

        // Only capture position/position-rotation packets
        if (packet instanceof C03PacketPlayer.C04PacketPlayerPosition ||
            packet instanceof C03PacketPlayer.C06PacketPlayerPosLook) {

            event.setCancelled(true);
            packets.add(event.getPacket());

            if (autoDisable.getBoolean() && packets.size() >= maxPackets.getInt()) {
                setEnabled(false);
            }
        }
    }

    @Override
    public void onTick() {
        // Nothing to tick, just hold packets
    }

    private void sendAll() {
        if (mc.getNetHandler() == null) return;
        for (Packet<?> packet : packets) {
            mc.getNetHandler().addToSendQueue(packet);
        }
        packets.clear();
    }

    public int getPacketsStored() {
        return packets.size();
    }

    public boolean isBlinking() {
        return isEnabled() && !packets.isEmpty();
    }
}
