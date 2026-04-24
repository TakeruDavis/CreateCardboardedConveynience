package net.takerudavis.cardboarded_conveynience.neoforge;

import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;
import net.takerudavis.cardboarded_conveynience.advancement.ModCriteria;
import net.takerudavis.cardboarded_conveynience.ponder.CardboardPonderIndex;
import net.takerudavis.cardboarded_conveynience.util.ChuteTeleportHelper;
import net.takerudavis.cardboarded_conveynience.util.InvisiblePackageHelper;
import net.takerudavis.cardboarded_conveynience.util.RouteCache;
import net.takerudavis.cardboarded_conveynience.util.SkyhookHelper;

@Mod(CardboardedConveynience.MOD_ID)
public final class CardboardedConveynienceNeoForge {

    public CardboardedConveynienceNeoForge(IEventBus modEventBus) {
        // Register criteria triggers via RegisterEvent like Create does
        ModCriteriaNeoForge.register(modEventBus);

        // Set suppliers so common code can access the triggers
        ModCriteria.setSuppliers(
            () -> ModCriteriaNeoForge.DISGUISE_AS_PACKAGE,
            () -> ModCriteriaNeoForge.FROGPORT_DELIVERY,
            () -> ModCriteriaNeoForge.CHUTE_TELEPORT
        );

        // Client-side initialization
        if (FMLEnvironment.dist == Dist.CLIENT) {
            initClient();
        }

        // Register server-side events
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);

        CardboardedConveynience.init();
    }

    private void initClient() {
        PonderIndex.addPlugin(new CardboardPonderIndex());

        // Register invisible package models for Frogport animation
        InvisiblePackageHelper.register();

        // Register client events
        NeoForge.EVENT_BUS.addListener(this::onClientLogin);
    }

    private void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        RouteCache.clearAll();
        if (event.getPlayer() != null) {
            event.getPlayer().setPose(Pose.STANDING);
        }
    }

    private void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer() == null) return;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            ChuteTeleportHelper.tickPlayer(player);
        }
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        event.getEntity().setPose(Pose.STANDING);
        SkyhookHelper.clearGracePeriod(event.getEntity());
        ChuteTeleportHelper.clearPlayer(event.getEntity());
    }
}
