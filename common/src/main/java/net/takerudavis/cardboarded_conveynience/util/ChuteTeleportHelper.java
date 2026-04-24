package net.takerudavis.cardboarded_conveynience.util;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.chute.AbstractChuteBlock;
import com.simibubi.create.content.logistics.chute.ChuteBlock;
import com.simibubi.create.content.logistics.chute.SmartChuteBlock;
import com.simibubi.create.content.logistics.chute.SmartChuteBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;
import net.takerudavis.cardboarded_conveynience.advancement.ModCriteria;
import net.takerudavis.cardboarded_conveynience.config.ModConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles instant teleportation through chutes for players in cardboard armor.
 * Scans chute paths (including diagonal routing) and teleports players to valid exits.
 */
public class ChuteTeleportHelper {

    // Cooldown to prevent spam (ticks)
    private static final int TELEPORT_COOLDOWN = 20; // 1 second
    private static final int MESSAGE_COOLDOWN = 40; // 2 seconds for warning messages

    // Track cooldowns per player
    private static final Map<UUID, Long> lastTeleportTick = new HashMap<>();
    private static final Map<UUID, Long> lastMessageTick = new HashMap<>();

    // Result of path scanning
    private enum PathResult {
        SUCCESS,
        TOO_LONG,
        FILTERED,
        POWERED,
        BLOCKED_EXIT
    }

    // Package height - used to calculate landing position below chute
    private static final double PACKAGE_HEIGHT = 0.6;

    private record ScanResult(PathResult result, BlockPos lastChute) {
        static ScanResult success(BlockPos lastChute) { return new ScanResult(PathResult.SUCCESS, lastChute); }
        static ScanResult tooLong() { return new ScanResult(PathResult.TOO_LONG, null); }
        static ScanResult filtered() { return new ScanResult(PathResult.FILTERED, null); }
        static ScanResult powered() { return new ScanResult(PathResult.POWERED, null); }
        static ScanResult blocked() { return new ScanResult(PathResult.BLOCKED_EXIT, null); }
    }

    /**
     * Check if a player should be teleported through a chute and do it.
     * Call this from server-side player tick.
     */
    public static void tickPlayer(Player player) {
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Must be disguised as a package (holding sneak + full cardboard armor)
        if (!CardboardHelper.testForArmor(player)) return;
        if (!player.isShiftKeyDown()) return;

        // Check cooldown
        UUID uuid = player.getUUID();
        long currentTick = player.level().getGameTime();
        Long lastTick = lastTeleportTick.get(uuid);
        if (lastTick != null && currentTick - lastTick < TELEPORT_COOLDOWN) return;

        // Find the chute the player is standing on
        BlockPos chutePos = findChuteUnderPlayer(player);
        if (chutePos == null) return;

        // Scan the chute path and validate exit
        ScanResult result = scanAndValidateChutePath(player.level(), chutePos);

        switch (result.result()) {
            case SUCCESS -> {
                // Teleport!
                performTeleport(serverPlayer, chutePos, result.lastChute());
                lastTeleportTick.put(uuid, currentTick);
            }
            case TOO_LONG -> showWarning(serverPlayer, "Chute path too long!", currentTick);
            case FILTERED -> showWarning(serverPlayer, "Blocked by smart chute filter", currentTick);
            case POWERED -> showWarning(serverPlayer, "Smart chute is closed!", currentTick);
            case BLOCKED_EXIT -> showWarning(serverPlayer, "Chute exit is blocked!", currentTick);
        }
    }

    /**
     * Check if a block is a chute.
     */
    private static boolean isChute(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof AbstractChuteBlock;
    }

    /**
     * Check if a chute has a top opening that a player can enter.
     * - facing=down: items come from above, so there's a top opening
     * - shape=intersection: multiple chutes joining, always has top opening
     * - SmartChuteBlock: always faces down, always has top opening
     */
    private static boolean hasChuteTopOpening(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AbstractChuteBlock)) {
            return false;
        }

        // Smart chutes always face down, always have top opening
        if (state.getBlock() instanceof SmartChuteBlock) {
            return true;
        }

        // Facing down = items come from above = top opening exists
        Direction facing = AbstractChuteBlock.getChuteFacing(state);
        if (facing == Direction.DOWN) {
            return true;
        }

        // Intersection shape always has top opening regardless of facing
        if (state.hasProperty(ChuteBlock.SHAPE)) {
            return state.getValue(ChuteBlock.SHAPE) == ChuteBlock.Shape.INTERSECTION;
        }

        return false;
    }

    /**
     * Find a chute directly under the player, with strict height check.
     * Mirrors Create's AbstractChuteBlock.updateEntityAfterFallOn logic:
     * BlockPos.containing(position.add(0, 0.5f, 0)).below()
     *
     * This means anything at or above 0.5 blocks gets the wrong block position.
     * - Carpets (1/16 = 0.0625): allowed
     * - Trapdoors (3/16 = 0.1875): allowed
     * - Slabs (1/2 = 0.5): rejected (exactly at threshold)
     *
     * @return The chute BlockPos, or null if player isn't properly positioned
     */
    private static BlockPos findChuteUnderPlayer(Player player) {
        Level level = player.level();
        double playerFeetY = player.getY();

        // Check the block at player's feet position
        BlockPos feetPos = player.blockPosition();

        // First check: is the block below the feet position a chute with a top opening?
        BlockPos potentialChute = feetPos.below();
        if (hasChuteTopOpening(level, potentialChute)) {
            // Chute top surface is at chuteY + 1
            double chuteTopY = potentialChute.getY() + 1.0;
            double heightAbove = playerFeetY - chuteTopY;

            // Create uses position.add(0, 0.5f, 0) - anything >= 0.5 above chute fails
            if (heightAbove >= 0 && heightAbove < 0.5) {
                return potentialChute;
            }
        }

        // Second check: player might be slightly inside a chute with top opening (falling)
        if (hasChuteTopOpening(level, feetPos)) {
            return feetPos;
        }

        return null;
    }

    /**
     * Show a warning message to the player via action bar, with cooldown to prevent spam.
     */
    private static void showWarning(ServerPlayer player, String message, long currentTick) {
        // Skip if player has no connection (e.g., in game tests)
        if (player.connection == null) return;

        UUID uuid = player.getUUID();
        Long lastMessage = lastMessageTick.get(uuid);
        if (lastMessage != null && currentTick - lastMessage < MESSAGE_COOLDOWN) {
            return; // Don't spam messages
        }
        lastMessageTick.put(uuid, currentTick);
        player.displayClientMessage(Component.literal(message), true);
    }

    /**
     * Scan from the starting chute, following the path items would take.
     * FACING points where items come FROM, so we go the opposite direction.
     * Returns a ScanResult with the last chute position or reason for failure.
     */
    private static ScanResult scanAndValidateChutePath(Level level, BlockPos start) {
        BlockPos current = start;
        BlockPos lastChute = start;
        int distance = 0;

        while (distance < ModConfig.get().maxChutePathLength) {
            BlockState state = level.getBlockState(current);

            if (!(state.getBlock() instanceof AbstractChuteBlock)) {
                // Hit non-chute - validate there's room to exit at landing position
                if (isValidExit(level, lastChute)) {
                    return ScanResult.success(lastChute);
                } else {
                    return ScanResult.blocked();
                }
            }

            // This is a chute - track it as the last one
            lastChute = current;

            if (state.getBlock() instanceof SmartChuteBlock
                && state.hasProperty(BlockStateProperties.POWERED)
                && state.getValue(BlockStateProperties.POWERED)) {
                return ScanResult.powered();
            }

            // Check smart chute filter - player is a "package", so only allow
            // if filter is empty or filter is set to a package
            if (!passesSmartChuteFilter(level, current)) {
                return ScanResult.filtered();
            }

            // Get facing direction (works for both ChuteBlock and SmartChuteBlock)
            // SmartChuteBlock always returns DOWN
            Direction facing = AbstractChuteBlock.getChuteFacing(state);

            // Calculate next position - items always go down, but diagonal chutes
            // have FACING pointing where items come FROM, so we go opposite
            BlockPos nextPos = current.below();
            if (facing.getAxis().isHorizontal()) {
                nextPos = nextPos.relative(facing.getOpposite());
            }

            current = nextPos;
            distance++;
        }

        // Path too long
        return ScanResult.tooLong();
    }

    /**
     * Check if a smart chute's filter allows a "package" (player) to pass.
     * Regular chutes always pass. Smart chutes pass if filter is empty or set to package.
     */
    private static boolean passesSmartChuteFilter(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof SmartChuteBlockEntity)) {
            // Not a smart chute - always passes
            return true;
        }

        // Get the filtering behaviour
        FilteringBehaviour filtering = BlockEntityBehaviour.get(level, pos, FilteringBehaviour.TYPE);
        if (filtering == null) {
            return true; // No filter behaviour - passes
        }

        ItemStack filter = filtering.getFilter();
        if (filter.isEmpty()) {
            return true; // Empty filter - passes
        }

        // Filter is set - only pass if it's a package
        return PackageItem.isPackage(filter);
    }

    /**
     * Check if there's room for a player to emerge below the last chute.
     * Creates a player bounding box at the landing position and checks for collisions.
     */
    private static boolean isValidExit(Level level, BlockPos lastChute) {
        // Calculate landing position (same as performTeleport)
        double landingY = lastChute.getY() - PACKAGE_HEIGHT;
        double centerX = lastChute.getX() + 0.5;
        double centerZ = lastChute.getZ() + 0.5;

        // Crouching player dimensions
        double playerWidth = 0.6;
        double playerHeight = 1.5;
        double halfWidth = playerWidth / 2.0;

        // Create player bounding box at landing position
        AABB playerBox = new AABB(
                centerX - halfWidth, landingY, centerZ - halfWidth,
                centerX + halfWidth, landingY + playerHeight, centerZ + halfWidth
        );

        // Check all blocks the player box might intersect
        int minX = (int) Math.floor(playerBox.minX);
        int minY = (int) Math.floor(playerBox.minY);
        int minZ = (int) Math.floor(playerBox.minZ);
        int maxX = (int) Math.floor(playerBox.maxX);
        int maxY = (int) Math.floor(playerBox.maxY);
        int maxZ = (int) Math.floor(playerBox.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    // Skip air
                    if (state.isAir()) continue;

                    // Skip chutes - player can overlap with the exit chute
                    if (state.getBlock() instanceof AbstractChuteBlock) continue;

                    VoxelShape collisionShape = state.getCollisionShape(level, checkPos);
                    if (collisionShape.isEmpty()) continue;

                    // Check if collision shape intersects player box
                    // Move collision bounds to world coordinates
                    AABB collisionBounds = collisionShape.bounds().move(checkPos);
                    if (playerBox.intersects(collisionBounds)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Perform the teleport with sound and particle effects.
     */
    private static void performTeleport(ServerPlayer player, BlockPos entry, BlockPos lastChute) {
        Level level = player.level();

        // Calculate landing position: just below the last chute
        // Player emerges from bottom of chute, offset by package height
        double landingY = lastChute.getY() - PACKAGE_HEIGHT;
        Vec3 landingPos = new Vec3(
                lastChute.getX() + 0.5,
                landingY,
                lastChute.getZ() + 0.5
        );

        // Play subtle entry sound - only audible to the teleporting player (stealth!)
        playStealthSound(player, true);

        // Teleport the player
        if (player.connection != null) {
            // Real gameplay - use teleportTo for proper client sync
            player.teleportTo(landingPos.x, landingPos.y, landingPos.z);
        } else {
            // Game tests - use moveTo since there's no connection
            player.moveTo(landingPos.x, landingPos.y, landingPos.z, player.getYRot(), player.getXRot());
        }

        // Reset velocity (pneumatic tube stops your momentum)
        player.setDeltaMovement(Vec3.ZERO);

        // Force pose back to crouching - teleport can interrupt the visual state
        // Player is still holding shift, so they should remain a package
        player.setPose(Pose.CROUCHING);

        // Play subtle exit sound - only audible to the teleporting player
        playStealthSound(player, false);

        // Trigger advancement (skip if no connection, e.g., in game tests)
        if (player.connection != null) {
            var trigger = ModCriteria.getChuteTeleportTrigger();
            if (trigger != null) trigger.trigger(player);
        }

        CardboardedConveynience.LOGGER.debug("Player {} teleported through chute from {} to {}",
                player.getName().getString(), entry, landingPos);
    }

    /**
     * Play a subtle sound only to the teleporting player.
     * No particles, no broadcast - proper stealth for a sneaky package.
     */
    private static void playStealthSound(ServerPlayer player, boolean isEntry) {
        // Skip if no connection (e.g., in game tests)
        if (player.connection == null) return;

        // Soft whoosh - only the player hears it
        float pitch = isEntry ? 1.5f : 1.2f;
        player.playNotifySound(SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.2f, pitch);
    }

    /**
     * Clear tracking for a player (call on disconnect).
     */
    public static void clearPlayer(Player player) {
        UUID uuid = player.getUUID();
        lastTeleportTick.remove(uuid);
        lastMessageTick.remove(uuid);
    }
}
