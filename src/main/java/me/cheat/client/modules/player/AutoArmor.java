package me.cheat.client.modules.player;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import me.cheat.client.utils.AntiDetection;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

/**
 * AutoArmor - Automatically equips the best armor from inventory.
 */
public class AutoArmor extends Module {
    private final Setting delay = new Setting("Delay", 100, 0, 500, 10);
    private final Setting randomize = new Setting("Randomize", true);

    private long lastSwap = 0;

    public AutoArmor() {
        super("AutoArmor", Category.PLAYER, 0);
        addSetting(delay);
        addSetting(randomize);
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof GuiInventory)) return;

        long now = System.currentTimeMillis();
        long effectiveDelay = delay.getLong();
        if (randomize.getBoolean()) {
            effectiveDelay += AntiDetection.getRandomizedDelay(-10, 20);
        }
        if (now - lastSwap < effectiveDelay) return;

        int bestSlot = -1;
        int bestArmorType = -1;
        int bestProtection = -1;

        // Scan inventory for better armor
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack == null || !(stack.getItem() instanceof ItemArmor)) continue;

            ItemArmor armor = (ItemArmor) stack.getItem();
            int armorType = armor.armorType; // 0=helmet, 1=chest, 2=legs, 3=boots

            // Check if this slot is empty or has worse armor
            ItemStack equipped = mc.thePlayer.inventory.armorInventory[3 - armorType];
            if (equipped != null) {
                ItemArmor equippedArmor = (ItemArmor) equipped.getItem();
                if (equippedArmor.damageReduceAmount >= armor.damageReduceAmount) {
                    continue; // Current armor is better
                }
            }

            if (armor.damageReduceAmount > bestProtection) {
                bestProtection = armor.damageReduceAmount;
                bestSlot = i;
                bestArmorType = armorType;
            }
        }

        if (bestSlot != -1) {
            // Shift-click to equip
            mc.playerController.windowClick(
                mc.thePlayer.inventoryContainer.windowId,
                bestSlot, 0, 1, mc.thePlayer
            );
            lastSwap = now;
        }
    }
}
