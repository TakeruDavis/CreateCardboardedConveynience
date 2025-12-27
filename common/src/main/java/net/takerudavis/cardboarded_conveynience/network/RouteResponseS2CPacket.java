package net.takerudavis.cardboarded_conveynience.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.takerudavis.cardboarded_conveynience.util.RouteCache;

import java.util.function.Supplier;

/**
 * Server-to-client packet containing the route response for a junction.
 * The client caches this for use when arriving at the junction.
 */
public class RouteResponseS2CPacket {

    private final BlockPos junctionPos;
    private final BlockPos routeConnection;

    public RouteResponseS2CPacket(BlockPos junctionPos, BlockPos routeConnection) {
        this.junctionPos = junctionPos;
        this.routeConnection = routeConnection;
    }

    public RouteResponseS2CPacket(FriendlyByteBuf buf) {
        this.junctionPos = buf.readBlockPos();
        this.routeConnection = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(junctionPos);
        buf.writeBlockPos(routeConnection);
    }

    public void apply(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();

        context.queue(() -> {
            // Cache the route for use when we arrive at this junction
            RouteCache.cacheRoute(junctionPos, routeConnection);
        });
    }
}
