package me.cheat.client.utils;

import net.minecraft.client.Minecraft;

import java.util.Random;

public class AntiDetection {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();

    private static long lastAction = 0;
    private static int cpsCounter = 0;
    private static long lastCpsReset = 0;
    private static int actionsPerTick = 0;

    public static long getRandomizedDelay(int minMs, int maxMs) {
        return minMs + (long) (random.nextDouble() * (maxMs - minMs));
    }

    public static double addJitter(double value, double range) {
        return value + (random.nextDouble() - 0.5) * 2 * range;
    }

    public static boolean checkCPS(int maxCps) {
        long now = System.currentTimeMillis();
        if (now - lastCpsReset > 1000) {
            cpsCounter = 0;
            lastCpsReset = now;
        }
        cpsCounter++;

        int variance = (int) (maxCps * 0.15);
        int effectiveMax = maxCps + random.nextInt(variance * 2) - variance;

        return cpsCounter <= effectiveMax;
    }

    public static boolean checkActionsPerTick(int maxActions) {
        actionsPerTick++;
        return actionsPerTick <= maxActions;
    }

    public static void resetActionsPerTick() {
        actionsPerTick = 0;
    }

    public static float addRotationNoise(float value, float maxNoise) {
        if (random.nextDouble() > 0.7) return value;
        return value + (float) (random.nextGaussian() * maxNoise);
    }

    public static boolean shouldSpoofPosition() {
        return random.nextDouble() < 0.03;
    }

    public static boolean shouldSendPacket() {
        long now = System.currentTimeMillis();
        if (now - lastAction < 50) {
            return random.nextDouble() < 0.5;
        }
        lastAction = now;
        return true;
    }

    public static boolean hasAntiCheat() {
        if (mc.thePlayer == null) return false;
        String brand = mc.thePlayer.getClientBrand();
        if (brand != null) {
            String lower = brand.toLowerCase();
            return lower.contains("nocheat") || lower.contains("anticheat") ||
                   lower.contains("grim") || lower.contains("intave") ||
                   lower.contains("spartan") || lower.contains("matrix") ||
                   lower.contains("vulcan") || lower.contains("negativity") ||
                   lower.contains("wizard") || lower.contains("karhu");
        }
        return false;
    }
}
