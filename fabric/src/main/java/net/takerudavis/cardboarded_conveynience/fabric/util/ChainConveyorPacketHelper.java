package net.takerudavis.cardboarded_conveynience.fabric.util;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.kinetics.chainConveyor.ServerboundChainConveyorRidingPacket;
import net.minecraft.core.BlockPos;

/**
 * Fabric-specific helper for sending chain conveyor packets.
 */
public class ChainConveyorPacketHelper {

    /**
     * Send a stop riding packet to the server.
     * This triggers ServerChainConveyorHandler.handleStopRidingPacket which properly
     * clears the hanging state and fixes the player's pose/hitbox.
     */
    public static void sendStopRidingPacket(BlockPos conveyorPos) {
        if (conveyorPos != null) {
            AllPackets.getChannel()
                .sendToServer(new ServerboundChainConveyorRidingPacket(conveyorPos, true));
        }
    }
}
