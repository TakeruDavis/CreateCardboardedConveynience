package net.takerudavis.cardboarded_conveynience.ponder;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.element.ElementLinkImpl;
import net.createmod.ponder.foundation.instruction.CreateParrotInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.takerudavis.cardboarded_conveynience.util.InvisiblePackageHelper;

import java.util.function.Consumer;

class PonderSceneHelper {

    static ItemStack createItem(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("create", id)));
    }

    static ElementLink<EntityElement> spawnActor(SceneBuilder scene, Vec3 pos, float yRot, Consumer<ArmorStand> configure) {
        return scene.world().createEntity(level -> {
            ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
            stand.setPos(pos.x, pos.y, pos.z);
            stand.setYRot(yRot);
            stand.setShowArms(true);
            stand.setNoBasePlate(true);
            stand.setItemSlot(EquipmentSlot.HEAD, createItem("cardboard_helmet"));
            stand.setItemSlot(EquipmentSlot.CHEST, createItem("cardboard_chestplate"));
            stand.setItemSlot(EquipmentSlot.LEGS, createItem("cardboard_leggings"));
            stand.setItemSlot(EquipmentSlot.FEET, createItem("cardboard_boots"));
            configure.accept(stand);
            return stand;
        });
    }

    static ElementLink<EntityElement> spawnActor(SceneBuilder scene, Vec3 pos, float yRot) {
        return spawnActor(scene, pos, yRot, stand -> {});
    }

    static Consumer<ArmorStand> crouching() {
        return stand -> {
            stand.setRightLegPose(new Rotations(-10, 0, 0));
            stand.setLeftLegPose(new Rotations(-10, 0, 0));
        };
    }

    /** Reset all poses to the default upright stance. */
    static Consumer<ArmorStand> standing() {
        return stand -> {
            stand.setBodyPose(new Rotations(0, 0, 0));
            stand.setHeadPose(new Rotations(0, 0, 0));
            stand.setRightArmPose(new Rotations(-10, 0, -10));
            stand.setLeftArmPose(new Rotations(-10, 0, 10));
            stand.setRightLegPose(new Rotations(-1, 0, -1));
            stand.setLeftLegPose(new Rotations(-1, 0, 1));
        };
    }

    /** Lay the actor flat — simulates crawling through a 1-block-high gap. */
    static Consumer<ArmorStand> crawling() {
        return stand -> {
            stand.setBodyPose(new Rotations(90, 0, 0));
            stand.setHeadPose(new Rotations(-90, 0, 0));   // counteract body to face forward
            stand.setRightArmPose(new Rotations(-120, 0, -10)); // reaching forward
            stand.setLeftArmPose(new Rotations(-120, 0, 10));
            stand.setRightLegPose(new Rotations(90, 0, 0));  // extend behind horizontally
            stand.setLeftLegPose(new Rotations(90, 0, 0));
        };
    }

    /** Apply a pose to an already-spawned actor without discarding it. */
    static void setPose(SceneBuilder scene, ElementLink<EntityElement> link, Consumer<ArmorStand> pose) {
        scene.world().modifyEntity(link, entity -> {
            if (entity instanceof ArmorStand stand) pose.accept(stand);
        });
    }

    /**
     * Convenience for holding an item in the off-hand.
     * Combine with other consumers via {@code offhand(item).andThen(crouching())}.
     */
    static Consumer<ArmorStand> offhand(String itemId) {
        return stand -> stand.setItemSlot(EquipmentSlot.OFFHAND, createItem(itemId));
    }

    /**
     * Convenience for replacing a single armor slot.
     * Use when the default cardboard set needs one piece swapped out.
     */
    static Consumer<ArmorStand> wearing(EquipmentSlot slot, String itemId) {
        return stand -> stand.setItemSlot(slot, createItem(itemId));
    }

    // ---- Riding actor helpers (CardboardActorElement) ----

    /**
     * Bundles an element link with its spawn location so moveRidingActor can
     * convert absolute world coords to element-relative offsets automatically.
     */
    record RidingActor(ElementLink<ParrotElement> link, Vec3 location) {}

    static RidingActor spawnRidingActor(SceneBuilder scene, Vec3 worldPos) {
        return spawnRidingActor(scene, worldPos, stand -> {});
    }

    static RidingActor spawnRidingActor(SceneBuilder scene, Vec3 worldPos, Consumer<ArmorStand> configure) {
        ElementLink<ParrotElement> link = new ElementLinkImpl<>(ParrotElement.class);
        CardboardActorElement element = new CardboardActorElement(
                worldPos, ParrotPose.FacePointOfInterestPose::new, configure);
        scene.addInstruction(new CreateParrotInstruction(5, Direction.DOWN, element));
        scene.addInstruction(s -> s.linkElement(element, link));
        return new RidingActor(link, worldPos);
    }

    /**
     * Positions the riding actor at absolute world coordinates with sub-tick lerp.
     * Call once per tick inside an animation loop, then scene.idle(1).
     */
    static void moveRidingActor(SceneBuilder scene, RidingActor actor, double wx, double wy, double wz) {
        final double ox = wx - actor.location().x;
        final double oy = wy - actor.location().y;
        final double oz = wz - actor.location().z;
        scene.addInstruction(s -> s.resolveOptional(actor.link())
                .ifPresent(a -> a.setPositionOffset(new Vec3(ox, oy, oz), false)));
    }

    /** Immediate teleport with no lerp — use for first placement before animation. */
    static void teleportRidingActor(SceneBuilder scene, RidingActor actor, double wx, double wy, double wz) {
        final double ox = wx - actor.location().x;
        final double oy = wy - actor.location().y;
        final double oz = wz - actor.location().z;
        scene.addInstruction(s -> s.resolveOptional(actor.link())
                .ifPresent(a -> a.setPositionOffset(new Vec3(ox, oy, oz), true)));
    }

    static void hideRidingActor(SceneBuilder scene, RidingActor actor) {
        scene.special().hideElement(actor.link(), Direction.DOWN);
    }

    // ---- Crawling actor helpers (CrawlingActorElement) ----

    record CrawlingActor(ElementLink<ParrotElement> link, Vec3 location) {}

    static CrawlingActor spawnCrawlingActor(SceneBuilder scene, Vec3 worldPos) {
        return spawnCrawlingActor(scene, worldPos, mob -> {});
    }

    static CrawlingActor spawnCrawlingActor(SceneBuilder scene, Vec3 worldPos, Consumer<Mob> configure) {
        ElementLink<ParrotElement> link = new ElementLinkImpl<>(ParrotElement.class);
        CrawlingActorElement element = new CrawlingActorElement(
                worldPos, ParrotPose.FacePointOfInterestPose::new, configure);
        scene.addInstruction(new CreateParrotInstruction(5, Direction.DOWN, element));
        scene.addInstruction(s -> s.linkElement(element, link));
        return new CrawlingActor(link, worldPos);
    }

    static void moveCrawlingActor(SceneBuilder scene, CrawlingActor actor, double wx, double wy, double wz) {
        final double ox = wx - actor.location().x;
        final double oy = wy - actor.location().y;
        final double oz = wz - actor.location().z;
        scene.addInstruction(s -> s.resolveOptional(actor.link())
                .ifPresent(a -> a.setPositionOffset(new Vec3(ox, oy, oz), false)));
    }

    static void setCrawling(SceneBuilder scene, CrawlingActor actor, boolean crawling) {
        scene.addInstruction(s -> s.resolveOptional(actor.link())
                .ifPresent(p -> {
                    if (p instanceof CrawlingActorElement cae) cae.setCrawling(crawling);
                }));
    }

    static void setCrawlingActorYRot(SceneBuilder scene, CrawlingActor actor, float offset) {
        scene.addInstruction(s -> s.resolveOptional(actor.link())
                .ifPresent(p -> {
                    if (p instanceof CrawlingActorElement cae) cae.setBodyYRotOffset(offset);
                }));
    }

    static void hideCrawlingActor(SceneBuilder scene, CrawlingActor actor) {
        scene.special().hideElement(actor.link(), Direction.DOWN);
    }

    // ---- Package helpers ----

    /** Holds the two entity links that make up a package-with-wrench pair. */
    record PackageWithWrench(ElementLink<EntityElement> pkg, ElementLink<EntityElement> wrench) {}

    /** Spawn a bare package entity (no gravity). Used in non-conveyor scenes. */
    static ElementLink<EntityElement> spawnPackage(SceneBuilder scene, Vec3 pos) {
        return scene.world().createEntity(level -> {
            PackageEntity p = new PackageEntity(level, pos.x, pos.y, pos.z);
            p.box = createItem("cardboard_package_12x12");
            p.setNoGravity(true);
            return p;
        });
    }

    /**
     * Spawn a package with a wrench ItemDisplay floating 1 block above it.
     * Use for chain conveyor scenes (Disguise, Frogports) where the wrench
     * is the held item that triggered the disguise.
     */
    static PackageWithWrench spawnPackageWithWrench(SceneBuilder scene, Vec3 pos) {
        ElementLink<EntityElement> pkg = spawnPackage(scene, pos);
        ElementLink<EntityElement> wrench = scene.world().createEntity(level -> {
            Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
            display.setPos(pos.x, pos.y + 1.0, pos.z);
            display.setYRot(display.yRotO = 180);
            display.getSlot(0).set(createItem("wrench"));
            return display;
        });
        return new PackageWithWrench(pkg, wrench);
    }

    /**
     * Move a package and its wrench display together.
     * The wrench stays 1 block above the package Y.
     */
    static void movePackage(SceneBuilder scene, PackageWithWrench p, double x, double y, double z) {
        scene.world().modifyEntity(p.pkg(),    e -> { e.setDeltaMovement(0, 0, 0); e.setPos(x, y,       z); });
        scene.world().modifyEntity(p.wrench(), e -> {
            e.setDeltaMovement(0, 0, 0);
            e.setPos(x, y + 1.0, z);
            e.setYRot(e.yRotO = 180);
        });
    }

    /** Discard both the package and its wrench display. */
    static void discardPackage(SceneBuilder scene, PackageWithWrench p) {
        scene.world().modifyEntity(p.pkg(),    e -> e.discard());
        scene.world().modifyEntity(p.wrench(), e -> e.discard());
    }

    /**
     * Start a frogport tongue animation. Call once, then call {@link #tickFrogportAnimation}
     * each frame to advance it. Uses the frogport's existing target from the schematic.
     * Bypasses startAnimation() to avoid PackageItem.isPackage() checks.
     */
    static void startFrogportAnimation(SceneBuilder scene, BlockPos pos) {
        scene.addInstruction(s -> {
            var be = s.getWorld().getBlockEntity(pos);
            if (be instanceof FrogportBlockEntity frogport) {
                frogport.animatedPackage = InvisiblePackageHelper.getInvisiblePackage();
                frogport.currentlyDepositing = false;
                frogport.animationProgress.startWithValue(0);
                frogport.animationProgress.chase(1, 0.1, LerpedFloat.Chaser.LINEAR);
            }
        });
    }

    /**
     * Advance the frogport animation by one tick. Call after {@link #startFrogportAnimation},
     * once per frame (piggyback on the scene's existing idle(1) calls).
     */
    static void tickFrogportAnimation(SceneBuilder scene, BlockPos pos) {
        scene.addInstruction(s -> {
            var be = s.getWorld().getBlockEntity(pos);
            if (be instanceof FrogportBlockEntity frogport) {
                frogport.animationProgress.tickChaser();
            }
        });
    }
}
