package me.cheat.client.modules.combat;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.util.Random;

public class HitBox extends Module {
    private final Random random = new Random();

    private final Setting width = new Setting("Width", 0.4, 0.1, 1.5, 0.05);
    private final Setting height = new Setting("Height", 0.15, 0.0, 0.8, 0.05);
    private final Setting expandOnly = new Setting("Expand Only", true);
    private final Setting playersOnly = new Setting("Players Only", true);
    private final Setting randomize = new Setting("Randomize", 0.1, 0.0, 0.5, 0.05);

    public HitBox() {
        super("HitBox", Category.COMBAT, 0);
        addSetting(width);
        addSetting(height);
        addSetting(expandOnly);
        addSetting(playersOnly);
        addSetting(randomize);
    }

    public AxisAlignedBB getExpandedBox(Entity entity, AxisAlignedBB original) {
        if (!isEnabled()) return original;
        if (playersOnly.getBoolean() && !(entity instanceof EntityPlayer)) return original;
        if (entity == mc.thePlayer) return original;

        double w = width.getDouble();
        double h = height.getDouble();

        if (randomize.getDouble() > 0) {
            w += (random.nextDouble() - 0.5) * 2 * randomize.getDouble();
            h += (random.nextDouble() - 0.5) * 2 * randomize.getDouble() * 0.5;
        }

        if (expandOnly.getBoolean()) {
            w = Math.max(0, w);
            h = Math.max(0, h);
        }

        return original.expand(w, h, w);
    }

    public Vec3 getHitBoxSize(Entity entity) {
        if (!isEnabled() || entity == mc.thePlayer) {
            if (entity instanceof EntityLivingBase) {
                return new Vec3(0.6, entity.height, 0.6);
            }
            return new Vec3(0.6, 0.6, 0.6);
        }

        double w = 0.6 + width.getDouble();
        double h = entity.height + height.getDouble();

        if (randomize.getDouble() > 0) {
            w += (random.nextDouble() - 0.5) * 2 * randomize.getDouble();
            h += (random.nextDouble() - 0.5) * 2 * randomize.getDouble() * 0.5;
        }

        return new Vec3(w, h, w);
    }
}
