package net.takerudavis.cardboarded_conveynience.mixin;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelShapes;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.takerudavis.cardboarded_conveynience.util.TunnelPassthroughRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.takerudavis.cardboarded_conveynience.util.CardboardHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.world.level.block.Block.box;

/**
 * Allows disguised players and certain projectiles to pass through belt tunnels.
 * For brass tunnels, respects the filter settings.
 */
@Mixin(BlockBehaviour.class)
public class BeltTunnelBlockMixin {

    @Unique
    private static final Map<BlockState, VoxelShape> cardboarded_conveynience$hollowShapeCache = new ConcurrentHashMap<>();

    @Unique
    private static VoxelShape cardboarded_conveynience$getHollowShape(BlockState state) {
        return cardboarded_conveynience$hollowShapeCache.computeIfAbsent(state, s -> {
            VoxelShape baseShape = BeltTunnelShapes.getShape(s);
            VoxelShape innerBox = box(2, -5, 2, 14, 10, 14);
            return Shapes.join(baseShape, innerBox, BooleanOp.ONLY_FIRST);
        });
    }

    // Class name for PotatoProjectileEntity (checked at runtime to avoid compile-time dependency issues)
    @Unique
    private static final String POTATO_PROJECTILE_CLASS = "com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity";

    @Unique
    private static ItemStack cardboarded_conveynience$getProjectileItem(Entity entity) {
        // PotatoProjectileEntity stores the actual food item being launched - filter by that!
        // Use class name check to avoid compile-time dependency on NeoForge-specific interfaces
        if (entity.getClass().getName().equals(POTATO_PROJECTILE_CLASS)) {
            // PotatoProjectileEntity extends ThrowableItemProjectile which has getItem()
            if (entity instanceof ThrowableItemProjectile throwable) {
                return throwable.getItem();
            }
        }
        // Check static projectile registry
        return TunnelPassthroughRegistry.getFilterItem(entity);
    }

    @Unique
    private static boolean cardboarded_conveynience$isAllowedProjectile(Entity entity) {
        return cardboarded_conveynience$getProjectileItem(entity) != null;
    }

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void cardboarded_conveynience$injectCollisionShape(
            BlockState state,
            BlockGetter world,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (!(state.getBlock() instanceof BeltTunnelBlock tunnelBlock)) return;

        // Check if this is a brass tunnel that might have filtering
        // We can only properly check filters when we have entity context
        // For null context: brass tunnels are treated as potentially filtered (conservative)
        // unless we can verify all sides pass a test item
        boolean isBrassTunnel = tunnelBlock instanceof BrassTunnelBlock;
        boolean hasFilter = false;

        if (isBrassTunnel && world.getBlockEntity(pos) instanceof BrassTunnelBlockEntity brassTunnel) {
            // Test with empty item - if ANY side blocks it, there's selective filtering
            ItemStack testItem = ItemStack.EMPTY;
            boolean allSidesPass = brassTunnel.testFlapFilter(Direction.NORTH, testItem) &&
                                   brassTunnel.testFlapFilter(Direction.SOUTH, testItem) &&
                                   brassTunnel.testFlapFilter(Direction.EAST, testItem) &&
                                   brassTunnel.testFlapFilter(Direction.WEST, testItem);
            hasFilter = !allSidesPass;
        }

        // For unfiltered tunnels: if we have no entity context, still allow passage
        // This fixes projectiles that sometimes get checked with null context
        if (!(context instanceof EntityCollisionContext entityContext)) {
            if (!hasFilter) {
                // Unfiltered tunnel with unknown entity - allow passage
                cir.setReturnValue(cardboarded_conveynience$getHollowShape(state));
            }
            return;
        }

        Entity entity = entityContext.getEntity();
        if (entity == null) {
            if (!hasFilter) {
                // Unfiltered tunnel with null entity - allow passage
                cir.setReturnValue(cardboarded_conveynience$getHollowShape(state));
            }
            return;
        }

        // Check vertical positioning - don't allow passage if entity is above or below tunnel
        double deltaY = (entity.getBlockY() - pos.getY() + 0.5);
        if (Math.abs(deltaY) > 0.5) {
            return;
        }

        boolean matchesFilter = cardboarded_conveynience$checkTunnelAccess(state, world, pos, entity);

        if (!matchesFilter) return;

        // Return cached passable shape with inner box cut out
        cir.setReturnValue(cardboarded_conveynience$getHollowShape(state));
    }

    @Unique
    private boolean cardboarded_conveynience$checkTunnelAccess(BlockState state, BlockGetter world, BlockPos pos, Entity entity) {
        // Quick eligibility check first - skip expensive position checks for ineligible entities
        boolean isPlayer = entity instanceof Player;
        boolean isAllowedProjectile = cardboarded_conveynience$isAllowedProjectile(entity);

        if (!isPlayer && !isAllowedProjectile) {
            return false;
        }

        // If entity is already inside a tunnel, allow passage (mid-transit case)
        BlockPos eyePos = BlockPos.containing(entity.getEyePosition());
        BlockPos feetPos = entity.blockPosition();

        if (world.getBlockState(eyePos).getBlock() instanceof BeltTunnelBlock ||
            world.getBlockState(feetPos).getBlock() instanceof BeltTunnelBlock) {
            return true;
        }

        // For projectiles, do extended position checks
        if (isAllowedProjectile) {
            // Check bounding box intersections
            BlockPos minPos = BlockPos.containing(entity.getBoundingBox().minX, entity.getBoundingBox().minY, entity.getBoundingBox().minZ);
            BlockPos maxPos = BlockPos.containing(entity.getBoundingBox().maxX, entity.getBoundingBox().maxY, entity.getBoundingBox().maxZ);

            for (BlockPos checkPos : BlockPos.betweenClosed(minPos, maxPos)) {
                if (world.getBlockState(checkPos).getBlock() instanceof BeltTunnelBlock) {
                    return true;
                }
            }

            // Check previous tick position - fast projectiles might have been in a tunnel last tick
            BlockPos oldPos = BlockPos.containing(entity.xOld, entity.yOld, entity.zOld);
            if (world.getBlockState(oldPos).getBlock() instanceof BeltTunnelBlock) {
                return true;
            }

            // Check along the motion path from old position to current
            double dx = entity.getX() - entity.xOld;
            double dy = entity.getY() - entity.yOld;
            double dz = entity.getZ() - entity.zOld;
            for (int i = 1; i <= 4; i++) {
                double t = i / 4.0;
                BlockPos pathPos = BlockPos.containing(
                    entity.xOld + dx * t,
                    entity.yOld + dy * t,
                    entity.zOld + dz * t
                );
                if (world.getBlockState(pathPos).getBlock() instanceof BeltTunnelBlock) {
                    return true;
                }
            }
        }

        // Brass tunnel - check filter
        if (state.getBlock() instanceof BrassTunnelBlock && world.getBlockEntity(pos) instanceof BrassTunnelBlockEntity tunnelBlockEntity) {
            double deltaX = (entity.getBlockX() - pos.getX());
            double deltaZ = (entity.getBlockZ() - pos.getZ());
            Direction side = Math.abs(deltaX) > Math.abs(deltaZ)
                    ? (deltaX < 0 ? Direction.WEST : Direction.EAST)
                    : (deltaZ < 0 ? Direction.NORTH : Direction.SOUTH);

            if (isPlayer) {
                return cardboarded_conveynience$checkPlayerFilter((Player) entity, tunnelBlockEntity, side, world);
            } else {
                return tunnelBlockEntity.testFlapFilter(side, cardboarded_conveynience$getProjectileItem(entity));
            }
        }

        // Non-brass tunnel - already verified eligible above
        return true;
    }

    @Unique
    private boolean cardboarded_conveynience$checkPlayerFilter(Player player, BrassTunnelBlockEntity tunnelBlockEntity, Direction side, BlockGetter world) {
        if (!CardboardHelper.testForStealthExtended(player)) {
            // Not disguised - test player's held items and armor directly
            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty() && tunnelBlockEntity.testFlapFilter(side, mainHand)) {
                return true;
            }

            ItemStack offHand = player.getOffhandItem();
            if (!offHand.isEmpty() && tunnelBlockEntity.testFlapFilter(side, offHand)) {
                return true;
            }

            for (int i = 0; i < 4; i++) {
                ItemStack armorItem = player.getInventory().armor.get(i);
                if (!armorItem.isEmpty() && tunnelBlockEntity.testFlapFilter(side, armorItem)) {
                    return true;
                }
            }
        } else {
            // Disguised as package - test against first package item
            if (world instanceof Level level) {
                Registry<Item> itemRegistry = level.registryAccess().registryOrThrow(Registries.ITEM);

                Collection<Item> items = itemRegistry.getTag(AllTags.AllItemTags.PACKAGES.tag)
                        .map(tag -> tag.stream().map(Holder::value).toList())
                        .orElse(List.of());

                for (Item item : items) {
                    return tunnelBlockEntity.testFlapFilter(side, new ItemStack(item));
                }
            }
        }
        return false;
    }
}
