package net.takerudavis.cardboarded_conveynience.network;

import dev.architectury.networking.NetworkChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;

import java.util.List;

/**
 * Handles packet registration and sending for Cardboarded Conveynience.
 * Uses Architectury's NetworkChannel for cross-platform compatibility.
 */
public class CardboardedNetworking {

    public static final NetworkChannel CHANNEL = NetworkChannel.create(
        new ResourceLocation(CardboardedConveynience.MOD_ID, "main")
    );

    private static boolean initialized = false;

    /**
     * Register all packets. Call this during mod initialization.
     * Safe to call multiple times - will only register once.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        // C2S: Client tells server about Frogport arrival
        CHANNEL.register(
            FrogportArrivalPacket.class,
            FrogportArrivalPacket::encode,
            FrogportArrivalPacket::new,
            FrogportArrivalPacket::apply
        );

        // S2C: Server tells clients to play animation
        CHANNEL.register(
            FrogportAnimationS2CPacket.class,
            FrogportAnimationS2CPacket::encode,
            FrogportAnimationS2CPacket::new,
            FrogportAnimationS2CPacket::apply
        );

        // C2S: Client queries route for upcoming junction
        CHANNEL.register(
            RouteQueryC2SPacket.class,
            RouteQueryC2SPacket::encode,
            RouteQueryC2SPacket::new,
            RouteQueryC2SPacket::apply
        );

        // S2C: Server responds with route for junction
        CHANNEL.register(
            RouteResponseS2CPacket.class,
            RouteResponseS2CPacket::encode,
            RouteResponseS2CPacket::new,
            RouteResponseS2CPacket::apply
        );
    }

    /**
     * Send a Frogport arrival notification to the server.
     * Call this from the client when arriving at a matching Frogport.
     */
    public static void sendFrogportArrival(BlockPos frogportPos) {
        CHANNEL.sendToServer(new FrogportArrivalPacket(frogportPos));
    }

    /**
     * Broadcast a Frogport animation packet to all players within range.
     * Call this from the server after validating the arrival.
     */
    public static void broadcastFrogportAnimation(ServerLevel level, BlockPos frogportPos, double radius) {
        Vec3 center = frogportPos.getCenter();
        FrogportAnimationS2CPacket packet = new FrogportAnimationS2CPacket(frogportPos);

        // Find all players within range and send the packet
        List<ServerPlayer> nearbyPlayers = level.getPlayers(player ->
            player.position().distanceTo(center) <= radius
        );

        for (ServerPlayer player : nearbyPlayers) {
            CHANNEL.sendToPlayer(player, packet);
        }
    }

    /**
     * Send a route query to the server for an upcoming junction.
     * Call this from the client when starting to travel toward a junction.
     */
    public static void sendRouteQuery(BlockPos junctionPos, String destinationAddress) {
        CHANNEL.sendToServer(new RouteQueryC2SPacket(junctionPos, destinationAddress));
    }

    /**
     * Send a route response to a specific player.
     * Call this from the server after processing a route query.
     */
    public static void sendRouteResponse(ServerPlayer player, BlockPos junctionPos, BlockPos routeConnection) {
        CHANNEL.sendToPlayer(player, new RouteResponseS2CPacket(junctionPos, routeConnection));
    }
}
