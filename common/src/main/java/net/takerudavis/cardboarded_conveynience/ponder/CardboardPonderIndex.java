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
            .addStoryBoard("cardboard/disguise", CardboardScenes::disguise);
    }
}
