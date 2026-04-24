package net.takerudavis.cardboarded_conveynience.util;

/**
 * Duck-typed interface implemented on LivingEntity via mixin to expose
 * write access to the package-private swimAmount / swimAmountO fields.
 */
public interface ISwimAmountSetter {
    void cardboarded_conveynience$setSwimAmount(float value);
}
