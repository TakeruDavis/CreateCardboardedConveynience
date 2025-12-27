package net.takerudavis.cardboarded_conveynience.fabric;

import net.createmod.ponder.foundation.PonderIndex;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.world.entity.Pose;
import net.takerudavis.cardboarded_conveynience.fabric.util.ChainConveyorPacketHelper;
import net.takerudavis.cardboarded_conveynience.ponder.CardboardPonderIndex;
import net.takerudavis.cardboarded_conveynience.util.DismountHelper;
import net.takerudavis.cardboarded_conveynience.util.InvisiblePackageHelper;
import net.takerudavis.cardboarded_conveynience.util.RouteCache;

public final class CardboardedConveynienceFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PonderIndex.addPlugin(new CardboardPonderIndex());

        // Register invisible package models for Frogport animation
        InvisiblePackageHelper.register();

        // Register platform-specific packet sender for dismount
        DismountHelper.registerPacketSender(ChainConveyorPacketHelper::sendStopRidingPacket);

        // Reset client-side state when joining a server (fixes hitbox after disconnect while riding)
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            RouteCache.clearAll();
            if (client.player != null) {
                client.player.setPose(Pose.STANDING);
            }
        });
    }
}
