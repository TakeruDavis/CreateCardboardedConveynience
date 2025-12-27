package net.takerudavis.cardboarded_conveynience.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;

/**
 * Triggers when a player rides a chain conveyor while wearing full cardboard armor.
 */
public class DisguiseTrigger extends SimpleCriterionTrigger<DisguiseTrigger.TriggerInstance> {

    public static final ResourceLocation ID = new ResourceLocation(
            CardboardedConveynience.MOD_ID, "disguise_as_package"
    );

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate predicate,
                                              DeserializationContext context) {
        return new TriggerInstance(predicate);
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {

        public TriggerInstance(ContextAwarePredicate predicate) {
            super(ID, predicate);
        }

        public static TriggerInstance disguised() {
            return new TriggerInstance(ContextAwarePredicate.ANY);
        }
    }
}
