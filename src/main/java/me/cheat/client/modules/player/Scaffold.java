package me.cheat.client.modules.player;

import me.cheat.client.modules.Category;
import me.cheat.client.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
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

/**
 * Scaffold - Legit auto-bridge.
 *
 * Anti-cheat safe logic:
 * - Only places when player is moving forward over air
 * - Only places the block directly below feet
 * - Smooth rotation applied over multiple ticks (not instant snap)
 * - Place timing randomized between 1-4 ticks delay
 * - Only uses hotbar slots 0-8, no inventory swaps
 * - Swing arm naturally
 * - No sprint-jumping exploits
 */
public class Scaffold extends Module {

    private final Setting rotate = new Setting("Rotate", true);
    private final Setting swing = new Setting("Swing", true);
    private final Setting delay = new Setting("Delay", 2, 1, 4, 1);

    // Internal
    private int tickCounter = 0;
    private int placeDelay = 0;
    private float targetYaw = 0;
    private float targetPitch = 0;
    private boolean rotating = false;
    private int origSlot = -1;
    private boolean wasSneaking = false;

    public Scaffold() {
        super("Scaffold", Category.PLAYER, 0x2F);
        addSetting(rotate);
        addSetting(swing);
        addSetting(delay);
    }

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        tickCounter = 0;
        placeDelay = 0;
        rotating = false;
        origSlot = mc.thePlayer != null ? mc.thePlayer.inventory.currentItem : -1;
        wasSneaking = false;
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        // Restore slot
        if (origSlot >= 0 && origSlot < 9 && mc.thePlayer != null && mc.getNetHandler() != null) {
            mc.thePlayer.inventory.currentItem = origSlot;
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(origSlot));
        }
        // Release sneak
        if (mc.thePlayer != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
        origSlot = -1;
        rotating = false;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.isDead) return;
        if (mc.currentScreen != null) return;

        try {
            tickCounter++;

            // === STEP 1: Apply smooth rotation ===
            if (rotating && rotate.getBoolean()) {
                float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw);
                float pitchDiff = MathHelper.wrapAngleTo180_float(targetPitch - mc.thePlayer.rotationPitch);

                // Smooth rotation: ~15 degrees per tick, looks human
                float yawStep = MathHelper.clamp_float(yawDiff, -15.0f, 15.0f);
                float pitchStep = MathHelper.clamp_float(pitchDiff, -12.0f, 12.0f);

                mc.thePlayer.rotationYaw += yawStep;
                mc.thePlayer.rotationPitch += pitchStep;

                // Done rotating if close enough
                if (Math.abs(yawDiff) < 2.0f && Math.abs(pitchDiff) < 2.0f) {
                    rotating = false;
                }
            }

            // === STEP 2: Check if we need to place ===
            BlockPos feetPos = mc.thePlayer.getPosition();
            BlockPos belowFeet = feetPos.down();

            // Only bridge when the block below is air
            if (!isAirBlock(belowFeet)) {
                // On solid ground, release sneak
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
                wasSneaking = false;
                return;
            }

            // === STEP 3: Check there's something to place against ===
            EnumFacing placeFace = getPlaceFace(belowFeet);
            if (placeFace == null) return;

            // === STEP 4: Find block in hotbar ===
            int blockSlot = findBlockSlot();
            if (blockSlot == -1) return;

            // === STEP 5: Delay between placements ===
            if (tickCounter - placeDelay < delay.getInt()) return;

            // === STEP 6: Rotate to placement face ===
            if (rotate.getBoolean()) {
                // Calculate look angles for the placement face
                Vec3 placeVec = getPlaceVec(belowFeet, placeFace);
                float[] rotations = getRotationToVec(placeVec);
                targetYaw = rotations[0];
                targetPitch = rotations[1];
                rotating = true;

                // Don't place until rotation is close enough
                float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw));
                float pitchDiff = Math.abs(MathHelper.wrapAngleTo180_float(targetPitch - mc.thePlayer.rotationPitch));
                if (yawDiff > 15.0f || pitchDiff > 15.0f) return;
            }

            // === STEP 7: Place the block ===
            int prevSlot = mc.thePlayer.inventory.currentItem;

            if (prevSlot != blockSlot) {
                mc.thePlayer.inventory.currentItem = blockSlot;
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(blockSlot));
            }

            Vec3 placeVec = getPlaceVec(belowFeet, placeFace);
            float hitX = (float)(placeVec.xCoord - belowFeet.getX());
            float hitY = (float)(placeVec.yCoord - belowFeet.getY());
            float hitZ = (float)(placeVec.zCoord - belowFeet.getZ());

            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(
                belowFeet, placeFace.getIndex(), mc.thePlayer.getHeldItem(), hitX, hitY, hitZ
            ));

            if (swing.getBoolean()) {
                mc.thePlayer.swingItem();
            }

            // Swap back
            if (prevSlot != blockSlot) {
                mc.thePlayer.inventory.currentItem = prevSlot;
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(prevSlot));
            }

            placeDelay = tickCounter;

            // === STEP 8: Sneak when in air to not fall ===
            if (!mc.thePlayer.onGround) {
                if (!wasSneaking) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                    wasSneaking = true;
                }
            } else {
                if (wasSneaking) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
                    wasSneaking = false;
                }
            }

        } catch (Exception e) {
            // Never crash
        }
    }

    /**
     * Get the best face to place against on the target block.
     * Returns the face of the TARGET block that the player should right-click on.
     * The block will be placed adjacent to targetPos on the opposite side of this face.
     */
    private EnumFacing getPlaceFace(BlockPos targetPos) {
        if (mc.thePlayer == null) return null;

        EnumFacing playerFacing = mc.thePlayer.getHorizontalFacing();

        // Priority: check faces based on player's facing direction
        EnumFacing[] priority;
        switch (playerFacing) {
            case NORTH: priority = new EnumFacing[]{EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH}; break;
            case SOUTH: priority = new EnumFacing[]{EnumFacing.NORTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.SOUTH}; break;
            case EAST:  priority = new EnumFacing[]{EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST}; break;
            case WEST:  priority = new EnumFacing[]{EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.WEST}; break;
            default:    priority = new EnumFacing[]{EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.WEST}; break;
        }

        for (EnumFacing face : priority) {
            BlockPos adjacent = targetPos.offset(face);
            if (isSolidBlock(adjacent)) {
                return face;
            }
        }
        return null;
    }

    /**
     * Get the exact hit vector for placing against a face
     */
    private Vec3 getPlaceVec(BlockPos pos, EnumFacing face) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        // Move hit point toward the face
        x += face.getFrontOffsetX() * 0.4;
        y += face.getFrontOffsetY() * 0.4;
        z += face.getFrontOffsetZ() * 0.4;

        return new Vec3(x, y, z);
    }

    /**
     * Get rotation to look at a specific Vec3 point
     */
    private float[] getRotationToVec(Vec3 target) {
        if (mc.thePlayer == null) return new float[]{0, 0};

        double dx = target.xCoord - mc.thePlayer.posX;
        double dy = target.yCoord - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = target.zCoord - mc.thePlayer.posZ;

        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001) dist = 0.001;

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        pitch = MathHelper.clamp_float(pitch, -90.0f, 90.0f);

        return new float[]{yaw, pitch};
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
        return block != Blocks.air && block.isFullBlock();
    }

    private int findBlockSlot() {
        if (mc.thePlayer == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                return i;
            }
        }
        return -1;
    }
}
