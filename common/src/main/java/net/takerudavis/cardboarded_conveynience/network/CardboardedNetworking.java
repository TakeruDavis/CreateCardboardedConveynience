package net.takerudavis.cardboarded_conveynience.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;

import java.util.List;

/**
 * Handles packet registration and sending for Cardboarded Conveynience.
 * Uses Architectury's NetworkManager API for cross-platform compatibility.
 */
public class CardboardedNetworking {

    // Packet type IDs
    public static final ResourceLocation FROGPORT_ARRIVAL_ID =
        ResourceLocation.fromNamespaceAndPath(CardboardedConveynience.MOD_ID, "frogport_arrival");
    public static final ResourceLocation FROGPORT_ANIMATION_ID =
        ResourceLocation.fromNamespaceAndPath(CardboardedConveynience.MOD_ID, "frogport_animation");
    public static final ResourceLocation ROUTE_QUERY_ID =
        ResourceLocation.fromNamespaceAndPath(CardboardedConveynience.MOD_ID, "route_query");
    public static final ResourceLocation ROUTE_RESPONSE_ID =
        ResourceLocation.fromNamespaceAndPath(CardboardedConveynience.MOD_ID, "route_response");

    private static boolean initialized = false;

    /**
     * Register all packets. Call this during mod initialization.
     * Safe to call multiple times - will only register once.
     */
    public static void init() {
        if (initialized) {
            CardboardedConveynience.LOGGER.debug("Networking already initialized, skipping");
            return;
        }
        initialized = true;

        CardboardedConveynience.LOGGER.debug("Registering network packets...");

        // C2S: Client tells server about Frogport arrival
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            FROGPORT_ARRIVAL_ID,
            (buf, context) -> {
                FrogportArrivalPacket packet = new FrogportArrivalPacket(buf);
                context.queue(() -> packet.handle(context));
            }
        );

        // S2C: Server tells clients to play animation
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            FROGPORT_ANIMATION_ID,
            (buf, context) -> {
                FrogportAnimationS2CPacket packet = new FrogportAnimationS2CPacket(buf);
                context.queue(() -> packet.handle(context));
            }
        );

        // C2S: Client queries route for upcoming junction
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            ROUTE_QUERY_ID,
            (buf, context) -> {
                RouteQueryC2SPacket packet = new RouteQueryC2SPacket(buf);
                context.queue(() -> packet.handle(context));
            }
        );

        // S2C: Server responds with route for junction
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            ROUTE_RESPONSE_ID,
            (buf, context) -> {
                RouteResponseS2CPacket packet = new RouteResponseS2CPacket(buf);
                context.queue(() -> packet.handle(context));
            }
        );

        CardboardedConveynience.LOGGER.debug("Network packets registered successfully");
    }

    /**
     * Create a buffer for sending packets.
     */
    private static RegistryFriendlyByteBuf createBuffer(RegistryAccess registryAccess) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
    }

    /**
     * Send a Frogport arrival notification to the server.
     * Call this from the client when arriving at a matching Frogport.
     */
    public static void sendFrogportArrival(BlockPos frogportPos) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;

        RegistryFriendlyByteBuf buf = createBuffer(mc.level.registryAccess());
        buf.writeBlockPos(frogportPos);
        NetworkManager.sendToServer(FROGPORT_ARRIVAL_ID, buf);
    }

    /**
     * Broadcast a Frogport animation packet to all players within range.
     * Call this from the server after validating the arrival.
     */
    public static void broadcastFrogportAnimation(ServerLevel level, BlockPos frogportPos, double radius) {
        Vec3 center = frogportPos.getCenter();

        // Find all players within range and send the packet
        List<ServerPlayer> nearbyPlayers = level.getPlayers(player ->
            player.position().distanceTo(center) <= radius
        );

        for (ServerPlayer player : nearbyPlayers) {
            RegistryFriendlyByteBuf buf = createBuffer(level.registryAccess());
            buf.writeBlockPos(frogportPos);
            NetworkManager.sendToPlayer(player, FROGPORT_ANIMATION_ID, buf);
        }
    }

    /**
     * Send a route query to the server for an upcoming junction.
     * Call this from the client when starting to travel toward a junction.
     */
    public static void sendRouteQuery(BlockPos junctionPos, String destinationAddress) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;

        RegistryFriendlyByteBuf buf = createBuffer(mc.level.registryAccess());
        buf.writeBlockPos(junctionPos);
        buf.writeUtf(destinationAddress);
        NetworkManager.sendToServer(ROUTE_QUERY_ID, buf);
    }

    /**
     * Send a route response to a specific player.
     * Call this from the server after processing a route query.
     */
    public static void sendRouteResponse(ServerPlayer player, BlockPos junctionPos, BlockPos routeConnection) {
        RegistryFriendlyByteBuf buf = createBuffer(player.level().registryAccess());
        buf.writeBlockPos(junctionPos);
        buf.writeBlockPos(routeConnection);
        NetworkManager.sendToPlayer(player, ROUTE_RESPONSE_ID, buf);
    }
}
