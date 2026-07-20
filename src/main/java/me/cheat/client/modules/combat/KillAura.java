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
    private int ticksExisted = 0;

    private final Setting range = new Setting("Range", 4.2, 3.0, 6.0, 0.1);
    private final Setting cps = new Setting("CPS", 12, 1, 20, 1);
    private final Setting rotationSpeed = new Setting("Rotation Speed", 15.0, 1.0, 45.0, 1.0);
    private final Setting onlyPlayers = new Setting("Only Players", false);
    private final Setting throughWalls = new Setting("Through Walls", false);
    private final Setting hitChance = new Setting("Hit Chance", 100.0, 1.0, 100.0, 1.0);
    private final Setting invisibles = new Setting("Hit Invisibles", false);
    private final Setting randomizeCPS = new Setting("Randomize CPS", true);
    private final Setting rotate = new Setting("Rotate", true);
    private final Setting boxESP = new Setting("Box ESP", false);

    public KillAura() {
        super("KillAura", Category.COMBAT, 0x13);
        addSetting(range);
        addSetting(cps);
        addSetting(rotationSpeed);
        addSetting(onlyPlayers);
        addSetting(throughWalls);
        addSetting(hitChance);
        addSetting(invisibles);
        addSetting(randomizeCPS);
        addSetting(rotate);
        addSetting(boxESP);
    }

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        target = null;
        targetRotations = null;
        ticksSinceLastAttack = 0;
        lastAttackTime = 0;
        ticksExisted = 0;
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        target = null;
        targetRotations = null;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.isDead) { target = null; return; }

        try {
            ticksExisted++;
            ticksSinceLastAttack++;

            // Find target
            target = findTarget();

            if (target == null) {
                targetRotations = null;
                return;
            }

            // Compute rotations to target center mass (not eyes)
            targetRotations = computeRotations(target);

            if (targetRotations != null && rotate.getBoolean()) {
                float speed = (float) rotationSpeed.getDouble();
                float yawDelta = MathHelper.wrapAngleTo180_float(targetRotations[0] - mc.thePlayer.rotationYaw);
                float pitchDelta = MathHelper.wrapAngleTo180_float(targetRotations[1] - mc.thePlayer.rotationPitch);

                mc.thePlayer.rotationYaw += MathHelper.clamp_float(yawDelta, -speed, speed);
                mc.thePlayer.rotationPitch += MathHelper.clamp_float(pitchDelta, -speed * 0.8f, speed * 0.8f);

                // Clamp pitch to valid range to prevent crash
                mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch, -90.0f, 90.0f);
            }

            // Attack
            if (shouldAttack()) {
                performAttack(target);
            }
        } catch (Exception e) {
            // Silently handle any exception during tick to prevent crash
            target = null;
            targetRotations = null;
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!boxESP.getBoolean() || target == null || mc.thePlayer == null || mc.theWorld == null) return;
        try {
            if (!(target instanceof EntityLivingBase)) return;
            if (target.isDead) return;
            if (mc.thePlayer.getDistanceToEntity(target) > range.getDouble() + 2.0) return;

            RenderUtils.drawEntityBox(target, RenderUtils.COLOR_PURPLE_PRIMARY, 2.0f);
        } catch (Exception e) {
            // Don't crash the renderer
        }
    }

    /**
     * Compute rotations to target body center (feet+eyes/2) instead of eye height
     * to avoid overshooting pitch
     */
    private float[] computeRotations(Entity entity) {
        if (entity == null || mc.thePlayer == null) return null;

        // Aim at body center - more stable than eye height
        double targetY = entity.posY + (entity.getEyeHeight() * 0.5);
        double dx = entity.posX - mc.thePlayer.posX;
        double dy = targetY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = entity.posZ - mc.thePlayer.posZ;

        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001) dist = 0.001;

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        // Clamp pitch BEFORE returning
        pitch = MathHelper.clamp_float(pitch, -90.0f, 90.0f);

        return new float[]{yaw, pitch};
    }

    private Entity findTarget() {
        try {
            List<Entity> entities = mc.theWorld.loadedEntityList.stream()
                .filter(e -> e != mc.thePlayer)
                .filter(e -> e instanceof EntityLivingBase)
                .filter(e -> !e.isDead)
                .filter(e -> e.isEntityAlive())
                .filter(e -> {
                    if (onlyPlayers.getBoolean()) return e instanceof EntityPlayer;
                    return true;
                })
                .filter(e -> {
                    if (!invisibles.getBoolean() && e.isInvisible()) return false;
                    return true;
                })
                .filter(e -> mc.thePlayer.getDistanceToEntity(e) <= range.getDouble())
                .filter(e -> {
                    if (!throughWalls.getBoolean() && !mc.thePlayer.canEntityBeSeen(e)) return false;
                    return true;
                })
                .collect(Collectors.toList());

            if (entities.isEmpty()) return null;

            // Prefer closest to crosshair, then by distance
            return entities.stream()
                .min(Comparator.comparingDouble(e -> {
                    double dist = mc.thePlayer.getDistanceToEntity(e);
                    double angle = getAngleTo(e);
                    return dist * 0.5 + angle * 0.5;
                }))
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private double getAngleTo(Entity entity) {
        try {
            Vec3 vec = mc.thePlayer.getLook(1.0F).normalize();
            Vec3 toEntity = new Vec3(
                entity.posX - mc.thePlayer.posX,
                entity.posY + entity.getEyeHeight() * 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()),
                entity.posZ - mc.thePlayer.posZ
            ).normalize();
            return Math.acos(MathHelper.clamp_double(vec.dotProduct(toEntity), -1, 1));
        } catch (Exception e) {
            return Math.PI;
        }
    }

    private boolean shouldAttack() {
        if (target == null || mc.thePlayer == null) return false;

        // Re-validate target
        if (target.isDead || !target.isEntityAlive()) return false;

        double dist = mc.thePlayer.getDistanceToEntity(target);
        if (dist > range.getDouble() + 0.5) return false;

        if (random.nextDouble() * 100.0 > hitChance.getDouble()) return false;

        // Cooldown check - more reliable than CPS limiter
        long now = System.currentTimeMillis();
        long delay = randomizeCPS.getBoolean() ?
            AntiDetection.getRandomizedDelay(50, 110) :
            AntiDetection.getRandomizedDelay(80, 100);

        if (now - lastAttackTime < delay) return false;

        return true;
    }

    private void performAttack(Entity entity) {
        try {
            if (mc.getNetHandler() == null) return;
            if (entity == null || entity.isDead || !entity.isEntityAlive()) return;

            // Send attack packet
            mc.getNetHandler().addToSendQueue(
                new C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK)
            );

            // Swing client-side only - do NOT trigger swingItem() which can cause issues
            // The server will sync the swing from the attack packet
            mc.thePlayer.swingProgress = 0.0f;
            mc.thePlayer.swingProgressInt = 0;
            mc.thePlayer.isSwingInProgress = true;

            lastAttackTime = System.currentTimeMillis();
            ticksSinceLastAttack = 0;
        } catch (Exception e) {
            // Never crash from attack
        }
    }
}
