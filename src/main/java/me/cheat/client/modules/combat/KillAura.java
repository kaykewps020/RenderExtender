package me.cheat.client.modules.combat;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import me.cheat.client.utils.AntiDetection;
import me.cheat.client.utils.RenderUtils;
import me.cheat.client.utils.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class KillAura extends Module {
    private final Random random = new Random();
    private Entity target = null;
    private int ticksSinceLastAttack = 0;
    private float[] targetRotations = null;
    private long lastAttackTime = 0;

    private final Setting range = new Setting("Range", 4.2, 3.0, 6.0, 0.1);
    private final Setting cps = new Setting("CPS", 12, 1, 20, 1);
    private final Setting rotationSpeed = new Setting("Rotation Speed", 15.0, 1.0, 45.0, 1.0);
    private final Setting onlyPlayers = new Setting("Only Players", true);
    private final Setting throughWalls = new Setting("Through Walls", true);
    private final Setting hitChance = new Setting("Hit Chance", 100.0, 1.0, 100.0, 1.0);
    private final Setting invisibles = new Setting("Hit Invisibles", true);
    private final Setting randomizeCPS = new Setting("Randomize CPS", true);
    private final Setting boxESP = new Setting("Box ESP", true);
    private final Setting colorMode = new Setting("Color Mode", "Purple", 
        java.util.Arrays.asList("Purple", "Rainbow", "Health", "White"));

    public KillAura() {
        super("KillAura", Category.COMBAT, 0x13); // R key
        addSetting(range);
        addSetting(cps);
        addSetting(rotationSpeed);
        addSetting(onlyPlayers);
        addSetting(throughWalls);
        addSetting(hitChance);
        addSetting(invisibles);
        addSetting(randomizeCPS);
        addSetting(boxESP);
        addSetting(colorMode);
    }

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        target = null;
        targetRotations = null;
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        target = null;
        targetRotations = null;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player != mc.thePlayer) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.isDead) { target = null; return; }

        target = findTarget();
        if (target == null) {
            targetRotations = null;
            return;
        }

        ticksSinceLastAttack++;

        // Compute rotations to target
        targetRotations = RotationUtils.getRotationsToEntity(target);

        if (targetRotations != null) {
            float[] rots = smoothRotation(targetRotations, (float)rotationSpeed.getDouble());
            mc.thePlayer.rotationYaw = rots[0];
            mc.thePlayer.rotationPitch = rots[1];
        }

        if (shouldAttack()) {
            attack(target);
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!boxESP.getBoolean() || target == null || !(target instanceof EntityLivingBase)) return;

        int color;
        switch (colorMode.getMode()) {
            case "Rainbow":
                color = RenderUtils.getRainbowColor(0, 2);
                break;
            case "Health":
                color = RenderUtils.getHealthColor((EntityLivingBase) target);
                break;
            case "White":
                color = 0xFFFFFFFF;
                break;
            default:
                color = RenderUtils.COLOR_PURPLE_PRIMARY;
        }
        RenderUtils.drawEntityBox(target, color, 2.0f);
    }

    private Entity findTarget() {
        List<Entity> entities = mc.theWorld.loadedEntityList.stream()
            .filter(e -> e != mc.thePlayer)
            .filter(e -> e instanceof EntityLivingBase)
            .filter(e -> !e.isDead)
            .filter(e -> {
                if (onlyPlayers.getBoolean()) return e instanceof EntityPlayer;
                return true;
            })
            .filter(e -> {
                if (!invisibles.getBoolean() && e.isInvisible()) return false;
                return true;
            })
            .filter(e -> mc.thePlayer.getDistanceToEntity(e) <= range.getDouble() + 0.5)
            .filter(e -> {
                if (!throughWalls.getBoolean() && !mc.thePlayer.canEntityBeSeen(e)) return false;
                return true;
            })
            .collect(Collectors.toList());

        if (entities.isEmpty()) return null;

        return entities.stream()
            .min(Comparator.comparingDouble(e -> {
                double dist = mc.thePlayer.getDistanceToEntity(e);
                double health = 20.0 - (e instanceof EntityLivingBase ? 
                    ((EntityLivingBase) e).getHealth() : 20.0);
                double angle = getAngleTo(e);
                return dist * 0.6 + angle * 0.3 + health * 0.1;
            }))
            .orElse(null);
    }

    private double getAngleTo(Entity entity) {
        if (mc.thePlayer == null) return 180;
        Vec3 vec = mc.thePlayer.getLook(1.0F).normalize();
        Vec3 toEntity = new Vec3(
            entity.posX - mc.thePlayer.posX,
            entity.posY + entity.getEyeHeight() / 2 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()),
            entity.posZ - mc.thePlayer.posZ
        ).normalize();
        return Math.acos(MathHelper.clamp_double(vec.dotProduct(toEntity), -1, 1));
    }

    private float[] smoothRotation(float[] target, float speed) {
        if (mc.thePlayer == null) return target;
        float yawDelta = MathHelper.wrapAngleTo180_float(target[0] - mc.thePlayer.rotationYaw);
        float pitchDelta = MathHelper.wrapAngleTo180_float(target[1] - mc.thePlayer.rotationPitch);
        return new float[]{
            mc.thePlayer.rotationYaw + MathHelper.clamp_float(yawDelta, -speed, speed),
            mc.thePlayer.rotationPitch + MathHelper.clamp_float(pitchDelta, -speed * 0.8f, speed * 0.8f)
        };
    }

    private boolean shouldAttack() {
        if (target == null) return false;
        if (mc.thePlayer.getDistanceToEntity(target) > range.getDouble() + 1.0) return false;
        if (random.nextDouble() * 100 > hitChance.getDouble()) return false;

        int maxCps = cps.getInt();
        if (randomizeCPS.getBoolean()) {
            maxCps = (int) (maxCps + random.nextGaussian() * maxCps * 0.2);
            maxCps = MathHelper.clamp_int(maxCps, 1, 30);
        }
        if (!AntiDetection.checkCPS(maxCps)) return false;

        if (System.currentTimeMillis() - lastAttackTime < 50) return false;
        if (System.currentTimeMillis() - lastAttackTime < AntiDetection.getRandomizedDelay(30, 80)) return false;

        return true;
    }

    private void attack(Entity entity) {
        if (mc.getNetHandler() == null) return;

        mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK));
        mc.thePlayer.swingItem();

        // Sync rotations after attack
        if (targetRotations != null) {
            mc.thePlayer.rotationYaw = targetRotations[0];
            mc.thePlayer.rotationPitch = targetRotations[1];
        }

        ticksSinceLastAttack = 0;
        lastAttackTime = System.currentTimeMillis();
    }
}
