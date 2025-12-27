package net.takerudavis.cardboarded_conveynience.network;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;

import java.util.function.Supplier;

/**
 * Client-to-server packet to query the route for a destination at a specific junction.
 * Sent preemptively when the player starts traveling toward a junction.
 */
public class RouteQueryC2SPacket {

    private final BlockPos junctionPos;
    private final String destinationAddress;

    public RouteQueryC2SPacket(BlockPos junctionPos, String destinationAddress) {
        this.junctionPos = junctionPos;
        this.destinationAddress = destinationAddress;
    }

    public RouteQueryC2SPacket(FriendlyByteBuf buf) {
        this.junctionPos = buf.readBlockPos();
        this.destinationAddress = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(junctionPos);
        buf.writeUtf(destinationAddress);
    }

    public void apply(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();

        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            if (!(serverPlayer.level() instanceof ServerLevel serverLevel)) {
                return;
            }

            // Get the chain conveyor block entity at the junction
            BlockEntity be = serverLevel.getBlockEntity(junctionPos);
            if (!(be instanceof ChainConveyorBlockEntity conveyor)) {
                CardboardedConveynience.LOGGER.debug(
                    "[RouteQuery] No chain conveyor at {}", junctionPos
                );
                // Send empty response
                CardboardedNetworking.sendRouteResponse(serverPlayer, junctionPos, BlockPos.ZERO);
                return;
            }

            // Query the server-side routing table
            ItemStack fakePackage = new ItemStack(Items.PAPER);
            PackageItem.addAddress(fakePackage, destinationAddress);
            BlockPos routeConnection = conveyor.routingTable.getExitFor(fakePackage);

            CardboardedConveynience.LOGGER.debug(
                "[RouteQuery] Junction {} for '{}' -> route: {}",
                junctionPos, destinationAddress, routeConnection
            );

            // Send the route back to the client
            CardboardedNetworking.sendRouteResponse(serverPlayer, junctionPos, routeConnection);
        });
    }
}
