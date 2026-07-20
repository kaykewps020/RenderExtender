package me.cheat.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.Random;

public class RotationUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();

    private static float serverYaw;
    private static float serverPitch;

    public static float[] getRotationsToEntity(Entity entity) {
        if (entity == null) return new float[]{0, 0};
        return getRotationsToPos(
            entity.posX,
            entity.posY + entity.getEyeHeight() / 2.0f,
            entity.posZ
        );
    }

    public static float[] getRotationsToPos(double x, double y, double z) {
        double dx = x - mc.thePlayer.posX;
        double dy = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = z - mc.thePlayer.posZ;

        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        return new float[]{
            mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw),
            mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)
        };
    }

    public static float[] getRotationsToBlock(BlockPos pos) {
        double dx = pos.getX() + 0.5 - mc.thePlayer.posX;
        double dy = pos.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = pos.getZ() + 0.5 - mc.thePlayer.posZ;

        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        return new float[]{
            mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw),
            mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)
        };
    }

    public static void smoothRotations(float[] targetRotations, float speed) {
        if (mc.thePlayer == null) return;

        float yawDelta = MathHelper.wrapAngleTo180_float(targetRotations[0] - mc.thePlayer.rotationYaw);
        float pitchDelta = MathHelper.wrapAngleTo180_float(targetRotations[1] - mc.thePlayer.rotationPitch);

        float yawStep = MathHelper.clamp_float(yawDelta, -speed, speed);
        float pitchStep = MathHelper.clamp_float(pitchDelta, -speed, speed);

        mc.thePlayer.rotationYaw += yawStep;
        mc.thePlayer.rotationPitch += pitchStep;
    }

    public static float[] applyRandomization(float yaw, float pitch, float range) {
        return new float[]{
            yaw + (float)(random.nextGaussian() * range),
            pitch + (float)(random.nextGaussian() * range * 0.5)
        };
    }

    public static float[] applyGCD(float yaw, float pitch) {
        float deltaYaw = yaw - mc.thePlayer.rotationYaw;
        float deltaPitch = pitch - mc.thePlayer.rotationPitch;

        float gcd = getGCD();

        float fixedYaw = deltaYaw - (deltaYaw % gcd);
        float fixedPitch = deltaPitch - (deltaPitch % gcd);

        return new float[]{
            mc.thePlayer.rotationYaw + fixedYaw,
            mc.thePlayer.rotationPitch + fixedPitch
        };
    }

    private static float getGCD() {
        float sensitivity = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float mouseDelta = sensitivity * sensitivity * sensitivity * 8.0F * 0.15F;
        return mouseDelta * 0.5F;
    }

    public static void setServerRotations(float yaw, float pitch) {
        serverYaw = yaw;
        serverPitch = pitch;
    }

    public static float getServerYaw() { return serverYaw; }
    public static float getServerPitch() { return serverPitch; }

    public static Vec3 getVectorForRotation(float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        double xz = -MathHelper.cos(pitchRad);
        double x = xz * MathHelper.sin(yawRad);
        double y = MathHelper.sin(pitchRad);
        double z = xz * MathHelper.cos(yawRad);

        return new Vec3(x, y, z);
    }
}
