package net.takerudavis.cardboarded_conveynience.ponder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A Ponder element that renders a skeleton in dyed leather armour, togglable
 * between standing and crawling (face-down horizontal) poses.
 *
 * Like {@link CardboardActorElement}, extends ParrotElementImpl for movement
 * infrastructure. When crawling, applies a 90° X rotation and swim animation.
 */
class CrawlingActorElement extends ParrotElementImpl {

    private static final int LEATHER_COLOR = 0x4A3728;
    private static final float CRAWL_Y_OFFSET = -0.4f;

    private Skeleton skeleton;
    private final Consumer<Mob> configure;
    private boolean crawling = false;
    private float bodyYRotOffset = 0;
    private float prevBodyYRotOffset = 0;

    CrawlingActorElement(Vec3 location, Supplier<? extends ParrotPose> pose) {
        this(location, pose, mob -> {});
    }

    CrawlingActorElement(Vec3 location, Supplier<? extends ParrotPose> pose,
                         Consumer<Mob> configure) {
        super(location, pose);
        this.configure = configure;
    }

    void setCrawling(boolean crawling) {
        this.crawling = crawling;
        if (skeleton != null) {
            setSwimAmount(crawling ? 1.0f : 0.0f);
        }
    }

    void setBodyYRotOffset(float offset) {
        this.prevBodyYRotOffset = this.bodyYRotOffset;
        this.bodyYRotOffset = offset;
    }

    @Override
    protected void renderLast(PonderLevel world, MultiBufferSource buffer,
                               GuiGraphics graphics, float fade, float pt) {
        if (entity == null) {
            entity = pose.create(world);
            entity.setYRot(entity.yRotO = 0);
        }

        if (skeleton == null) {
            skeleton = new Skeleton(EntityType.SKELETON, world);
            skeleton.setYRot(skeleton.yRotO = 0);
            skeleton.setNoGravity(true);
            skeleton.setItemSlot(EquipmentSlot.HEAD,     dyedLeather(Items.LEATHER_HELMET));
            skeleton.setItemSlot(EquipmentSlot.CHEST,    dyedLeather(Items.LEATHER_CHESTPLATE));
            skeleton.setItemSlot(EquipmentSlot.LEGS,     dyedLeather(Items.LEATHER_LEGGINGS));
            skeleton.setItemSlot(EquipmentSlot.FEET,     dyedLeather(Items.LEATHER_BOOTS));
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            setSwimAmount(crawling ? 1.0f : 0.0f);
            configure.accept(skeleton);
        }

        double lx = Mth.lerp(pt, entity.xo, entity.getX());
        double ly = Mth.lerp(pt, entity.yo, entity.getY());
        double lz = Mth.lerp(pt, entity.zo, entity.getZ());

        PoseStack ps = graphics.pose();
        EntityRenderDispatcher erd = Minecraft.getInstance().getEntityRenderDispatcher();

        ps.pushPose();

        if (crawling) {
            ps.translate(location.x + lx, location.y + ly + CRAWL_Y_OFFSET, location.z + lz);

            // Lay the skeleton face-down, head-first into +Z (tunnel direction).
            ps.translate(0, 0.5, 0);
            ps.mulPose(Axis.YP.rotationDegrees(180));
            ps.mulPose(Axis.XP.rotationDegrees(-90));
            ps.translate(0, -0.5, 0);

            // Drive swim animation from Ponder's scene clock.
            float sceneTime = world.scene != null ? world.scene.getCurrentTime() : 0;
            skeleton.tickCount = (int) sceneTime;
            skeleton.walkAnimation.update(0.02f, 0.15f);
        } else {
            ps.translate(location.x + lx, location.y + ly, location.z + lz);
        }

        // The renderer internally applies mulPose(YP, 180 - bodyYRot).
        // Crawling: PoseStack already has 180° Y, so bodyYRot=180 → net 0°.
        // Standing: no PoseStack Y, so bodyYRot=0 → renderer applies 180° → faces +Z.
        float base = crawling ? 180 : 0;
        float bodyYRot = base + bodyYRotOffset;
        float prevBodyYRot = base + prevBodyYRotOffset;
        skeleton.setYBodyRot(bodyYRot);
        skeleton.yBodyRotO = prevBodyYRot;
        skeleton.setYHeadRot(bodyYRot);
        skeleton.yHeadRotO = prevBodyYRot;
        erd.render(skeleton, 0, 0, 0, 0, pt, ps, buffer, lightCoordsFromFade(fade));
        ps.popPose();
    }

    private void setSwimAmount(float value) {
        ((net.takerudavis.cardboarded_conveynience.util.ISwimAmountSetter) skeleton)
            .cardboarded_conveynience$setSwimAmount(value);
    }

    private static ItemStack dyedLeather(Item item) {
        ItemStack stack = new ItemStack(item);
        ((net.minecraft.world.item.DyeableLeatherItem) stack.getItem()).setColor(stack, LEATHER_COLOR);
        return stack;
    }
}
