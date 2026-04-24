package net.takerudavis.cardboarded_conveynience.test;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.takerudavis.cardboarded_conveynience.util.TunnelPassthroughRegistry;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * GameTests for BeltTunnelBlockMixin functionality.
 * Tests collision shape modification for players and projectiles through belt tunnels.
 *
 * "I can show you the bugs!" - Ryan
 */
public class BeltTunnelGameTest implements FabricGameTest {

    // ========== ANDESITE TUNNEL TESTS ==========

    /**
     * Test that players get hollow collision shape for andesite tunnels.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void playerPassesThroughAndesiteTunnel(GameTestHelper helper) {
        BlockPos tunnelPos = new BlockPos(1, 1, 1);
        helper.setBlock(tunnelPos, AllBlocks.ANDESITE_TUNNEL.getDefaultState());

        Player player = helper.makeMockPlayer();
        player.setPos(helper.absoluteVec(Vec3.atCenterOf(tunnelPos)));

        VoxelShape shape = getCollisionShape(helper, tunnelPos, player);

        helper.assertTrue(isHollowShape(shape), "Player should get hollow collision shape");
        helper.succeed();
    }

    /**
     * Test that ALL registered static projectiles get hollow collision shape.
     * Loops through every projectile type in TunnelPassthroughRegistry.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void allStaticProjectilesPassThroughAndesiteTunnel(GameTestHelper helper) {
        BlockPos tunnelPos = new BlockPos(1, 1, 1);
        helper.setBlock(tunnelPos, AllBlocks.ANDESITE_TUNNEL.getDefaultState());
        Vec3 spawnPos = helper.absoluteVec(Vec3.atCenterOf(tunnelPos));

        // Test each registered projectile type
        for (Map.Entry<Class<? extends Entity>, ItemStack> entry : TunnelPassthroughRegistry.STATIC_PROJECTILES.entrySet()) {
            Class<? extends Entity> projectileClass = entry.getKey();
            ItemStack expectedItem = entry.getValue();

            Entity projectile = createProjectile(helper, projectileClass, spawnPos);
            if (projectile == null) {
                helper.fail("Could not create projectile: " + projectileClass.getSimpleName());
                return;
            }

            helper.getLevel().addFreshEntity(projectile);

            // Verify it's in registry with correct item
            ItemStack filterItem = TunnelPassthroughRegistry.getFilterItem(projectile);
            helper.assertTrue(
                filterItem != null && filterItem.getItem() == expectedItem.getItem(),
                projectileClass.getSimpleName() + " should map to " + expectedItem.getItem()
            );

            VoxelShape shape = getCollisionShape(helper, tunnelPos, projectile);
            helper.assertTrue(
                isHollowShape(shape),
                projectileClass.getSimpleName() + " should get hollow collision shape"
            );

            projectile.discard();
        }

        helper.succeed();
    }

    /**
     * Test that non-eligible entities (chicken) are blocked.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void chickenBlockedByAndesiteTunnel(GameTestHelper helper) {
        BlockPos tunnelPos = new BlockPos(1, 1, 1);
        helper.setBlock(tunnelPos, AllBlocks.ANDESITE_TUNNEL.getDefaultState());

        Chicken chicken = helper.spawn(EntityType.CHICKEN, new BlockPos(1, 1, 0));

        // Verify chicken is NOT in registry
        helper.assertTrue(
            TunnelPassthroughRegistry.getFilterItem(chicken) == null,
            "Chicken should not be in projectile registry"
        );

        VoxelShape shape = getCollisionShape(helper, tunnelPos, chicken);

        helper.assertTrue(!shape.isEmpty(), "Chicken should be blocked by tunnel");
        helper.succeed();
    }

    /**
     * Test that VoxelShape caching works - same instance returned.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void voxelShapeCacheReturnsSameInstance(GameTestHelper helper) {
        BlockPos tunnelPos = new BlockPos(1, 1, 1);
        helper.setBlock(tunnelPos, AllBlocks.ANDESITE_TUNNEL.getDefaultState());

        Player player = helper.makeMockPlayer();
        player.setPos(helper.absoluteVec(Vec3.atCenterOf(tunnelPos)));

        VoxelShape shape1 = getCollisionShape(helper, tunnelPos, player);
        VoxelShape shape2 = getCollisionShape(helper, tunnelPos, player);

        helper.assertTrue(shape1 == shape2, "VoxelShape should be cached (same instance)");
        helper.succeed();
    }

    // ========== BRASS TUNNEL TESTS ==========

    /**
     * Test that brass tunnel without filter allows eligible entities.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void brassTunnelNoFilterAllowsPlayer(GameTestHelper helper) {
        BlockPos tunnelPos = new BlockPos(1, 1, 1);
        helper.setBlock(tunnelPos, AllBlocks.BRASS_TUNNEL.getDefaultState());

        // Wait a tick for block entity to initialize
        helper.runAfterDelay(1, () -> {
            Player player = helper.makeMockPlayer();
            player.setPos(helper.absoluteVec(Vec3.atCenterOf(tunnelPos)));

            VoxelShape shape = getCollisionShape(helper, tunnelPos, player);

            helper.assertTrue(isHollowShape(shape), "Brass tunnel without filter should allow player");
            helper.succeed();
        });
    }

    /**
     * Test that brass tunnel without filter allows registered projectiles.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void brassTunnelNoFilterAllowsProjectile(GameTestHelper helper) {
        BlockPos tunnelPos = new BlockPos(1, 1, 1);
        helper.setBlock(tunnelPos, AllBlocks.BRASS_TUNNEL.getDefaultState());

        helper.runAfterDelay(1, () -> {
            Vec3 spawnPos = helper.absoluteVec(Vec3.atCenterOf(tunnelPos));
            Snowball snowball = new Snowball(helper.getLevel(), spawnPos.x, spawnPos.y, spawnPos.z);
            helper.getLevel().addFreshEntity(snowball);

            VoxelShape shape = getCollisionShape(helper, tunnelPos, snowball);

            helper.assertTrue(isHollowShape(shape), "Brass tunnel without filter should allow snowball");
            snowball.discard();
            helper.succeed();
        });
    }

    /**
     * Test that brass tunnel with matching filter allows projectile.
     * TODO: Fix reflection-based filter setting for tests
     */
    // @GameTest(template = EMPTY_STRUCTURE)
    public void brassTunnelMatchingFilterAllowsProjectile(GameTestHelper helper) {
        BlockPos tunnelPos = new BlockPos(1, 1, 1);
        helper.setBlock(tunnelPos, AllBlocks.BRASS_TUNNEL.getDefaultState());

        // Brass tunnel behaviors need more time to initialize
        helper.runAfterDelay(5, () -> {
            // Set filter to snowball
            boolean filterSet = setTunnelFilter(helper, tunnelPos, Direction.NORTH, new ItemStack(Items.SNOWBALL));
            helper.assertTrue(filterSet, "Should be able to set tunnel filter");

            Vec3 spawnPos = helper.absoluteVec(new Vec3(1.5, 1.5, 0.5)); // North of tunnel
            Snowball snowball = new Snowball(helper.getLevel(), spawnPos.x, spawnPos.y, spawnPos.z);
            helper.getLevel().addFreshEntity(snowball);

            VoxelShape shape = getCollisionShape(helper, tunnelPos, snowball);

            helper.assertTrue(isHollowShape(shape), "Brass tunnel with matching snowball filter should allow snowball");
            snowball.discard();
            helper.succeed();
        });
    }

    /**
     * Test that brass tunnel with non-matching filter blocks projectile.
     * TODO: Fix reflection-based filter setting for tests
     */
    // @GameTest(template = EMPTY_STRUCTURE)
    public void brassTunnelNonMatchingFilterBlocksProjectile(GameTestHelper helper) {
        BlockPos tunnelPos = new BlockPos(1, 1, 1);
        helper.setBlock(tunnelPos, AllBlocks.BRASS_TUNNEL.getDefaultState());

        // Brass tunnel behaviors need more time to initialize
        helper.runAfterDelay(5, () -> {
            // Set filter to arrow (not snowball)
            boolean filterSet = setTunnelFilter(helper, tunnelPos, Direction.NORTH, new ItemStack(Items.ARROW));
            helper.assertTrue(filterSet, "Should be able to set tunnel filter");

            Vec3 spawnPos = helper.absoluteVec(new Vec3(1.5, 1.5, 0.5)); // North of tunnel
            Snowball snowball = new Snowball(helper.getLevel(), spawnPos.x, spawnPos.y, spawnPos.z);
            helper.getLevel().addFreshEntity(snowball);

            VoxelShape shape = getCollisionShape(helper, tunnelPos, snowball);

            // Snowball should be blocked because filter is set to arrow
            helper.assertTrue(!isHollowShape(shape), "Brass tunnel with arrow filter should block snowball");
            snowball.discard();
            helper.succeed();
        });
    }

    // ========== HELPER METHODS ==========

    /**
     * Get collision shape for an entity at a block position.
     */
    private VoxelShape getCollisionShape(GameTestHelper helper, BlockPos pos, Entity entity) {
        BlockState state = helper.getBlockState(pos);
        CollisionContext context = CollisionContext.of(entity);
        return state.getCollisionShape(helper.getLevel(), helper.absolutePos(pos), context);
    }

    /**
     * Check if a VoxelShape is hollow (has cutout in middle).
     */
    private boolean isHollowShape(VoxelShape shape) {
        // Hollow shapes either have multiple AABBs or are empty
        return shape.isEmpty() || shape.toAabbs().size() > 1;
    }

    /**
     * Set a filter on a brass tunnel using reflection.
     * Returns true if successful.
     */
    private boolean setTunnelFilter(GameTestHelper helper, BlockPos pos, Direction side, ItemStack filter) {
        BlockEntity be = helper.getBlockEntity(pos);
        if (!(be instanceof BrassTunnelBlockEntity brassTunnel)) {
            return false;
        }

        try {
            // Access the filtering field via reflection
            Field filteringField = BrassTunnelBlockEntity.class.getDeclaredField("filtering");
            filteringField.setAccessible(true);
            SidedFilteringBehaviour filtering = (SidedFilteringBehaviour) filteringField.get(brassTunnel);

            if (filtering != null) {
                return filtering.setFilter(side, filter);
            }
        } catch (Exception e) {
            // Reflection failed - log and return false
            helper.fail("Failed to set tunnel filter: " + e.getMessage());
        }
        return false;
    }

    /**
     * Create a projectile entity of the given class at the specified position.
     */
    private Entity createProjectile(GameTestHelper helper, Class<? extends Entity> projectileClass, Vec3 pos) {
        try {
            if (projectileClass == Snowball.class) {
                return new Snowball(helper.getLevel(), pos.x, pos.y, pos.z);
            } else if (projectileClass == Arrow.class || projectileClass.getSuperclass() == Arrow.class
                       || projectileClass.getName().contains("AbstractArrow")) {
                // Use Arrow as representative for AbstractArrow
                return new Arrow(helper.getLevel(), pos.x, pos.y, pos.z);
            } else if (projectileClass == ThrownTrident.class) {
                ThrownTrident trident = new ThrownTrident(EntityType.TRIDENT, helper.getLevel());
                trident.setPos(pos.x, pos.y, pos.z);
                return trident;
            } else if (projectileClass == ThrownEnderpearl.class) {
                Player player = helper.makeMockPlayer();
                return new ThrownEnderpearl(helper.getLevel(), player);
            } else if (projectileClass == ThrownPotion.class) {
                Player player = helper.makeMockPlayer();
                ThrownPotion potion = new ThrownPotion(helper.getLevel(), player);
                potion.setPos(pos.x, pos.y, pos.z);
                return potion;
            } else if (projectileClass == FireworkRocketEntity.class) {
                return new FireworkRocketEntity(helper.getLevel(), pos.x, pos.y, pos.z, ItemStack.EMPTY);
            } else if (projectileClass == FishingHook.class) {
                Player player = helper.makeMockPlayer();
                FishingHook hook = new FishingHook(player, helper.getLevel(), 0, 0);
                hook.setPos(pos.x, pos.y, pos.z);
                return hook;
            }
        } catch (Exception e) {
            // Some projectiles may fail to create in test environment
            return null;
        }
        return null;
    }
}
