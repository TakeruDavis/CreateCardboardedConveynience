package net.takerudavis.cardboarded_conveynience.ponder;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.core.Rotations;
import net.minecraft.world.phys.Vec3;

import static net.takerudavis.cardboarded_conveynience.ponder.PonderSceneHelper.*;

public class DisguiseScene {

    public static void disguise(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("cardboard_disguise", "Package Disguise");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        // Show all blocks above the base plate (chain conveyors, etc.)
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        // Position adjusted (-0.5 on X/Z to align with package under chain)
        scene.addKeyframe();
        Vec3 armorStandPos = util.vector().centerOf(2, 1, 4).add(-0.5, 0.0, -0.5);
        ElementLink<EntityElement> armorStand = spawnActor(scene, armorStandPos, 180,
            stand -> stand.setItemSlot(EquipmentSlot.MAINHAND, createItem("wrench")));
        scene.idle(10);

        scene.overlay().showText(40)
            .text("cardboarded_conveynience.ponder.cardboard_disguise.text_1")
            .placeNearTarget()
            .pointAt(armorStandPos.add(0, 1, 0));
        scene.idle(50);

        // Armor stand jumps before transforming (rise to peak, then transform)
        for (int i = 0; i < 5; i++) {
            final int tick = i;
            scene.world().modifyEntity(armorStand, entity -> {
                if (entity instanceof ArmorStand stand) {
                    double jumpHeight = tick * 0.1;
                    stand.setPos(armorStandPos.x, armorStandPos.y + jumpHeight, armorStandPos.z);

                    float progress = tick / 4f;
                    float legAngle = 30f * progress;
                    float armAngle = -120f * progress;

                    stand.setRightLegPose(new Rotations(-legAngle, 0, 0));
                    stand.setLeftLegPose(new Rotations(legAngle, 0, 0));
                    stand.setRightArmPose(new Rotations(armAngle, 0, 0));
                }
            });
            scene.idle(1);
        }

        // Transform into package with wrench floating above
        scene.addKeyframe();
        scene.world().modifyEntity(armorStand, entity -> entity.discard());

        Vec3 packagePos = armorStandPos.add(0, 1.5, 0);
        PonderSceneHelper.PackageWithWrench pkg = spawnPackageWithWrench(scene, packagePos);
        scene.world().modifyEntity(pkg.pkg(), e -> e.setYRot(180));

        scene.overlay().showText(60)
            .text("cardboarded_conveynience.ponder.cardboard_disguise.text_2")
            .independent();

        // Animate package and wrench moving diagonally (forward-right from 180° POV = -Z, +X)
        double speed = 0.05;
        for (int i = 0; i < 60; i++) {
            final double newX = packagePos.x + (speed * i);
            final double newZ = packagePos.z - (speed * i);
            movePackage(scene, pkg, newX, packagePos.y, newZ);
            scene.idle(1);
        }
    }
}
