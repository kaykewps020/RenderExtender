package me.cheat.client.modules.movement;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import org.lwjgl.input.Keyboard;

/**
 * AutoSprint - Automatically sprints when moving forward.
 * Supports legitimate sprint and Omnisprint (all directions).
 */
public class AutoSprint extends Module {
    private final Setting mode = new Setting("Mode", "Legit",
        java.util.Arrays.asList("Legit", "Omni", "KeepSprint"));
    private final Setting onlyOnGround = new Setting("Only On Ground", false);

    public AutoSprint() {
        super("AutoSprint", Category.MOVEMENT, 0);
        addSetting(mode);
        addSetting(onlyOnGround);
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null) return;
        if (mc.thePlayer.isSprinting()) return;
        if (mc.thePlayer.getHealth() <= 0) return;
        if (mc.currentScreen != null) return;

        boolean movingForward = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
        boolean movingBack = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
        boolean movingLeft = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
        boolean movingRight = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
        boolean sneaking = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
        boolean usingItem = mc.thePlayer.isUsingItem();

        if (sneaking || usingItem) return;
        if (!movingForward && mode.getMode().equals("Legit")) return;

        boolean isMoving = movingForward || movingBack || movingLeft || movingRight;
        if (!isMoving) return;

        if (onlyOnGround.getBoolean() && !mc.thePlayer.onGround) return;

        switch (mode.getMode()) {
            case "Legit":
                if (movingForward && !movingBack) {
                    mc.thePlayer.setSprinting(true);
                }
                break;
            case "Omni":
                mc.thePlayer.setSprinting(true);
                break;
            case "KeepSprint":
                mc.thePlayer.setSprinting(true);
                // Don't cancel sprint on attack
                break;
        }
    }
}
