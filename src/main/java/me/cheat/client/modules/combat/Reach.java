package me.cheat.client.modules.combat;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import me.cheat.client.utils.AntiDetection;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;

import java.util.Random;

public class Reach extends Module {
    private final Random random = new Random();

    private final Setting reachValue = new Setting("Reach", 3.5, 3.0, 6.0, 0.1);
    private final Setting verticalReach = new Setting("Vertical Reach", 1.5, 0.5, 3.0, 0.1);
    private final Setting randomness = new Setting("Randomization", 0.3, 0.0, 1.0, 0.05);

    private int packetsSent = 0;

    public Reach() {
        super("Reach", Category.COMBAT, 0);
        addSetting(reachValue);
        addSetting(verticalReach);
        addSetting(randomness);
    }

    @Override
    protected void onEnable() {
        packetsSent = 0;
    }

    @Override
    protected void onDisable() {
        packetsSent = 0;
    }

    public boolean isWithinReach(Vec3 pos, Vec3 playerPos) {
        if (!isEnabled()) return false;

        double dx = pos.xCoord - playerPos.xCoord;
        double dy = pos.yCoord - playerPos.yCoord;
        double dz = pos.zCoord - playerPos.zCoord;

        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double vertical = Math.abs(dy);

        double baseReach = reachValue.getDouble() +
            (randomness.getDouble() > 0 ? random.nextDouble() * randomness.getDouble() : 0);

        return horizontal <= baseReach && vertical <= verticalReach.getDouble();
    }

    public double getCurrentReach() {
        if (!isEnabled()) return 3.0;
        return reachValue.getDouble() +
            (randomness.getDouble() > 0 ? random.nextDouble() * randomness.getDouble() : 0);
    }

    public double getExtendedReach(double originalReach) {
        if (!isEnabled()) return originalReach;
        double rand = randomness.getDouble() > 0 ?
            (random.nextDouble() - 0.5) * 2 * randomness.getDouble() : 0;
        return Math.max(originalReach, reachValue.getDouble() + rand);
    }
}
