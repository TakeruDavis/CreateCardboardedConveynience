package net.takerudavis.cardboarded_conveynience.util;

import net.minecraft.core.BlockPos;

import java.util.function.Consumer;

/**
 * Helper for sending dismount packets. Platform-specific implementations
 * register their packet sender during client init.
 */
public class DismountHelper {

    private static Consumer<BlockPos> stopRidingPacketSender = null;

    /**
     * Register the platform-specific packet sender.
     * Call this during client initialization.
     */
    public static void registerPacketSender(Consumer<BlockPos> sender) {
        stopRidingPacketSender = sender;
    }

    /**
     * Send a stop riding packet to the server.
     * This triggers proper server-side cleanup including pose/hitbox fix.
     */
    public static void sendStopRidingPacket(BlockPos conveyorPos) {
        if (stopRidingPacketSender != null && conveyorPos != null) {
            stopRidingPacketSender.accept(conveyorPos);
        }
    }
}
