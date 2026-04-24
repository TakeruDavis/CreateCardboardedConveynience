package net.takerudavis.cardboarded_conveynience.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;

public class CardboardPonderIndex implements PonderPlugin {

    @Override
    public String getModId() {
        return CardboardedConveynience.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(new ResourceLocation("create", "chain_conveyor"))
            .addStoryBoard("cardboard/disguise", DisguiseScene::disguise,
                sb -> sb.orderAfter("create", "high_logistics/chain_conveyor"));

        helper.forComponents(
                new ResourceLocation("create", "andesite_tunnel"),
                new ResourceLocation("create", "brass_tunnel"))
            .addStoryBoard("cardboard/belt_tunnel", BeltTunnelScene::beltTunnel,
                sb -> sb.orderAfter("create", "tunnels/andesite")
                        .orderAfter("create", "tunnels/brass_modes"));

        helper.forComponents(
                new ResourceLocation("create", "chute"),
                new ResourceLocation("create", "smart_chute"))
            .addStoryBoard("cardboard/chute", ChuteTeleportScene::chuteTeleport,
                sb -> sb.orderAfter("create", "chute/smart"));

        helper.forComponents(new ResourceLocation("create", "package_frogport"))
            .addStoryBoard("cardboard/frogport", FrogportDeliveryScene::frogportDelivery,
                sb -> sb.orderAfter("create", "high_logistics/package_frogport"));
    }
}
