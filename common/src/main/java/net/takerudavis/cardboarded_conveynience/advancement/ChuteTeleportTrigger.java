package net.takerudavis.cardboarded_conveynience.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Triggers when a player in cardboard armor teleports through a chute.
 * "Express Delivery" - pneumatic tube style instant transit.
 */
public class ChuteTeleportTrigger extends SimpleCriterionTrigger<ChuteTeleportTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
            ).apply(instance, TriggerInstance::new)
        );

        public static TriggerInstance teleported() {
            return new TriggerInstance(Optional.empty());
        }

        @Override
        public Optional<ContextAwarePredicate> player() {
            return player;
        }
    }
}
