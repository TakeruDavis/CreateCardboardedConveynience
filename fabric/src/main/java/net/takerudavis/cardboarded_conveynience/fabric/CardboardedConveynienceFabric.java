package net.takerudavis.cardboarded_conveynience.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;
import net.takerudavis.cardboarded_conveynience.util.ChuteTeleportHelper;
import net.takerudavis.cardboarded_conveynience.util.SkyhookHelper;

public final class CardboardedConveynienceFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CardboardedConveynience.init();

        // Server tick: check for players standing on chutes
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ChuteTeleportHelper.tickPlayer(player);
            }
        });

        // Clean up when player disconnects: reset pose and clear grace period tracking
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            handler.getPlayer().setPose(Pose.STANDING);
            SkyhookHelper.clearGracePeriod(handler.getPlayer());
            ChuteTeleportHelper.clearPlayer(handler.getPlayer());
        });
    }
}
