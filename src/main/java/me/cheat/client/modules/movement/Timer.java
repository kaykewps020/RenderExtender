package me.cheat.client.modules.movement;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;

/**
 * Timer - Speed hack. Modifies the game timer to speed up or slow down the game.
 * Server-side: spoof tick timing. Client-side: change timer speed.
 */
public class Timer extends Module {
    private final Setting speed = new Setting("Speed", 2.0, 0.1, 10.0, 0.1);
    private final Setting mode = new Setting("Mode", "Client",
        java.util.Arrays.asList("Client", "Packet", "Step"));
    private final Setting sneakSlowdown = new Setting("Sneak Slowdown", true);

    public Timer() {
        super("Timer", Category.MOVEMENT, 0);
        addSetting(speed);
        addSetting(mode);
        addSetting(sneakSlowdown);
    }

    @Override
    protected void onEnable() {
        mc.timer.timerSpeed = (float) speed.getDouble();
    }

    @Override
    protected void onDisable() {
        mc.timer.timerSpeed = 1.0f;
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null) return;

        switch (mode.getMode()) {
            case "Client":
                mc.timer.timerSpeed = (float) speed.getDouble();
                if (sneakSlowdown.getBoolean() && mc.thePlayer.isSneaking()) {
                    mc.timer.timerSpeed = 1.0f;
                }
                break;
            case "Step":
                // Step up blocks quickly
                mc.timer.timerSpeed = (float) speed.getDouble();
                // Reset after step
                if (mc.thePlayer.stepHeight > 0.6f) {
                    mc.thePlayer.stepHeight = 0.6f;
                }
                break;
        }
    }
}
