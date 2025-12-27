package net.takerudavis.cardboarded_conveynience.forge;

import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;
import net.takerudavis.cardboarded_conveynience.forge.util.ChainConveyorPacketHelper;
import net.takerudavis.cardboarded_conveynience.ponder.CardboardPonderIndex;
import net.takerudavis.cardboarded_conveynience.util.ChuteTeleportHelper;
import net.takerudavis.cardboarded_conveynience.util.DismountHelper;
import net.takerudavis.cardboarded_conveynience.util.InvisiblePackageHelper;
import net.takerudavis.cardboarded_conveynience.util.RouteCache;
import net.takerudavis.cardboarded_conveynience.util.SkyhookHelper;

@Mod(CardboardedConveynience.MOD_ID)
public final class CardboardedConveynienceForge {
    public CardboardedConveynienceForge() {
        // Client-side initialization
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            PonderIndex.addPlugin(new CardboardPonderIndex());

            // Register invisible package models for Frogport animation
            InvisiblePackageHelper.register();

            // Register platform-specific packet sender for dismount
            DismountHelper.registerPacketSender(ChainConveyorPacketHelper::sendStopRidingPacket);

            // Reset client-side state when joining a server (fixes hitbox after disconnect while riding)
            MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
                RouteCache.clearAll();
                if (event.getPlayer() != null) {
                    event.getPlayer().setPose(Pose.STANDING);
                }
            });
        });

        // Server tick: check for players standing on chutes
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END) return;
            if (event.getServer() == null) return;
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                ChuteTeleportHelper.tickPlayer(player);
            }
        });

        // Clean up when player disconnects: reset pose and clear grace period tracking
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            event.getEntity().setPose(Pose.STANDING);
            SkyhookHelper.clearGracePeriod(event.getEntity());
            ChuteTeleportHelper.clearPlayer(event.getEntity());
        });

        CardboardedConveynience.init();
    }
}
