package me.cheat.client.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class ConfigManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    public static void init() {
        configPath = mc.mcDataDir.toPath().resolve("config").resolve("renderextender.json");
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException ignored) {}
    }

    public static void save(String key, String value) {
        try {
            JsonObject config = loadJson();
            config.addProperty(key, value);
            Files.write(configPath, gson.toJson(config).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {}
    }

    public static void save(String key, boolean value) {
        save(key, String.valueOf(value));
    }

    public static void save(String key, int value) {
        save(key, String.valueOf(value));
    }

    public static void save(String key, double value) {
        save(key, String.valueOf(value));
    }

    public static String load(String key, String defaultValue) {
        try {
            JsonObject config = loadJson();
            if (config.has(key)) {
                return config.get(key).getAsString();
            }
        } catch (Exception ignored) {}
        return defaultValue;
    }

    public static boolean load(String key, boolean defaultValue) {
        return Boolean.parseBoolean(load(key, String.valueOf(defaultValue)));
    }

    public static int load(String key, int defaultValue) {
        try {
            return Integer.parseInt(load(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static double load(String key, double defaultValue) {
        try {
            return Double.parseDouble(load(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static JsonObject loadJson() {
        try {
            if (Files.exists(configPath)) {
                String content = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
                return new JsonParser().parse(content).getAsJsonObject();
            }
        } catch (Exception ignored) {}
        return new JsonObject();
    }

    public static void saveEncrypted(String key, String value) {
        String encrypted = Base64.getEncoder().encodeToString(
            xorEncrypt(value.getBytes(StandardCharsets.UTF_8))
        );
        save(key, encrypted);
    }

    public static String loadEncrypted(String key, String defaultValue) {
        String encrypted = load(key, "");
        if (encrypted.isEmpty()) return defaultValue;
        try {
            byte[] decrypted = xorDecrypt(Base64.getDecoder().decode(encrypted));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static byte[] xorEncrypt(byte[] data) {
        byte[] key = {0x4C, 0x6F, 0x3A, 0x2E, 0x5B, 0x1C, 0x7D, 0x4A};
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }

    private static byte[] xorDecrypt(byte[] data) {
        return xorEncrypt(data);
    }
}
