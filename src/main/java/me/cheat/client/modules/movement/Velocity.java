package me.cheat.client.modules.movement;

import me.cheat.client.CheatClient;
import me.cheat.client.events.EventTarget;
import me.cheat.client.events.PacketEvent;
import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

/**
 * Velocity - Anti-knockback. Cancels or reduces server-side knockback.
 * Uses CheatClient.EVENT_BUS (custom) for PacketEvent.
 */
public class Velocity extends Module {
    private final Setting horizontal = new Setting("Horizontal", 0.0, 0.0, 100.0, 1.0);
    private final Setting vertical = new Setting("Vertical", 0.0, 0.0, 100.0, 1.0);
    private final Setting mode = new Setting("Mode", "Cancel",
        java.util.Arrays.asList("Cancel", "Reduce", "Reverse", "Packet"));
    private final Setting chance = new Setting("Chance", 100.0, 1.0, 100.0, 1.0);

    public Velocity() {
        super("Velocity", Category.MOVEMENT, 0);
        addSetting(horizontal);
        addSetting(vertical);
        addSetting(mode);
        addSetting(chance);
    }

    @Override
    protected void onEnable() {
        CheatClient.EVENT_BUS.register(this);
    }

    @Override
    protected void onDisable() {
        CheatClient.EVENT_BUS.unregister(this);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.thePlayer == null) return;
        if (!(event.getPacket() instanceof S12PacketEntityVelocity)) return;

        S12PacketEntityVelocity packet = event.getPacketAs();
        if (packet.getEntityID() != mc.thePlayer.getEntityId()) return;

        if (Math.random() * 100 > chance.getDouble()) return;

        switch (mode.getMode()) {
            case "Cancel":
                event.setCancelled(true);
                break;
            case "Reduce":
                event.setCancelled(true);
                double motX = packet.getMotionX() / 8000.0 * (horizontal.getDouble() / 100.0);
                double motY = packet.getMotionY() / 8000.0 * (vertical.getDouble() / 100.0);
                double motZ = packet.getMotionZ() / 8000.0 * (horizontal.getDouble() / 100.0);
                mc.thePlayer.motionX = motX;
                mc.thePlayer.motionY = motY;
                mc.thePlayer.motionZ = motZ;
                break;
            case "Reverse":
                event.setCancelled(true);
                mc.thePlayer.motionX = -packet.getMotionX() / 8000.0 * 0.1;
                mc.thePlayer.motionZ = -packet.getMotionZ() / 8000.0 * 0.1;
                break;
            case "Packet":
                mc.thePlayer.motionX *= horizontal.getDouble() / 100.0;
                mc.thePlayer.motionY *= vertical.getDouble() / 100.0;
                mc.thePlayer.motionZ *= horizontal.getDouble() / 100.0;
                break;
        }
    }
}
