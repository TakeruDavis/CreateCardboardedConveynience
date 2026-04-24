package net.takerudavis.cardboarded_conveynience.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.takerudavis.cardboarded_conveynience.util.ISwimAmountSetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySwimAmountAccessor implements ISwimAmountSetter {

    @Shadow
    protected float swimAmount;

    @Shadow
    protected float swimAmountO;

    @Override
    public void cardboarded_conveynience$setSwimAmount(float value) {
        this.swimAmount = value;
        this.swimAmountO = value;
    }
}
