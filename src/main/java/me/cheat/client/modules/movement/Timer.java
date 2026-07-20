package me.cheat.client.modules.movement;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * Timer - Speed hack. Modifies the game timer to speed up or slow down the game.
 */
public class Timer extends Module {
    private final Setting speed = new Setting("Speed", 2.0, 0.1, 10.0, 0.1);
    private final Setting mode = new Setting("Mode", "Client",
        java.util.Arrays.asList("Client", "Step"));
    private final Setting sneakSlowdown = new Setting("Sneak Slowdown", true);

    private Timer timerInstance = null;

    public Timer() {
        super("Timer", Category.MOVEMENT, 0);
        addSetting(speed);
        addSetting(mode);
        addSetting(sneakSlowdown);
    }

    private Timer getTimer() {
        if (timerInstance == null) {
            try {
                timerInstance = ReflectionHelper.getPrivateValue(
                    Minecraft.class, mc, "timer", "field_71428_T"
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return timerInstance;
    }

    @Override
    protected void onEnable() {
        Timer timer = getTimer();
        if (timer != null) {
            timer.timerSpeed = (float) speed.getDouble();
        }
    }

    @Override
    protected void onDisable() {
        Timer timer = getTimer();
        if (timer != null) {
            timer.timerSpeed = 1.0f;
        }
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null) return;
        Timer timer = getTimer();
        if (timer == null) return;

        switch (mode.getMode()) {
            case "Client":
                timer.timerSpeed = (float) speed.getDouble();
                if (sneakSlowdown.getBoolean() && mc.thePlayer.isSneaking()) {
                    timer.timerSpeed = 1.0f;
                }
                break;
            case "Step":
                timer.timerSpeed = (float) speed.getDouble();
                if (mc.thePlayer.stepHeight > 0.6f) {
                    mc.thePlayer.stepHeight = 0.6f;
                }
                break;
        }
    }
}
