package me.cheat.client.events;

import net.minecraft.network.Packet;

public class PacketEvent extends Event {
    private final Packet<?> packet;

    public PacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() { return packet; }

    @SuppressWarnings("unchecked")
    public <T extends Packet<?>> T getPacketAs() {
        return (T) packet;
    }
}
