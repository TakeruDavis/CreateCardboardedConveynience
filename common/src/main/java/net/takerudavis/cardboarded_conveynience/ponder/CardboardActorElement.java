package net.takerudavis.cardboarded_conveynience.ponder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.element.ParrotElementImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.takerudavis.cardboarded_conveynience.ponder.PonderSceneHelper.createItem;

/**
 * A Ponder element that renders a player-in-cardboard-armour on a chain conveyor.
 *
 * Extends ParrotElementImpl to inherit the movement infrastructure
 * (moveParrot, setPositionOffset, sub-tick lerp via entity.xo/yo/zo).
 * The phantom Parrot entity drives position interpolation but is never rendered —
 * we render an ArmorStand with full cardboard gear instead.
 *
 * A sine-wave sway around the waist (matching Create's own parrot-on-conveyor effect)
 * is applied in renderLast.
 */
class CardboardActorElement extends ParrotElementImpl {

    private ArmorStand armorStand;
    private final Consumer<ArmorStand> configure;

    CardboardActorElement(Vec3 location, Supplier<? extends ParrotPose> pose) {
        this(location, pose, stand -> {});
    }

    CardboardActorElement(Vec3 location, Supplier<? extends ParrotPose> pose,
                          Consumer<ArmorStand> configure) {
        super(location, pose);
        this.configure = configure;
    }

    @Override
    protected void renderLast(PonderLevel world, MultiBufferSource buffer,
                               GuiGraphics graphics, float fade, float pt) {
        // Phantom parrot: initialised by tick(), used here only for position lerp
        if (entity == null) {
            entity = pose.create(world);
            entity.setYRot(entity.yRotO = 180);
        }

        // Armour stand: the actual visual, created once and reused each frame
        if (armorStand == null) {
            armorStand = new ArmorStand(EntityType.ARMOR_STAND, world);
            armorStand.setYRot(armorStand.yRotO = 180);
            armorStand.setShowArms(true);
            armorStand.setNoBasePlate(true);
            armorStand.setItemSlot(EquipmentSlot.HEAD,  createItem("cardboard_helmet"));
            armorStand.setItemSlot(EquipmentSlot.CHEST, createItem("cardboard_chestplate"));
            armorStand.setItemSlot(EquipmentSlot.LEGS,  createItem("cardboard_leggings"));
            armorStand.setItemSlot(EquipmentSlot.FEET,  createItem("cardboard_boots"));
            configure.accept(armorStand);
        }

        // Sub-tick interpolated position and facing from the phantom parrot
        double lx = Mth.lerp(pt, entity.xo, entity.getX());
        double ly = Mth.lerp(pt, entity.yo, entity.getY());
        double lz = Mth.lerp(pt, entity.zo, entity.getZ());
        float yaw = AngleHelper.angleLerp(pt, entity.yRotO, entity.getYRot());

        PoseStack ps = graphics.pose();
        EntityRenderDispatcher erd = Minecraft.getInstance().getEntityRenderDispatcher();

        ps.pushPose();
        ps.translate(location.x + lx, location.y + ly, location.z + lz);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));

        // Riding sway — oscillates ±10° around waist (ArmorStand Y-centre ≈ 1.0)
        float swayTime = (world.scene != null ? world.scene.getCurrentTime() : entity.tickCount) + pt;
        ps.translate(0, 1.0, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.sin(swayTime * 0.2f) * 10));
        ps.translate(0, -1.0, 0);

        erd.render(armorStand, 0, 0, 0, 0, pt, ps, buffer, lightCoordsFromFade(fade));
        ps.popPose();
    }
}
