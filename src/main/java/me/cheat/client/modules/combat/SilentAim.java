package me.cheat.client.modules.combat;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import me.cheat.client.utils.AntiDetection;
import me.cheat.client.utils.RotationUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Silent Aim - rotates server-side only, client view stays normal.
 * The server receives modified rotations while the player's actual view is unchanged.
 */
public class SilentAim extends Module {
    private final Random random = new Random();
    private float[] silentRotations = null;
    private Entity target = null;

    private final Setting range = new Setting("Range", 4.5, 3.0, 6.0, 0.1);
    private final Setting onlyPlayers = new Setting("Only Players", true);
    private final Setting smooth = new Setting("Smooth", true);
    private final Setting randomAim = new Setting("Random Aim Point", true);
    private final Setting headshotChance = new Setting("Headshot Chance", 30.0, 0.0, 100.0, 1.0);
    private final Setting cps = new Setting("CPS", 14, 1, 20, 1);
    private final Setting throughWalls = new Setting("Through Walls", false);

    public SilentAim() {
        super("SilentAim", Category.COMBAT, 0);
        addSetting(range);
        addSetting(onlyPlayers);
        addSetting(smooth);
        addSetting(randomAim);
        addSetting(headshotChance);
        addSetting(cps);
        addSetting(throughWalls);
    }

    @Override
    protected void onEnable() {
        silentRotations = null;
        target = null;
    }

    @Override
    protected void onDisable() {
        silentRotations = null;
        target = null;
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.isDead) { target = null; return; }

        target = findTarget();
        if (target == null) {
            silentRotations = null;
            return;
        }

        // Compute aim point - optionally aim at head for headshots
        double aimY = target.posY + target.getEyeHeight() / 2.0;
        if (random.nextDouble() * 100 < headshotChance.getDouble()) {
            aimY = target.posY + target.getEyeHeight(); // head
        }

        // Add random spread
        double aimX = target.posX;
        double aimZ = target.posZ;
        if (randomAim.getBoolean()) {
            double spread = 0.1;
            aimX += (random.nextDouble() - 0.5) * spread;
            aimY += (random.nextDouble() - 0.5) * spread;
            aimZ += (random.nextDouble() - 0.5) * spread;
        }

        silentRotations = RotationUtils.getRotationsToPos(aimX, aimY, aimZ);

        // Apply noise to make it less detectable
        if (AntiDetection.hasAntiCheat()) {
            silentRotations[0] = AntiDetection.addRotationNoise(silentRotations[0], 0.5f);
            silentRotations[1] = AntiDetection.addRotationNoise(silentRotations[1], 0.3f);
        }
    }

    public float[] getSilentRotations() {
        return silentRotations;
    }

    public boolean isAiming() {
        return isEnabled() && silentRotations != null;
    }

    private Entity findTarget() {
        List<Entity> entities = mc.theWorld.loadedEntityList.stream()
            .filter(e -> e != mc.thePlayer)
            .filter(e -> e instanceof EntityLivingBase)
            .filter(e -> !e.isDead)
            .filter(e -> !onlyPlayers.getBoolean() || e instanceof EntityPlayer)
            .filter(e -> mc.thePlayer.getDistanceToEntity(e) <= range.getDouble())
            .filter(e -> throughWalls.getBoolean() || mc.thePlayer.canEntityBeSeen(e))
            .collect(Collectors.toList());

        return entities.stream()
            .min(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)))
            .orElse(null);
    }
}
