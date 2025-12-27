package net.takerudavis.cardboarded_conveynience.ponder;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.core.Rotations;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class CardboardScenes {

    private static ItemStack createItem(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("create", id)));
    }

    public static void disguise(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("cardboard_disguise", "Package Disguise");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        // Show all blocks above the base plate (chain conveyors, etc.)
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        // Spawn armor stand with arms and cardboard armor
        // Position adjusted (-0.5 on X/Z to align with package under chain)
        Vec3 armorStandPos = util.vector().centerOf(2, 1, 4).add(-0.5, 0.0, -0.5);
        ElementLink<EntityElement> armorStand = scene.world().createEntity(level -> {
            ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
            stand.setPos(armorStandPos.x, armorStandPos.y, armorStandPos.z);
            stand.setYRot(180); // Rotate 180 degrees
            stand.setShowArms(true);
            stand.setNoBasePlate(true); // Hide base plate
            // Equip cardboard armor
            stand.setItemSlot(EquipmentSlot.HEAD, createItem("cardboard_helmet"));
            stand.setItemSlot(EquipmentSlot.CHEST, createItem("cardboard_chestplate"));
            stand.setItemSlot(EquipmentSlot.LEGS, createItem("cardboard_leggings"));
            stand.setItemSlot(EquipmentSlot.FEET, createItem("cardboard_boots"));
            // Wrench in hand
            stand.setItemSlot(EquipmentSlot.MAINHAND, createItem("wrench"));
            return stand;
        });
        scene.idle(10);

        scene.overlay().showText(40)
            .text("Wear full cardboard armor while riding a chain conveyor...")
            .placeNearTarget()
            .pointAt(armorStandPos.add(0, 1, 0));
        scene.idle(50);

        // Armor stand jumps before transforming (rise to peak, then transform)
        for (int i = 0; i < 5; i++) {
            final int tick = i;
            scene.world().modifyEntity(armorStand, entity -> {
                if (entity instanceof ArmorStand stand) {
                    // Rise to peak
                    double jumpHeight = tick * 0.1;
                    stand.setPos(armorStandPos.x, armorStandPos.y + jumpHeight, armorStandPos.z);

                    // Animate pose during jump
                    float progress = tick / 4f; // 0 to 1
                    float legAngle = 30f * progress; // Legs spread more as jump progresses
                    float armAngle = -120f * progress; // Raise arm with wrench

                    stand.setRightLegPose(new Rotations(-legAngle, 0, 0)); // Right leg forward
                    stand.setLeftLegPose(new Rotations(legAngle, 0, 0)); // Left leg backward
                    stand.setRightArmPose(new Rotations(armAngle, 0, 0)); // Raise wrench arm
                }
            });
            scene.idle(1);
        }

        // Transform into package - hide armor stand and show package entity
        scene.world().modifyEntity(armorStand, entity -> entity.discard());

        // Create a PackageEntity - position relative to armor stand (offset for hanging under chain)
        Vec3 packageOffset = new Vec3(0, 1.5, 0); // Only Y offset - directly above armor stand
        Vec3 packagePos = armorStandPos.add(packageOffset);
        ElementLink<EntityElement> packageEntity = scene.world().createEntity(level -> {
            PackageEntity pkg = new PackageEntity(level, packagePos.x, packagePos.y, packagePos.z);
            // Must be an actual PackageItem or entity will discard itself in tick()
            pkg.box = createItem("cardboard_package_12x12");
            pkg.setYRot(180); // Face same direction as armor stand
            return pkg;
        });

        // Create ItemDisplay with wrench above the package
        Vec3 wrenchOffset = new Vec3(0, 1.0, 0); // Above the package
        ElementLink<EntityElement> wrenchEntity = scene.world().createEntity(level -> {
            Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
            display.setPos(packagePos.x, packagePos.y + wrenchOffset.y, packagePos.z);
            display.setYRot(180); // Rotate to align wrench teeth with chains
            // Use SlotAccess to set the item (slot 0 is the displayed item)
            display.getSlot(0).set(createItem("wrench"));
            return display;
        });

        scene.overlay().showText(60)
            .text("...to disguise yourself as a package!")
            .independent();

        // Animate package and wrench moving diagonally (forward-right from 180° POV = -Z, +X)
        // Move 3 blocks over 60 ticks
        double speed = 0.05;
        for (int i = 0; i < 60; i++) {
            final int tick = i;
            final double newX = packagePos.x + (speed * tick); // Right from 180° POV = +X
            final double newZ = packagePos.z - (speed * tick); // Forward from 180° POV = -Z

            scene.world().modifyEntity(packageEntity, entity -> {
                entity.setDeltaMovement(0, 0, 0);
                entity.setPos(newX, packagePos.y, newZ);
            });
            scene.world().modifyEntity(wrenchEntity, entity -> {
                entity.setDeltaMovement(0, 0, 0);
                entity.setPos(newX, packagePos.y + wrenchOffset.y, newZ);
            });
            scene.idle(1);
        }
    }
}
