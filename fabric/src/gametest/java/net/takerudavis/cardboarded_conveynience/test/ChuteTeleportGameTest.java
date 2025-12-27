package net.takerudavis.cardboarded_conveynience.test;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.chute.ChuteBlock;
import com.simibubi.create.content.logistics.chute.SmartChuteBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.takerudavis.cardboarded_conveynience.config.ModConfig;
import net.takerudavis.cardboarded_conveynience.util.ChuteTeleportHelper;

/**
 * GameTests for ChuteTeleportHelper - the chute teleportation system.
 *
 * "Ooh, what happens if I drop a player down a 50-block chute?
 *  ...Actually, let's find out!" - Ryan
 */
public class ChuteTeleportGameTest implements FabricGameTest {

    // Create's cardboard armor item IDs
    private static final ResourceLocation CARDBOARD_HELMET = new ResourceLocation("create", "cardboard_helmet");
    private static final ResourceLocation CARDBOARD_CHESTPLATE = new ResourceLocation("create", "cardboard_chestplate");
    private static final ResourceLocation CARDBOARD_LEGGINGS = new ResourceLocation("create", "cardboard_leggings");
    private static final ResourceLocation CARDBOARD_BOOTS = new ResourceLocation("create", "cardboard_boots");

    // Package height constant (must match ChuteTeleportHelper.PACKAGE_HEIGHT)
    private static final double PACKAGE_HEIGHT = 0.6;

    // ========== BASIC TELEPORT TESTS ==========

    /**
     * Test basic vertical chute teleport - player with armor + sneaking teleports down.
     * "The classic drop test. Simple. Elegant. Terrifying." - Ryan
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void basicVerticalChuteTeleport(GameTestHelper helper) {
        // Build a 5-block vertical chute
        BlockPos topChute = new BlockPos(1, 6, 1);
        BlockPos bottomChute = new BlockPos(1, 2, 1);

        for (int y = 2; y <= 6; y++) {
            helper.setBlock(new BlockPos(1, y, 1), AllBlocks.CHUTE.getDefaultState());
        }
        // Air below the bottom chute for exit
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.AIR.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            // Create and setup player
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            // Position player on top of the chute
            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 7.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            // Record starting Y
            double startY = player.getY();

            // Tick the teleport system
            ChuteTeleportHelper.tickPlayer(player);

            // Player should have teleported down
            // Landing Y = bottomChute.Y - PACKAGE_HEIGHT = 2 - 0.6 = 1.4
            double expectedY = helper.absolutePos(bottomChute).getY() - PACKAGE_HEIGHT;
            double actualY = player.getY();

            helper.assertTrue(
                Math.abs(actualY - expectedY) < 0.1,
                "Player should teleport to Y=" + expectedY + " but was at Y=" + actualY
            );
            helper.succeed();
        });
    }

    /**
     * Test that player without armor doesn't teleport.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void noArmorNoTeleport(GameTestHelper helper) {
        BlockPos chutePos = new BlockPos(1, 2, 1);
        helper.setBlock(chutePos, AllBlocks.CHUTE.getDefaultState());
        helper.setBlock(chutePos.below(), Blocks.AIR.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            // NO armor!

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 3.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() == startY,
                "Player without armor should NOT teleport"
            );
            helper.succeed();
        });
    }

    /**
     * Test that player not sneaking doesn't teleport.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void notSneakingNoTeleport(GameTestHelper helper) {
        BlockPos chutePos = new BlockPos(1, 2, 1);
        helper.setBlock(chutePos, AllBlocks.CHUTE.getDefaultState());
        helper.setBlock(chutePos.below(), Blocks.AIR.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);
            // NOT sneaking!

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 3.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(false);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() == startY,
                "Player not sneaking should NOT teleport"
            );
            helper.succeed();
        });
    }

    // ========== HEIGHT DETECTION TESTS ==========

    /**
     * Test that player on carpet (0.0625 blocks) CAN teleport.
     * "Carpets are basically decorative air, right?" - Ryan
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void carpetHeightAllowsTeleport(GameTestHelper helper) {
        BlockPos chutePos = new BlockPos(1, 2, 1);
        helper.setBlock(chutePos, AllBlocks.CHUTE.getDefaultState());
        helper.setBlock(chutePos.above(), Blocks.WHITE_CARPET.defaultBlockState());
        helper.setBlock(chutePos.below(), Blocks.AIR.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            // Position on carpet (carpet is 1/16 = 0.0625 blocks tall)
            // Chute top is at Y=3, carpet top is at Y=3.0625
            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 3.0625, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() != startY,
                "Player on carpet should teleport (height 0.0625 < 0.5)"
            );
            helper.succeed();
        });
    }

    /**
     * Test that player on slab (0.5 blocks) does NOT teleport.
     * "Half a block too high! So close, yet so far." - Ryan
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void slabHeightBlocksTeleport(GameTestHelper helper) {
        BlockPos chutePos = new BlockPos(1, 2, 1);
        helper.setBlock(chutePos, AllBlocks.CHUTE.getDefaultState());
        // Bottom slab on top of chute
        helper.setBlock(chutePos.above(), Blocks.STONE_SLAB.defaultBlockState());
        helper.setBlock(chutePos.below(), Blocks.AIR.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            // Position on slab (slab is 0.5 blocks tall)
            // Chute top is at Y=3, slab top is at Y=3.5
            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 3.5, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() == startY,
                "Player on slab should NOT teleport (height 0.5 >= 0.5 threshold)"
            );
            helper.succeed();
        });
    }

    /**
     * Test that player on closed trapdoor (0.1875 blocks) CAN teleport.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void trapdoorHeightAllowsTeleport(GameTestHelper helper) {
        BlockPos chutePos = new BlockPos(1, 2, 1);
        helper.setBlock(chutePos, AllBlocks.CHUTE.getDefaultState());
        // Closed trapdoor on bottom half (on top of chute)
        BlockState trapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState()
            .setValue(TrapDoorBlock.HALF, Half.BOTTOM)
            .setValue(TrapDoorBlock.OPEN, false);
        helper.setBlock(chutePos.above(), trapdoor);
        helper.setBlock(chutePos.below(), Blocks.AIR.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            // Position on trapdoor (3/16 = 0.1875 blocks tall)
            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 3.1875, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() != startY,
                "Player on trapdoor should teleport (height 0.1875 < 0.5)"
            );
            helper.succeed();
        });
    }

    // ========== CHUTE OPENING TESTS ==========

    /**
     * Test that diagonal chute (no top opening) does NOT allow teleport.
     * "Nice try, but that's a slide, not an entrance!" - Ryan
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void diagonalChuteNoTopOpening(GameTestHelper helper) {
        BlockPos chutePos = new BlockPos(1, 2, 1);

        // Create a diagonal chute facing NORTH (not DOWN) - no top opening
        BlockState diagonalChute = AllBlocks.CHUTE.getDefaultState()
                .setValue(ChuteBlock.FACING, Direction.NORTH)
                .setValue(ChuteBlock.SHAPE, ChuteBlock.Shape.NORMAL);
        helper.setBlock(chutePos, diagonalChute);
        helper.setBlock(chutePos.below(), Blocks.AIR.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 3.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() == startY,
                "Player on diagonal chute (no top opening) should NOT teleport"
            );
            helper.succeed();
        });
    }

    /**
     * Test that intersection chute allows teleport.
     * "Multiple paths, one entrance - still works!" - Ryan
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void intersectionChuteAllowsTeleport(GameTestHelper helper) {
        BlockPos chutePos = new BlockPos(1, 2, 1);

        // Create an intersection chute - has top opening regardless of facing
        BlockState intersectionChute = AllBlocks.CHUTE.getDefaultState()
                .setValue(ChuteBlock.FACING, Direction.NORTH)
                .setValue(ChuteBlock.SHAPE, ChuteBlock.Shape.INTERSECTION);
        helper.setBlock(chutePos, intersectionChute);
        helper.setBlock(chutePos.below(), Blocks.AIR.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 3.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() != startY,
                "Player on intersection chute should teleport (has top opening)"
            );
            helper.succeed();
        });
    }

    // ========== EXIT VALIDATION TESTS ==========

    /**
     * Test that solid block at exit blocks teleportation.
     * "You can't teleport INTO a wall. Well, you can, but you shouldn't." - Ryan
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void solidBlockBlocksExit(GameTestHelper helper) {
        BlockPos topChute = new BlockPos(1, 3, 1);
        BlockPos bottomChute = new BlockPos(1, 2, 1);

        helper.setBlock(topChute, AllBlocks.CHUTE.getDefaultState());
        helper.setBlock(bottomChute, AllBlocks.CHUTE.getDefaultState());
        // STONE blocking the exit!
        helper.setBlock(bottomChute.below(), Blocks.STONE.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 4.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() == startY,
                "Player should NOT teleport when exit is blocked by solid block"
            );
            helper.succeed();
        });
    }

    /**
     * Test that closed trapdoor at exit blocks teleportation.
     * Unlike bottom trapdoor, a top-half closed trapdoor would intersect the player.
     * "Trapdoors: sometimes open, sometimes TRAP!" - Ryan
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void closedTrapdoorBlocksExit(GameTestHelper helper) {
        BlockPos topChute = new BlockPos(1, 3, 1);
        BlockPos bottomChute = new BlockPos(1, 2, 1);

        helper.setBlock(topChute, AllBlocks.CHUTE.getDefaultState());
        helper.setBlock(bottomChute, AllBlocks.CHUTE.getDefaultState());
        // Closed top-half trapdoor - collision at 0.8125 to 1.0, player lands at 1.4
        // Actually wait - player box is 1.5 tall, so from 1.4 to 2.9
        // Top trapdoor collision is at Y+0.8125 to Y+1, so at exit block Y=1: 1.8125 to 2.0
        // Player box 1.4 to 2.9 DOES intersect 1.8125 to 2.0!
        BlockState topTrapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState()
            .setValue(TrapDoorBlock.HALF, Half.TOP)
            .setValue(TrapDoorBlock.OPEN, false);
        helper.setBlock(bottomChute.below(), topTrapdoor);

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 4.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() == startY,
                "Player should NOT teleport through closed top trapdoor (collision intersects)"
            );
            helper.succeed();
        });
    }

    /**
     * Test that open trapdoor at exit allows teleportation.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void openTrapdoorAllowsExit(GameTestHelper helper) {
        BlockPos topChute = new BlockPos(1, 3, 1);
        BlockPos bottomChute = new BlockPos(1, 2, 1);

        helper.setBlock(topChute, AllBlocks.CHUTE.getDefaultState());
        helper.setBlock(bottomChute, AllBlocks.CHUTE.getDefaultState());
        // Open trapdoor at exit - no collision!
        BlockState openTrapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState()
            .setValue(TrapDoorBlock.OPEN, true);
        helper.setBlock(bottomChute.below(), openTrapdoor);
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.AIR.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 4.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() != startY,
                "Player should teleport through open trapdoor (no collision)"
            );
            helper.succeed();
        });
    }

    /**
     * Test that open fence gate at exit allows teleportation.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void openFenceGateAllowsExit(GameTestHelper helper) {
        BlockPos topChute = new BlockPos(1, 3, 1);
        BlockPos bottomChute = new BlockPos(1, 2, 1);

        helper.setBlock(topChute, AllBlocks.CHUTE.getDefaultState());
        helper.setBlock(bottomChute, AllBlocks.CHUTE.getDefaultState());
        // Open fence gate at exit - no collision!
        BlockState openGate = Blocks.OAK_FENCE_GATE.defaultBlockState()
            .setValue(FenceGateBlock.OPEN, true);
        helper.setBlock(bottomChute.below(), openGate);
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.AIR.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 4.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() != startY,
                "Player should teleport through open fence gate (no collision)"
            );
            helper.succeed();
        });
    }

    /**
     * Test that bottom trapdoor at exit allows teleportation.
     * Player lands above the trapdoor collision box.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void bottomTrapdoorAllowsExit(GameTestHelper helper) {
        BlockPos topChute = new BlockPos(1, 3, 1);
        BlockPos bottomChute = new BlockPos(1, 2, 1);

        helper.setBlock(topChute, AllBlocks.CHUTE.getDefaultState());
        helper.setBlock(bottomChute, AllBlocks.CHUTE.getDefaultState());
        // Closed bottom trapdoor - player lands at Y=1.4, trapdoor collision is 1.0-1.1875
        BlockState bottomTrapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState()
            .setValue(TrapDoorBlock.HALF, Half.BOTTOM)
            .setValue(TrapDoorBlock.OPEN, false);
        helper.setBlock(bottomChute.below(), bottomTrapdoor);

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 4.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            // Player lands at bottomChute.Y - 0.6 = 2 - 0.6 = 1.4
            // Bottom trapdoor collision is at 1.0 to 1.1875, so 1.4 clears it!
            helper.assertTrue(
                player.getY() != startY,
                "Player should teleport above bottom trapdoor (lands at Y=1.4, trapdoor at 1.0-1.1875)"
            );
            helper.succeed();
        });
    }

    /**
     * Test that fence post at exit blocks teleportation.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void fencePostBlocksExit(GameTestHelper helper) {
        BlockPos topChute = new BlockPos(1, 3, 1);
        BlockPos bottomChute = new BlockPos(1, 2, 1);

        helper.setBlock(topChute, AllBlocks.CHUTE.getDefaultState());
        helper.setBlock(bottomChute, AllBlocks.CHUTE.getDefaultState());
        // Fence post in the way!
        helper.setBlock(bottomChute.below(), Blocks.OAK_FENCE.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 4.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() == startY,
                "Player should NOT teleport into fence post"
            );
            helper.succeed();
        });
    }

    // ========== DIAGONAL CHUTE TESTS ==========

    /**
     * Test diagonal chute path following.
     * "Items go diagonal, players go diagonal. Physics!" - Ryan
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void diagonalChutePathFollowing(GameTestHelper helper) {
        // Build diagonal chute: starts at (1,5,1), ends at (3,2,1)
        // Each diagonal chute goes down and sideways

        // Top chute - straight down
        helper.setBlock(new BlockPos(1, 5, 1), AllBlocks.CHUTE.getDefaultState());

        // Diagonal chutes facing WEST (items come FROM west, so they go EAST)
        BlockState diagonalChute = AllBlocks.CHUTE.getDefaultState()
            .setValue(ChuteBlock.FACING, Direction.WEST);
        helper.setBlock(new BlockPos(1, 4, 1), diagonalChute);
        helper.setBlock(new BlockPos(2, 3, 1), diagonalChute);
        helper.setBlock(new BlockPos(3, 2, 1), AllBlocks.CHUTE.getDefaultState());

        // Air at exit
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.AIR.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 6.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            ChuteTeleportHelper.tickPlayer(player);

            // Should end up at X=3.5 (center of bottom chute at x=3)
            BlockPos expectedExit = helper.absolutePos(new BlockPos(3, 2, 1));
            double expectedX = expectedExit.getX() + 0.5;
            double expectedY = expectedExit.getY() - PACKAGE_HEIGHT;

            helper.assertTrue(
                Math.abs(player.getX() - expectedX) < 0.1,
                "Player X should be at diagonal exit: expected " + expectedX + ", got " + player.getX()
            );
            helper.assertTrue(
                Math.abs(player.getY() - expectedY) < 0.1,
                "Player Y should be at diagonal exit: expected " + expectedY + ", got " + player.getY()
            );
            helper.succeed();
        });
    }

    // ========== PATH LENGTH LIMIT TESTS ==========

    /**
     * Test that path length respects config limit.
     * "How deep can we go? Let's find out!" - Ryan
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void pathLengthLimitRespected(GameTestHelper helper) {
        // Build a chute that exceeds the default limit
        // We'll build 10 chutes but set limit to 5
        int originalLimit = ModConfig.get().maxChutePathLength;

        // Temporarily set a low limit for testing
        ModConfig.get().maxChutePathLength = 5;

        // Build 10 chutes
        for (int y = 2; y <= 11; y++) {
            helper.setBlock(new BlockPos(1, y, 1), AllBlocks.CHUTE.getDefaultState());
        }
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.AIR.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            try {
                ServerPlayer player = createTestServerPlayer(helper);
                equipFullCardboardArmor(player);

                Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 12.0, 1.5));
                player.setPos(startPos.x, startPos.y, startPos.z);
                player.setShiftKeyDown(true);

                double startY = player.getY();
                ChuteTeleportHelper.tickPlayer(player);

                // With limit of 5, the 10-chute path should fail
                helper.assertTrue(
                    player.getY() == startY,
                    "Player should NOT teleport when path exceeds limit (10 chutes > 5 limit)"
                );
            } finally {
                // Restore original limit
                ModConfig.get().maxChutePathLength = originalLimit;
            }
            helper.succeed();
        });
    }

    /**
     * Test that path within limit works.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void pathWithinLimitWorks(GameTestHelper helper) {
        int originalLimit = ModConfig.get().maxChutePathLength;
        ModConfig.get().maxChutePathLength = 10;

        // Build 5 chutes - well within limit
        for (int y = 2; y <= 6; y++) {
            helper.setBlock(new BlockPos(1, y, 1), AllBlocks.CHUTE.getDefaultState());
        }
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.AIR.defaultBlockState());

        helper.runAfterDelay(5, () -> {
            try {
                ServerPlayer player = createTestServerPlayer(helper);
                equipFullCardboardArmor(player);

                Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 7.0, 1.5));
                player.setPos(startPos.x, startPos.y, startPos.z);
                player.setShiftKeyDown(true);

                double startY = player.getY();
                ChuteTeleportHelper.tickPlayer(player);

                helper.assertTrue(
                    player.getY() != startY,
                    "Player should teleport when path within limit (5 chutes < 10 limit)"
                );
            } finally {
                ModConfig.get().maxChutePathLength = originalLimit;
            }
            helper.succeed();
        });
    }

    // ========== SMART CHUTE FILTER TESTS ==========
    // Note: These require SmartChuteBlockEntity setup which may need
    // additional ticks for block entity initialization

    /**
     * Test that empty smart chute filter allows passage.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void emptySmartChuteFilterAllows(GameTestHelper helper) {
        BlockPos topChute = new BlockPos(1, 3, 1);
        BlockPos smartChutePos = new BlockPos(1, 2, 1);

        helper.setBlock(topChute, AllBlocks.CHUTE.getDefaultState());
        helper.setBlock(smartChutePos, AllBlocks.SMART_CHUTE.getDefaultState());
        helper.setBlock(smartChutePos.below(), Blocks.AIR.defaultBlockState());

        // Wait for block entity to initialize
        helper.runAfterDelay(10, () -> {
            // Smart chute with empty filter should allow passage
            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 4.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() != startY,
                "Player should teleport through smart chute with empty filter"
            );
            helper.succeed();
        });
    }

    /**
     * Test that wrong item filter on smart chute blocks passage.
     * "Cobblestone filter? Sorry mate, you're not cobblestone!" - Ryan
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void wrongSmartChuteFilterBlocks(GameTestHelper helper) {
        BlockPos topChute = new BlockPos(1, 3, 1);
        BlockPos smartChutePos = new BlockPos(1, 2, 1);

        helper.setBlock(topChute, AllBlocks.CHUTE.getDefaultState());
        helper.setBlock(smartChutePos, AllBlocks.SMART_CHUTE.getDefaultState());
        helper.setBlock(smartChutePos.below(), Blocks.AIR.defaultBlockState());

        // Wait for block entity to initialize, then set filter
        helper.runAfterDelay(10, () -> {
            // Set filter to cobblestone - player is NOT cobblestone!
            BlockPos absPos = helper.absolutePos(smartChutePos);
            FilteringBehaviour filtering = BlockEntityBehaviour.get(
                helper.getLevel(), absPos, FilteringBehaviour.TYPE);

            if (filtering != null) {
                filtering.setFilter(new ItemStack(Items.COBBLESTONE));
            }

            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 4.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() == startY,
                "Player should NOT teleport through smart chute with cobblestone filter"
            );
            helper.succeed();
        });
    }

    /**
     * Test that package filter on smart chute allows passage.
     * "Package filter? Well I AM a package!" - Ryan
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void packageSmartChuteFilterAllows(GameTestHelper helper) {
        BlockPos topChute = new BlockPos(1, 3, 1);
        BlockPos smartChutePos = new BlockPos(1, 2, 1);

        helper.setBlock(topChute, AllBlocks.CHUTE.getDefaultState());
        helper.setBlock(smartChutePos, AllBlocks.SMART_CHUTE.getDefaultState());
        helper.setBlock(smartChutePos.below(), Blocks.AIR.defaultBlockState());

        // Wait for block entity to initialize, then set filter
        helper.runAfterDelay(10, () -> {
            // Set filter to a package - player IS a package!
            BlockPos absPos = helper.absolutePos(smartChutePos);
            FilteringBehaviour filtering = BlockEntityBehaviour.get(
                helper.getLevel(), absPos, FilteringBehaviour.TYPE);

            if (filtering != null) {
                // Create a package item using PackageStyles
                ItemStack packageStack = PackageStyles.getDefaultBox();
                filtering.setFilter(packageStack);
            }

            ServerPlayer player = createTestServerPlayer(helper);
            equipFullCardboardArmor(player);

            Vec3 startPos = helper.absoluteVec(new Vec3(1.5, 4.0, 1.5));
            player.setPos(startPos.x, startPos.y, startPos.z);
            player.setShiftKeyDown(true);

            double startY = player.getY();
            ChuteTeleportHelper.tickPlayer(player);

            helper.assertTrue(
                player.getY() != startY,
                "Player should teleport through smart chute with package filter"
            );
            helper.succeed();
        });
    }

    // ========== HELPER METHODS ==========

    /**
     * Get a cardboard armor item from the registry.
     */
    private ItemStack getCardboardItem(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item);
    }

    /**
     * Equip a player with full cardboard armor set.
     */
    private void equipFullCardboardArmor(Player player) {
        player.setItemSlot(EquipmentSlot.HEAD, getCardboardItem(CARDBOARD_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, getCardboardItem(CARDBOARD_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, getCardboardItem(CARDBOARD_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, getCardboardItem(CARDBOARD_BOOTS));
    }

    /**
     * Create a ServerPlayer for testing.
     * In 1.20.1, GameTestHelper doesn't have makeMockServerPlayer(),
     * so we create one manually.
     */
    private ServerPlayer createTestServerPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "TestPlayer");
        return new ServerPlayer(level.getServer(), level, profile);
    }
}
