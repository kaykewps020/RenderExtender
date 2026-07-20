package me.cheat.client.modules.player;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import me.cheat.client.utils.AntiDetection;
import me.cheat.client.utils.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Random;

public class Scaffold extends Module {
    private final Random random = new Random();

    private final Setting extend = new Setting("Extend", 4, 1, 6, 1);
    private final Setting tower = new Setting("Tower Mode", false);
    private final Setting swing = new Setting("Swing", true);
    private final Setting rotate = new Setting("Rotate", true);
    private final Setting ninjaBridge = new Setting("Ninja Bridge", true);
    private final Setting keepY = new Setting("Keep Y Level", true);
    private final Setting speed = new Setting("Speed", 1.0, 0.5, 3.0, 0.1);

    private BlockPos targetBlock = null;
    private EnumFacing targetFacing = EnumFacing.UP;
    private int slot = -1;
    private float[] rotations = null;
    private int blocksPlaced = 0;
    private boolean wasSneaking = false;
    private boolean wasSprinting = false;

    public Scaffold() {
        super("Scaffold", Category.PLAYER, 0x2F); // V key
        addSetting(extend);
        addSetting(tower);
        addSetting(swing);
        addSetting(rotate);
        addSetting(ninjaBridge);
        addSetting(keepY);
        addSetting(speed);
    }

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        targetBlock = null;
        slot = -1;
        rotations = null;
        blocksPlaced = 0;
        wasSneaking = false;
        wasSprinting = false;
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        if (mc.thePlayer != null) {
            mc.gameSettings.keyBindSneak.pressed = false;
            mc.gameSettings.keyBindSprint.pressed = false;
        }
        targetBlock = null;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player != mc.thePlayer) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.isDead) return;

        targetBlock = findTargetBlock();
        if (targetBlock == null) {
            targetBlock = findExtendedBlock();
            if (targetBlock == null) return;
        }

        slot = findBlockSlot();
        if (slot == -1) return;

        BlockPos placePos = targetBlock.up();
        if (!canPlaceBlock(placePos)) return;

        if (rotate.getBoolean()) {
            float[] rots = RotationUtils.getRotationsToBlock(targetBlock);
            if (rots != null) {
                rotations = rots;
                mc.thePlayer.rotationYaw = rots[0];
                mc.thePlayer.rotationPitch = rots[1];
            }
        }

        if (ninjaBridge.getBoolean()) {
            handleNinjaBridge();
        }

        if (tower.getBoolean() && Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
            handleTower();
            return;
        }

        placeBlock(placePos);
    }

    private BlockPos findTargetBlock() {
        if (mc.thePlayer == null) return null;
        BlockPos playerPos = mc.thePlayer.getPosition();
        EnumFacing facing = mc.thePlayer.getHorizontalFacing();

        BlockPos below = playerPos.down();
        if (canPlaceOn(below)) {
            targetFacing = EnumFacing.UP;
            return below;
        }

        int range = Math.max(3, extend.getInt());
        for (int i = 1; i <= range; i++) {
            BlockPos check = playerPos.down().offset(facing, i);
            if (canPlaceOn(check)) {
                targetFacing = EnumFacing.UP;
                return check;
            }
        }
        return null;
    }

    private BlockPos findExtendedBlock() {
        if (mc.thePlayer == null) return null;
        BlockPos playerPos = mc.thePlayer.getPosition();
        EnumFacing facing = mc.thePlayer.getHorizontalFacing();
        int range = extend.getInt() + 1;

        for (int i = 1; i <= range; i++) {
            for (int j = -1; j <= 1; j++) {
                EnumFacing side = j == -1 ? facing.rotateY() :
                    j == 1 ? facing.rotateYCCW() : facing;
                BlockPos check = playerPos.down().offset(side, i);
                if (canPlaceOn(check)) {
                    targetFacing = EnumFacing.UP;
                    return check;
                }
            }
        }
        return null;
    }

    private boolean canPlaceOn(BlockPos pos) {
        if (mc.theWorld == null) return false;
        IBlockState state = mc.theWorld.getBlockState(pos);
        Block block = state.getBlock();
        return !block.isAir(mc.theWorld, pos) && block.isFullBlock() &&
               !(block instanceof BlockLiquid) && !(block instanceof BlockFire);
    }

    private boolean canPlaceBlock(BlockPos pos) {
        if (mc.theWorld == null) return false;
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block.isAir(mc.theWorld, pos) || block.isReplaceable(mc.theWorld, pos);
    }

    private int findBlockSlot() {
        if (mc.thePlayer == null) return -1;
        InventoryPlayer inv = mc.thePlayer.inventory;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (isBlockItem(stack)) return i;
        }
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (isBlockItem(stack)) return i;
        }
        return -1;
    }

    private boolean isBlockItem(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();
        return item instanceof ItemBlock && item != null;
    }

    private void handleNinjaBridge() {
        if (mc.thePlayer == null) return;
        boolean atEdge = isAtEdge();

        if (atEdge) {
            mc.gameSettings.keyBindSneak.pressed = true;
            mc.thePlayer.setSprinting(true);
            wasSneaking = true;
        } else {
            if (wasSneaking) {
                mc.gameSettings.keyBindSneak.pressed = false;
                wasSneaking = false;
            }
        }
    }

    private boolean isAtEdge() {
        if (mc.thePlayer == null) return false;
        double xOffset = Math.abs(mc.thePlayer.posX - mc.thePlayer.getPosition().getX() - 0.5);
        double zOffset = Math.abs(mc.thePlayer.posZ - mc.thePlayer.getPosition().getZ() - 0.5);
        return xOffset > 0.3 || zOffset > 0.3;
    }

    private void handleTower() {
        if (mc.thePlayer == null) return;
        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump();
        }
        BlockPos below = mc.thePlayer.getPosition().down();
        if (canPlaceBlock(below)) {
            placeBlock(below);
        }
    }

    private void placeBlock(BlockPos pos) {
        if (mc.thePlayer == null || mc.getNetHandler() == null) return;

        int currentSlot = mc.thePlayer.inventory.currentItem;

        if (slot >= 0 && slot < 9 && slot != currentSlot) {
            mc.thePlayer.inventory.currentItem = slot;
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(slot));
        } else if (slot >= 9) {
            mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId,
                slot, currentSlot, 2, mc.thePlayer);
        }

        Vec3 hitVec = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        EnumFacing placeFace = getPlacementFace(pos);

        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(
            pos, placeFace.getIndex(),
            mc.thePlayer.getHeldItem(),
            (float)(hitVec.xCoord - pos.getX()),
            (float)(hitVec.yCoord - pos.getY()),
            (float)(hitVec.zCoord - pos.getZ())
        ));

        if (swing.getBoolean()) {
            mc.thePlayer.swingItem();
        }

        blocksPlaced++;

        if (slot >= 0 && slot < 9 && slot != currentSlot) {
            mc.thePlayer.inventory.currentItem = currentSlot;
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(currentSlot));
        }
    }

    private EnumFacing getPlacementFace(BlockPos pos) {
        Vec3 playerPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        Vec3 blockCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3 diff = blockCenter.subtract(playerPos);

        EnumFacing bestFace = EnumFacing.UP;
        double bestDist = Double.MAX_VALUE;

        for (EnumFacing dir : EnumFacing.values()) {
            Vec3 facePos = blockCenter.addVector(
                dir.getFrontOffsetX() * 0.5,
                dir.getFrontOffsetY() * 0.5,
                dir.getFrontOffsetZ() * 0.5
            );
            double dist = facePos.distanceTo(playerPos);
            if (dist < bestDist) {
                bestDist = dist;
                bestFace = dir;
            }
        }
        return bestFace.getOpposite();
    }

    public boolean isNinjaBridging() { return isEnabled() && ninjaBridge.getBoolean(); }
    public int getBlocksPlaced() { return blocksPlaced; }
}
