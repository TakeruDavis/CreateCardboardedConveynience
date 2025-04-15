package net.TakeruDavis.create_cardboarded_conveynience.mixin;

import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelShapes;
import net.TakeruDavis.create_cardboarded_conveynience.utils.CardboardHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        if (!(entity instanceof Player player)) return;
        if (!CardboardHelper.testForArmor(player)) return;

        VoxelShape voxelShape = BeltTunnelShapes.getShape(state);

        VoxelShape innerBox = box(2, -5, 2, 14, 10, 14);

        voxelShape = Shapes.join(voxelShape, innerBox, BooleanOp.ONLY_FIRST);

        cir.setReturnValue(voxelShape);
    }

}
