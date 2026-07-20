package me.cheat.client.utils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class StringObfuscator {
    private static final Map<String, String> cache = new HashMap<>();
    private static final Random random = new Random();

    private static final byte[][] XOR_KEYS = {
        {0x4A, 0x6F, 0x3C, 0x2B, 0x5E, 0x1D, 0x7A, 0x4F},
        {0x3B, 0x2C, 0x5F, 0x1E, 0x7B, 0x4E, 0x6A, 0x3D},
        {0x1C, 0x7E, 0x4B, 0x6F, 0x3A, 0x2D, 0x5C, 0x1F},
        {0x5D, 0x3F, 0x2A, 0x4C, 0x1B, 0x7F, 0x3E, 0x6D},
        {0x2E, 0x4A, 0x6C, 0x1A, 0x7D, 0x3B, 0x5F, 0x2B}
    };

    public static String decrypt(String encrypted) {
        if (cache.containsKey(encrypted)) {
            return cache.get(encrypted);
        }
        try {
            byte[] data = Base64.getDecoder().decode(encrypted);
            byte[] key = XOR_KEYS[random.nextInt(XOR_KEYS.length)];
            for (int i = 0; i < data.length; i++) {
                data[i] ^= key[i % key.length];
                data[i] = (byte) ~data[i];
                data[i] ^= 0x55;
            }
            for (int i = 0; i < data.length / 2; i++) {
                byte tmp = data[i];
                data[i] = data[data.length - 1 - i];
                data[data.length - 1 - i] = tmp;
            }
            for (int i = 0; i < data.length; i++) {
                data[i] ^= (byte) (i * 7 + 0x33);
            }
            String result = new String(data, "UTF-8");
            cache.put(encrypted, result);
            return result;
        } catch (Exception e) {
            return encrypted;
        }
    }

    public static String s(String encrypted) {
        return decrypt(encrypted);
    }

    public static String obf(String plaintext) {
        return "s(\"" + encrypt(plaintext) + "\")";
    }

    public static String encrypt(String plaintext) {
        try {
            byte[] data = plaintext.getBytes("UTF-8");
            for (int i = 0; i < data.length; i++) {
                data[i] ^= (byte) (i * 7 + 0x33);
            }
            for (int i = 0; i < data.length / 2; i++) {
                byte tmp = data[i];
                data[i] = data[data.length - 1 - i];
                data[data.length - 1 - i] = tmp;
            }
            byte[] key = XOR_KEYS[0];
            for (int i = 0; i < data.length; i++) {
                data[i] ^= 0x55;
                data[i] = (byte) ~data[i];
                data[i] ^= key[i % key.length];
            }
            return Base64.getEncoder().encodeToString(data);
        } catch (Exception e) {
            return plaintext;
        }
    }
}
