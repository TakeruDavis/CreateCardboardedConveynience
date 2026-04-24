package net.TakeruDavis.create_cardboarded_conveynience.mixin;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.armor.CardboardArmorHandler;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelShapes;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

import static net.minecraft.world.level.block.Block.box;

@Mixin(BlockBehaviour.class)
public class BeltTunnelBlockMixin {

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectCollisionShape(
            BlockState state,
            BlockGetter world,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (!(state.getBlock() instanceof BeltTunnelBlock)) return;
        if (!(context instanceof EntityCollisionContext entityContext)) return;
        Entity entity = entityContext.getEntity();
        if (entity == null) return;

        boolean matchesFilter = false;

        double deltaX = (entity.getBlockX() - pos.getX());
        double deltaY = (entity.getBlockY() - pos.getY() + 0.5);
        double deltaZ = (entity.getBlockZ() - pos.getZ());

        if (Math.abs(deltaY) > 0.5) {
            // entity is above or below the tunnel, do not allow them to pass through
            return;
        }

        HashMap<Class, ItemStack> allowedProjectiles = new HashMap<>();
        allowedProjectiles.put(FireworkRocketEntity.class, new ItemStack(Items.FIREWORK_ROCKET));
        allowedProjectiles.put(ThrownEnderpearl.class, new ItemStack(Items.ENDER_PEARL));
        allowedProjectiles.put(ThrownPotion.class, new ItemStack(Items.SPLASH_POTION));
        allowedProjectiles.put(Snowball.class, new ItemStack(Items.SNOWBALL));

        BlockPos entityPos = BlockPos.containing(entity.getEyePosition());
        if (!(world.getBlockState(entityPos).getBlock() instanceof BeltTunnelBlock)) {
            if (state.getBlock() == AllBlocks.BRASS_TUNNEL.get() && world.getBlockEntity(pos) instanceof BrassTunnelBlockEntity tunnelBlockEntity) {
                Direction side = Math.abs(deltaX) > Math.abs(deltaZ)
                    ? (deltaX < 0 ? Direction.WEST : Direction.EAST)
                    : (deltaZ < 0 ? Direction.NORTH : Direction.SOUTH);

                if (entity instanceof Player player) {
                    HashMap<String, ItemStack> testedItems = new HashMap<>();

                    if (!CardboardArmorHandler.testForStealth(player)) {
                        testedItems.put("Main hand", player.getMainHandItem());
                        testedItems.put("Off hand", player.getOffhandItem());
                        for (int i = 0; i < 4; i++) {
                            ItemStack armorItem = player.getInventory().armor.get(i);
                            testedItems.put("Armor " + i, armorItem);
                        }
                    } else {
                        if (world instanceof Level level) {
                            Registry<Item> itemRegistry = level.registryAccess().registryOrThrow(Registries.ITEM);

                            Collection<Item> items = itemRegistry.getTag(AllTags.AllItemTags.PACKAGES.tag)
                                    .map(tag -> tag.stream().map(Holder::value).toList())
                                    .orElse(List.of());

                            for (Item item : items) {
                                testedItems.put("Disguise", new ItemStack(item));
                                //one should be enough
                                break;
                            }
                        }
                    }

                    for (String testedSlot : testedItems.keySet()) {
                        if (!testedItems.get(testedSlot).isEmpty()) {
                            if (tunnelBlockEntity.testFlapFilter(side, testedItems.get(testedSlot))) {
                                matchesFilter = true;
                                break;
                            }
                        }
                    }
                } else if (entity instanceof Projectile projectile && allowedProjectiles.containsKey(projectile.getClass())) {
                    matchesFilter = tunnelBlockEntity.testFlapFilter(side, allowedProjectiles.get(projectile.getClass()));
                }
            } else {
                //if tunnel is not brass, but are supported, allow them pass through
                matchesFilter = entity instanceof Player || allowedProjectiles.containsKey(entity.getClass());
            }
        } else {
            // entity is already in the tunnel, allow them to pass through
            matchesFilter = true;
        }

        if (!matchesFilter) return;

        VoxelShape voxelShape = BeltTunnelShapes.getShape(state);

        VoxelShape innerBox = box(2, -5, 2, 14, 10, 14);

        voxelShape = Shapes.join(voxelShape, innerBox, BooleanOp.ONLY_FIRST);

        cir.setReturnValue(voxelShape);
    }

}
