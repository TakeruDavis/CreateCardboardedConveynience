package net.takerudavis.cardboarded_conveynience.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;
import net.takerudavis.cardboarded_conveynience.util.FrogportAnimationHelper;
import net.takerudavis.cardboarded_conveynience.util.InvisiblePackageHelper;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;

import java.util.function.Supplier;

/**
 * Server-to-client packet to trigger Frogport tongue animation on all nearby clients.
 * This avoids server-side item spawning by having each client animate locally.
 */
public class FrogportAnimationS2CPacket {

    private final BlockPos frogportPos;

    public FrogportAnimationS2CPacket(BlockPos frogportPos) {
        this.frogportPos = frogportPos;
    }

    public FrogportAnimationS2CPacket(FriendlyByteBuf buf) {
        this.frogportPos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(frogportPos);
    }

    public void apply(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();

        // Queue work on client thread
        context.queue(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }

            BlockEntity be = mc.level.getBlockEntity(frogportPos);
            if (!(be instanceof FrogportBlockEntity frogport)) {
                CardboardedConveynience.LOGGER.warn(
                    "[FrogportAnimation] No Frogport at {} on client",
                    frogportPos
                );
                return;
            }

            // Trigger animation client-side with invisible package
            FrogportAnimationHelper.setBypassPackageCheck(true);
            frogport.startAnimation(InvisiblePackageHelper.getInvisiblePackage(), false);
        });
    }
}
