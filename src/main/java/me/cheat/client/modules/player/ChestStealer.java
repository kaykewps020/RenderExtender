package me.cheat.client.modules.player;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import me.cheat.client.utils.AntiDetection;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * ChestStealer - Automatically takes all items from chests.
 * With randomized delay to look human.
 */
public class ChestStealer extends Module {
    private final Setting delay = new Setting("Delay", 50, 0, 500, 10);
    private final Setting randomize = new Setting("Randomize", true);
    private final Setting onlyValuable = new Setting("Only Valuable", false);
    private final Setting closeAfter = new Setting("Close After", true);
    private final Setting autoOpen = new Setting("Auto Open", false);

    private long lastSteal = 0;
    private int currentSlot = 0;

    public ChestStealer() {
        super("ChestStealer", Category.PLAYER, 0);
        addSetting(delay);
        addSetting(randomize);
        addSetting(onlyValuable);
        addSetting(closeAfter);
        addSetting(autoOpen);
    }

    @Override
    protected void onEnable() {
        currentSlot = 0;
        lastSteal = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null) return;
        if (!(mc.currentScreen instanceof GuiChest)) return;

        GuiChest chest = (GuiChest) mc.currentScreen;
        Container container = chest.inventorySlots;

        if (!(container instanceof ContainerChest)) return;

        ContainerChest chestContainer = (ContainerChest) container;
        // Get lowerChestInventory via reflection (private field)
        int totalSlots = 27; // default single chest
        try {
            IInventory lowerInv = ReflectionHelper.getPrivateValue(
                ContainerChest.class, chestContainer, "lowerChestInventory", "field_75155_c"
            );
            totalSlots = lowerInv.getSizeInventory();
        } catch (Exception e) {
            // Fallback: assume 27 slots (single chest)
        }
        int numRows = totalSlots / 9;
        int totalSlots = numRows * 9;

        long now = System.currentTimeMillis();
        long effectiveDelay = delay.getInt();
        if (randomize.getBoolean()) {
            effectiveDelay += AntiDetection.getRandomizedDelay(-20, 40);
        }

        if (now - lastSteal < effectiveDelay) return;

        // Find next item to steal
        for (int i = currentSlot; i < totalSlots; i++) {
            Slot slot = chestContainer.getSlot(i);
            if (slot != null && slot.getHasStack()) {
                ItemStack stack = slot.getStack();
                if (stack != null) {
                    if (onlyValuable.getBoolean() && !isValuable(stack)) {
                        continue;
                    }
                    // Shift-click the item out
                    mc.playerController.windowClick(
                        chestContainer.windowId,
                        i, 0, 1, mc.thePlayer
                    );
                    lastSteal = now;
                    currentSlot = i + 1;
                    return;
                }
            }
        }

        // All items stolen
        currentSlot = 0;
        if (closeAfter.getBoolean()) {
            mc.thePlayer.closeScreen();
        }
    }

    private boolean isValuable(ItemStack stack) {
        if (stack == null) return false;
        // Keep valuable items: diamond, gold, iron, emerald, etc.
        String name = stack.getDisplayName().toLowerCase();
        return name.contains("diamond") || name.contains("gold") ||
               name.contains("iron") || name.contains("emerald") ||
               name.contains("netherite") || name.contains("enchanted") ||
               name.contains("sword") || name.contains("axe") ||
               name.contains("pickaxe") || name.contains("armor") ||
               stack.isItemEnchanted();
    }
}
