package net.takerudavis.cardboarded_conveynience.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;
import net.takerudavis.cardboarded_conveynience.advancement.ModCriteria;
import net.takerudavis.cardboarded_conveynience.util.CardboardHelper;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;

import java.util.function.Supplier;

/**
 * Client-to-server packet sent when a player in cardboard armor arrives at a Frogport.
 * The server validates proximity and broadcasts an S2C packet to all nearby clients
 * to trigger the animation client-side (avoiding server-side item spawning).
 */
public class FrogportArrivalPacket {

    private final BlockPos frogportPos;

    // Radius for broadcasting animation to nearby players
    private static final double BROADCAST_RADIUS = 64.0;

    public FrogportArrivalPacket(BlockPos frogportPos) {
        this.frogportPos = frogportPos;
    }

    public FrogportArrivalPacket(FriendlyByteBuf buf) {
        this.frogportPos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(frogportPos);
    }

    public void apply(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();

        // Queue work on server thread
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            // Validate player is wearing cardboard armor
            if (!CardboardHelper.testForArmor(serverPlayer)) {
                CardboardedConveynience.LOGGER.warn(
                    "[FrogportArrival] Player {} not wearing cardboard armor, ignoring packet",
                    serverPlayer.getName().getString()
                );
                return;
            }

            // Validate Frogport exists
            if (!(serverPlayer.level() instanceof ServerLevel serverLevel)) {
                return;
            }

            BlockEntity be = serverLevel.getBlockEntity(frogportPos);
            if (!(be instanceof FrogportBlockEntity)) {
                CardboardedConveynience.LOGGER.warn(
                    "[FrogportArrival] No Frogport at {}, ignoring packet",
                    frogportPos
                );
                return;
            }

            // Validate player is reasonably close to the Frogport
            double distance = serverPlayer.position().distanceTo(
                frogportPos.getCenter()
            );
            if (distance > 10.0) {  // Generous range to account for network lag
                CardboardedConveynience.LOGGER.warn(
                    "[FrogportArrival] Player {} too far from Frogport ({}), ignoring packet",
                    serverPlayer.getName().getString(), distance
                );
                return;
            }

            // Broadcast S2C packet to all nearby players to trigger client-side animation
            CardboardedNetworking.broadcastFrogportAnimation(serverLevel, frogportPos, BROADCAST_RADIUS);

            // Grant the "Just a Nermal Delivery" advancement
            ModCriteria.FROGPORT_DELIVERY.trigger(serverPlayer);
        });
    }
}
