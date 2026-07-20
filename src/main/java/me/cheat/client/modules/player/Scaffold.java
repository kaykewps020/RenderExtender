package me.cheat.client.modules.player;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import me.cheat.client.utils.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Scaffold - Auto-bridge / auto-block placer.
 * 
 * LOGIC:
 * 1. Check if player is over air (needs bridging). If on solid ground, do nothing.
 * 2. Find the best air position to place a block in.
 * 3. Rotate to it, swap to a block item, place, swap back.
 * 4. Only act when there's actually something to place.
 */
public class Scaffold extends Module {

    private final Setting extend = new Setting("Extend", 4, 1, 6, 1);
    private final Setting tower = new Setting("Tower", false);
    private final Setting rotate = new Setting("Rotate", true);
    private final Setting swing = new Setting("Swing", true);

    // Internal state
    private int origSlot = -1;
    private boolean wasPlacing = false;

    public Scaffold() {
        super("Scaffold", Category.PLAYER, 0x2F);
        addSetting(extend);
        addSetting(tower);
        addSetting(rotate);
        addSetting(swing);
    }

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        origSlot = -1;
        wasPlacing = false;
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        // Restore original hotbar slot
        if (origSlot >= 0 && origSlot < 9 && mc.thePlayer != null && mc.getNetHandler() != null) {
            mc.thePlayer.inventory.currentItem = origSlot;
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(origSlot));
        }
        origSlot = -1;
        wasPlacing = false;
        // Release sneak
        if (mc.thePlayer != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.isDead) return;

        try {
            // Tower mode: jump when space held
            if (tower.getBoolean() && Keyboard.isKeyDown(Keyboard.KEY_SPACE) && mc.thePlayer.onGround) {
                mc.thePlayer.jump();
            }

            // STEP 1: Check if we need to place a block
            // We need bridging when the block 1 below AND the block 2 below our future position are air
            // Simple check: is the block directly below our feet AIR?
            BlockPos feetPos = mc.thePlayer.getPosition();
            BlockPos belowFeet = feetPos.down();

            // If below feet is solid and we're on ground, we don't need to scaffold
            if (isSolidBlock(belowFeet) && mc.thePlayer.onGround) {
                wasPlacing = false;
                return;
            }

            // STEP 2: Find the best position to place
            BlockPos placePos = findPlacePosition();
            if (placePos == null) {
                wasPlacing = false;
                return;
            }

            // STEP 3: Find a block item in hotbar
            int blockSlot = findBlockInHotbar();
            if (blockSlot == -1) {
                wasPlacing = false;
                return;
            }

            // STEP 4: Rotate to the block position
            if (rotate.getBoolean()) {
                float yaw = getLookAngleXZ(placePos);
                mc.thePlayer.rotationYaw = yaw;
                // Look down at ~75 degrees to place at feet
                mc.thePlayer.rotationPitch = 75.0f;
            }

            // STEP 5: Swap to block item and place
            int prevSlot = mc.thePlayer.inventory.currentItem;
            if (prevSlot != blockSlot) {
                mc.thePlayer.inventory.currentItem = blockSlot;
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(blockSlot));
            }

            // Place the block
            Vec3 hitVec = new Vec3(
                placePos.getX() + 0.5,
                placePos.getY() + 1.0,
                placePos.getZ() + 0.5
            );
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(
                placePos,
                1, // top face
                mc.thePlayer.getHeldItem(),
                0.5f, 1.0f, 0.5f
            ));

            if (swing.getBoolean()) {
                mc.thePlayer.swingItem();
            }

            // STEP 6: Swap back to original slot
            if (prevSlot != blockSlot) {
                mc.thePlayer.inventory.currentItem = prevSlot;
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(prevSlot));
            }

            wasPlacing = true;

            // Sneak when near edge to not fall off
            if (!mc.thePlayer.onGround) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
            } else {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            }

        } catch (Exception e) {
            // Never crash
        }
    }

    /**
     * Find the best air position to place a block.
     * Looks for air positions adjacent to the player's feet that have a solid neighbor.
     * Priority: directly below > forward > sides
     */
    private BlockPos findPlacePosition() {
        if (mc.thePlayer == null || mc.theWorld == null) return null;

        EnumFacing facing = mc.thePlayer.getHorizontalFacing();
        BlockPos feetPos = mc.thePlayer.getPosition();
        BlockPos belowFeet = feetPos.down();

        // Try positions in order of priority:
        // 1. Directly below feet (if air)
        if (isAirBlock(belowFeet) && hasSolidNeighbor(belowFeet)) {
            return belowFeet;
        }

        // 2. Forward from below feet
        int ext = Math.min(extend.getInt(), 4);
        for (int i = 1; i <= ext; i++) {
            BlockPos check = belowFeet.offset(facing, i);
            if (isAirBlock(check) && hasSolidNeighbor(check)) {
                return check;
            }
        }

        // 3. Sides
        for (int i = 0; i <= ext; i++) {
            EnumFacing left = facing.rotateY();
            EnumFacing right = facing.rotateYCCW();

            BlockPos leftPos = belowFeet.offset(left, i);
            BlockPos rightPos = belowFeet.offset(right, i);

            if (isAirBlock(leftPos) && hasSolidNeighbor(leftPos)) return leftPos;
            if (isAirBlock(rightPos) && hasSolidNeighbor(rightPos)) return rightPos;
        }

        return null;
    }

    /**
     * Check if block position has at least one solid adjacent block (to place against)
     */
    private boolean hasSolidNeighbor(BlockPos pos) {
        for (EnumFacing dir : EnumFacing.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (isSolidBlock(neighbor)) return true;
        }
        return false;
    }

    private boolean isAirBlock(BlockPos pos) {
        if (mc.theWorld == null) return false;
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block.isAir(mc.theWorld, pos);
    }

    private boolean isSolidBlock(BlockPos pos) {
        if (mc.theWorld == null) return false;
        IBlockState state = mc.theWorld.getBlockState(pos);
        Block block = state.getBlock();
        return !block.isAir(mc.theWorld, pos) && block.isFullBlock() && !(block instanceof BlockLiquid);
    }

    /**
     * Get horizontal look angle (yaw) to a block position
     */
    private float getLookAngleXZ(BlockPos pos) {
        double dx = pos.getX() + 0.5 - mc.thePlayer.posX;
        double dz = pos.getZ() + 0.5 - mc.thePlayer.posZ;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        return yaw;
    }

    /**
     * Find first slot in hotbar (0-8) with a block item
     */
    private int findBlockInHotbar() {
        if (mc.thePlayer == null) return -1;
        InventoryPlayer inv = mc.thePlayer.inventory;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                return i;
            }
        }
        return -1;
    }

    public int getBlocksPlaced() { return 0; }
}
