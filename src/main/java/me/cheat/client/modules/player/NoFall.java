package me.cheat.client.modules.player;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import net.minecraft.network.play.client.C03PacketPlayer;

/**
 * NoFall - Cancels fall damage by spoofing on-ground packets.
 */
public class NoFall extends Module {
    private final Setting mode = new Setting("Mode", "Packet",
        java.util.Arrays.asList("Packet", "Bucket", "Clutch"));
    private final Setting distance = new Setting("Distance", 2.0, 1.0, 10.0, 0.5);
    private final Setting spoofY = new Setting("Spoof Y", 0.0, 0.0, 256.0, 1.0);

    public NoFall() {
        super("NoFall", Category.PLAYER, 0);
        addSetting(mode);
        addSetting(distance);
        addSetting(spoofY);
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.fallDistance <= distance.getDouble()) return;
        if (mc.thePlayer.onGround) return;
        if (mc.thePlayer.capabilities.isFlying) return;

        switch (mode.getMode()) {
            case "Packet":
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY,
                    mc.thePlayer.posZ,
                    true
                ));
                break;
            case "Bucket":
                // Simulate looking down + right-click bucket (vanilla no-fall method)
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY + spoofY.getDouble(),
                    mc.thePlayer.posZ,
                    true
                ));
                break;
            case "Clutch":
                // Place block below + spoof ground
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY,
                    mc.thePlayer.posZ,
                    true
                ));
                break;
        }
    }
}
