package net.takerudavis.cardboarded_conveynience.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import static net.takerudavis.cardboarded_conveynience.ponder.PonderSceneHelper.*;

public class FrogportDeliveryScene {

    // Schematic layout:
    //   Y=0  floor (checker concrete)
    //   Y=1  frogports — "Storage" at [4,1,6], "Workshop" at [13,1,10]
    //   Y=1-3 cut_diorite_wall pillars at (2,6) (14,6) (8,8) (2,14) (8,14) (14,14)
    //   Y=4  chain_conveyor nodes on pillar tops

    public static void frogportDelivery(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("frogport_delivery", "Frogport Delivery");
        scene.configureBasePlate(2, 2, 13);
        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);
        new CreateSceneBuilder(scene).world().setKineticSpeed(util.select().layersFrom(1), 16f);

        // Junction node C at block (8,4,8) — single source of truth for all positions.
        // Every actor spawn, package waypoint, and arc center is an offset from here.
        final double JX = 8.5, JY = 3.125, JZ = 8.5;
        final double GY = 1.0;  // ground-level Y (top of floor blocks)
        final double R  = 1.25; // node arc radius — Create's chain anchor distance from node center (ChainConveyorBlockEntity#calculateConnectionStats)
        final double CO = R * Math.sin(Math.toRadians(35)); // connection offset — perpendicular distance from connection axis to chain chord (~0.717)
        final double CA = R * Math.cos(Math.toRadians(35)); // connection-axial offset — axial distance from node center to chord endpoint (~1.024)
        final int    S  = 2;    // speed multiplier — increase to slow packages down

        // Key positions derived from junction
        Vec3 nearFrogport   = new Vec3(JX - 4,  GY, JZ - 2);      // Storage  [4,1,6]
        Vec3 farFrogport    = new Vec3(JX + 5,  GY, JZ + 2);      // Workshop [13,1,10]
        Vec3 junctionGround = new Vec3(JX,      GY, JZ - R);      // north apex of C's node circle (pkg1 spawn)
        Vec3 groundA        = new Vec3(JX - CO, GY, JZ + CA);     // directly below C-E west chord end at C
        Vec3 groundB        = new Vec3(JX + CO, GY, JZ + CA);     // directly below C-E east chord end at C

        // ============================== STAGE 1 ==============================
        // No nametag — actor rides the conveyor circuit back and forth between
        // junction node C [8,4,8] and center node E [8,4,14], going nowhere.
        scene.addKeyframe();

        ElementLink<EntityElement> actor1 = spawnActor(scene, junctionGround, 0f,
            stand -> stand.setItemSlot(EquipmentSlot.MAINHAND, createItem("wrench")));
        scene.idle(15);

        scene.overlay().showText(50)
            .text("Usually, players ride chain conveyors with no regards to frogports...")
            .placeNearTarget()
            .pointAt(junctionGround.add(0, 1, 0));
        scene.idle(55);

        jumpOntoConveyor(scene, actor1, junctionGround);
        scene.world().modifyEntity(actor1, Entity::discard);

        // Actor transforms into a package on the conveyor.
        PackageWithWrench pkg1 = spawnPackageWithWrench(scene, new Vec3(JX, JY, JZ - R));

        // Pkg1 rides the single C↔E chain loop:
        //   Arc formula (unified): x = cx + R*sin(θ), z = cz + R*cos(θ)
        //     θ=0° south apex, 90° east, 180° north, 270° west (CCW with increasing θ).
        //   At C (dead-end, chord ends at θ=±35°): external wrap is 290° north-side.
        //     Starting at north apex splits that wrap into two 145° halves.
        //   At E (3-way junction, C-chord ends at θ=145°/215°): external wrap via
        //     south is 290° CCW, avoiding the D/F chord ends.

        // C part 1: north apex (180°) → west chord end (325°), 145° CCW.
        for (int i = 0; i <= 11 * S; i++) {
            final double theta = Math.toRadians(180 + 145.0 * i / (11.0 * S));
            movePackage(scene, pkg1, JX + R * Math.sin(theta), JY, JZ + R * Math.cos(theta));
            scene.idle(1);
        }
        // Chord C→E west: x = JX-CO, z from JZ+CA to JZ+6-CA.
        for (int i = 0; i <= 14 * S; i++) {
            final double p = i / (14.0 * S);
            movePackage(scene, pkg1, JX - CO, JY, (JZ + CA) + (6 - 2 * CA) * p);
            scene.idle(1);
        }
        // Show text_2 when pkg1 reaches node E — runs parallel to the return half of the circuit.
        scene.overlay().showText(50)
            .text("...and routing at junctions just follows the direction you're facing.")
            .independent();
        // E wrap: C-west (215°) → C-east (215°+290°=505°), 290° CCW via south.
        for (int i = 0; i <= 22 * S; i++) {
            final double theta = Math.toRadians(215 + 290.0 * i / (22.0 * S));
            movePackage(scene, pkg1, JX + R * Math.sin(theta), JY, (JZ + 6) + R * Math.cos(theta));
            scene.idle(1);
        }
        // Chord E→C east: x = JX+CO, z from JZ+6-CA to JZ+CA.
        for (int i = 0; i <= 14 * S; i++) {
            final double p = i / (14.0 * S);
            movePackage(scene, pkg1, JX + CO, JY, (JZ + 6 - CA) - (6 - 2 * CA) * p);
            scene.idle(1);
        }
        // C part 2: east chord end (35°) → north apex (180°), 145° CCW.
        for (int i = 0; i <= 11 * S; i++) {
            final double theta = Math.toRadians(35 + 145.0 * i / (11.0 * S));
            movePackage(scene, pkg1, JX + R * Math.sin(theta), JY, JZ + R * Math.cos(theta));
            scene.idle(1);
        }
        discardPackage(scene, pkg1);
        ElementLink<EntityElement> actor1End = spawnActor(scene, junctionGround, 0f,
            stand -> stand.setItemSlot(EquipmentSlot.MAINHAND, createItem("wrench")));
        scene.idle(20);

        // ============================== STAGE 2 ==============================
        // Two actors with nametags diverge from the junction to their destinations.
        scene.addKeyframe();

        scene.world().modifyEntity(actor1End, Entity::discard);
        scene.idle(10);

        ItemStack storageTag = new ItemStack(Items.NAME_TAG);
        storageTag.setHoverName(Component.literal("Storage"));
        ItemStack workshopTag = new ItemStack(Items.NAME_TAG);
        workshopTag.setHoverName(Component.literal("Workshop"));

        // actorA at west (groundA) → pkgB goes to Workshop
        // actorB at east (groundB) → pkgA U-turns to Storage
        ElementLink<EntityElement> actorA = spawnActor(scene, groundA, 135f,
            stand -> {
                stand.setItemSlot(EquipmentSlot.MAINHAND, createItem("wrench"));
                stand.setItemSlot(EquipmentSlot.OFFHAND, workshopTag);
            });
        ElementLink<EntityElement> actorB = spawnActor(scene, groundB, -45f,
            stand -> {
                stand.setItemSlot(EquipmentSlot.MAINHAND, createItem("wrench"));
                stand.setItemSlot(EquipmentSlot.OFFHAND, storageTag);
            });
        scene.idle(15);

        scene.overlay().showControls(groundA.add(-0.5, 1, 0), Pointing.RIGHT, 80).withItem(workshopTag);
        scene.overlay().showText(80)
            .text("Workshop")
            .placeNearTarget()
            .pointAt(groundA.add(-0.5, 1.5, 0));
        scene.overlay().showControls(groundB.add(0.5, 1, 0), Pointing.LEFT, 80).withItem(storageTag);
        scene.overlay().showText(80)
            .text("Storage")
            .placeNearTarget()
            .pointAt(groundB.add(0.5, 1.5, 0));
        scene.idle(20);

        scene.overlay().showText(50)
            .text("While disguised as a package, a nametag in your offhand routes you automatically...")
            .independent();
        scene.idle(55);

        parallelJump(scene, actorA, groundA, actorB, groundB, 0);

        scene.world().modifyEntity(actorA, Entity::discard);
        scene.world().modifyEntity(actorB, Entity::discard);

        // Label the destination frogports
        scene.overlay().showText(80)
            .text("Storage")
            .placeNearTarget()
            .pointAt(nearFrogport.add(0, 1, 0));
        scene.overlay().showText(80)
            .text("Workshop")
            .placeNearTarget()
            .pointAt(farFrogport.add(0, 1, 0));

        // Path animation — both packages depart simultaneously.
        // Arc formula (unified): x = cx + R*sin(θ), z = cz + R*cos(θ).
        //   θ=0 south, 90 east, 180 north, 270 west; CCW with increasing θ.
        //
        // Turn sweeps at a node (CCW from entry chord end to exit chord end):
        //   Right turn = 20°  (adjacent CCW chord end)
        //   Left  turn = 200° (skips two chord ends the long way around)
        //   U-turn     = 290° (wraps the external side of a dead-end node)
        //
        // Node centers: C=(JX,JZ) E=(JX,JZ+6) D=(JX-6,JZ+6) F=(JX+6,JZ+6)
        //               A=(JX-6,JZ-2) B=(JX+6,JZ-2)
        // Storage=(JX-4,JZ-2) Workshop=(JX+5,JZ+2)

        // pkgA cumulative checkpoints — Storage via C U-turn → E → D → A → drop.
        final int A0 = 21 * S;              // 290° U-turn at C (east chord → west chord)
        final int A1 = A0 + 13 * S;         // chord C→E west
        final int A2 = A1 + 2 * S;          // 20° right turn at E
        final int A3 = A2 + 13 * S;         // chord E→D north
        final int A4 = A3 + 2 * S;          // 20° right turn at D
        final int A5 = A4 + 20 * S;         // chord D→A east
        final int A6 = A5 + 8;              // vertical drop straight from chord end (no lateral pull)

        // pkgB cumulative checkpoints — Workshop via C → E → F → B wrap → return → drop.
        final int B0 = 13 * S;              // chord C→E west
        final int B1 = B0 + 15 * S;         // 200° left turn at E
        final int B2 = B1 + 13 * S;         // chord E→F south-side east
        final int B3 = B2 + 15 * S;         // 200° left turn at F
        final int B4 = B3 + 20 * S;         // chord F→B east side, full length
        final int B5 = B4 + 21 * S;         // 290° U-turn at B (east chord → west chord)
        final int B6 = B5 + 3 * S;          // chord B→F west side, partial (until Workshop target row, z=JZ)
        final int B7 = B6 + 8;              // vertical drop straight down onto Workshop target area

        BlockPos storagePos = new BlockPos(4, 1, 6);
        BlockPos workshopPos = new BlockPos(13, 1, 10);

        PackageWithWrench pkgA = spawnPackageWithWrench(scene, new Vec3(JX + CO, JY, JZ + CA));
        PackageWithWrench pkgB = spawnPackageWithWrench(scene, new Vec3(JX - CO, JY, JZ + CA));

        int loopEnd = Math.max(A6, B7);
        ElementLink<EntityElement> actorAEnd = null;

        for (int t = 0; t <= loopEnd; t++) {
            // pkgA: U-turn at C → chord south → right at E → chord west → right at D → chord south → transit → drop.
            double ax, ay = JY, az;
            if (t <= A0) {
                // C U-turn: θ=35° CCW by 290° to 325° (through north apex at 180°).
                double theta = Math.toRadians(35 + 290.0 * t / A0);
                ax = JX + R * Math.sin(theta);
                az = JZ + R * Math.cos(theta);
            } else if (t <= A1) {
                // Chord C→E west: x = JX-CO, z from JZ+CA to JZ+6-CA.
                double p = (double) (t - A0) / (A1 - A0);
                ax = JX - CO;
                az = (JZ + CA) + (6 - 2 * CA) * p;
            } else if (t <= A2) {
                // E right turn: C-west (215°) CCW by 20° to D-north (235°).
                double theta = Math.toRadians(215 + 20.0 * (t - A1) / (A2 - A1));
                ax = JX + R * Math.sin(theta);
                az = (JZ + 6) + R * Math.cos(theta);
            } else if (t <= A3) {
                // Chord E→D north: z = JZ+6-CO, x from JX-CA to JX-6+CA (going west).
                double p = (double) (t - A2) / (A3 - A2);
                ax = (JX - CA) - (6 - 2 * CA) * p;
                az = JZ + 6 - CO;
            } else if (t <= A4) {
                // D right turn: E-north (125°) CCW by 20° to A-east (145°).
                double theta = Math.toRadians(125 + 20.0 * (t - A3) / (A4 - A3));
                ax = (JX - 6) + R * Math.sin(theta);
                az = (JZ + 6) + R * Math.cos(theta);
            } else if (t <= A5) {
                // Chord D→A east: x = JX-6+CO, z from JZ+6-CA to JZ-2+CA (going north).
                double p = (double) (t - A4) / (A5 - A4);
                ax = JX - 6 + CO;
                az = (JZ + 6 - CA) - (8 - 2 * CA) * p;
            } else if (t <= A6) {
                // Vertical drop straight down from the chord end.
                double p = (double) (t - A5) / (A6 - A5);
                ax = JX - 6 + CO;
                ay = JY + (GY - JY) * p;
                az = JZ - 2 + CA;
            } else {
                ax = JX - 6 + CO; ay = GY; az = JZ - 2 + CA;
            }

            // pkgB: chord south → left at E → chord east → left at F → chord north → U-turn at B → chord south partial → drop.
            double bx, by = JY, bz;
            if (t <= B0) {
                // Chord C→E west: x = JX-CO, z from JZ+CA to JZ+6-CA.
                double p = (double) t / B0;
                bx = JX - CO;
                bz = (JZ + CA) + (6 - 2 * CA) * p;
            } else if (t <= B1) {
                // E left turn: C-west (215°) CCW by 200° to F-south (415°).
                double theta = Math.toRadians(215 + 200.0 * (t - B0) / (B1 - B0));
                bx = JX + R * Math.sin(theta);
                bz = (JZ + 6) + R * Math.cos(theta);
            } else if (t <= B2) {
                // Chord E→F south-side east: z = JZ+6+CO, x from JX+CA to JX+6-CA.
                double p = (double) (t - B1) / (B2 - B1);
                bx = (JX + CA) + (6 - 2 * CA) * p;
                bz = JZ + 6 + CO;
            } else if (t <= B3) {
                // F left turn: E-south (305°) CCW by 200° to B-east (505°).
                double theta = Math.toRadians(305 + 200.0 * (t - B2) / (B3 - B2));
                bx = (JX + 6) + R * Math.sin(theta);
                bz = (JZ + 6) + R * Math.cos(theta);
            } else if (t <= B4) {
                // Chord F→B east side (full): x = JX+6+CO, z from JZ+6-CA to JZ-2+CA (going north).
                double p = (double) (t - B3) / (B4 - B3);
                bx = JX + 6 + CO;
                bz = (JZ + 6 - CA) - (8 - 2 * CA) * p;
            } else if (t <= B5) {
                // B U-turn: θ=35° CCW by 290° to 325° (through north apex at 180°).
                double theta = Math.toRadians(35 + 290.0 * (t - B4) / (B5 - B4));
                bx = (JX + 6) + R * Math.sin(theta);
                bz = (JZ - 2) + R * Math.cos(theta);
            } else if (t <= B6) {
                // Chord B→F west side (partial, stops at Workshop target row): x = JX+6-CO, z from JZ-2+CA to JZ.
                double p = (double) (t - B5) / (B6 - B5);
                bx = JX + 6 - CO;
                bz = (JZ - 2 + CA) + (2 - CA) * p;
            } else if (t <= B7) {
                // Vertical drop straight down onto Workshop target area (2 blocks north of Workshop frogport).
                double p = (double) (t - B6) / (B7 - B6);
                bx = JX + 6 - CO;
                by = JY + (GY - JY) * p;
                bz = JZ;
            } else {
                bx = JX + 6 - CO; by = GY; bz = JZ;
            }

            if (t <= A6) movePackage(scene, pkgA, ax, ay, az);
            if (t <= B7) movePackage(scene, pkgB, bx, by, bz);

            // Frogport tongue animations — start when package arrives at drop position, tick during drop and after.
            if (t == A5) startFrogportAnimation(scene, storagePos);
            if (t > A5) tickFrogportAnimation(scene, storagePos);
            if (t == B6) startFrogportAnimation(scene, workshopPos);
            if (t > B6) tickFrogportAnimation(scene, workshopPos);

            // Undisguise pkgA as soon as it lands.
            if (t == A6) {
                discardPackage(scene, pkgA);
                actorAEnd = spawnActor(scene, new Vec3(JX - 6 + CO, GY, JZ - 2 + CA), 135f);
            }

            scene.idle(1);
        }

        discardPackage(scene, pkgB);
        ElementLink<EntityElement> actorBEnd = spawnActor(scene,
            new Vec3(JX + 6 - CO, GY, JZ), -45f);

        scene.overlay().showText(60)
            .text("...to the matching Frogport, anywhere on the network.")
            .independent();
        scene.idle(70);

        // ============================== STAGE 3 ==============================
        // Wildcard nametag — "Stor*" also routes to Storage
        scene.addKeyframe();

        scene.world().modifyEntity(actorAEnd, Entity::discard);
        scene.world().modifyEntity(actorBEnd, Entity::discard);
        scene.idle(10);

        ItemStack wildcardTag = new ItemStack(Items.NAME_TAG);
        wildcardTag.setHoverName(Component.literal("Stor*"));

        ElementLink<EntityElement> actor3 = spawnActor(scene, groundA, 135f,
            stand -> {
                stand.setItemSlot(EquipmentSlot.MAINHAND, createItem("wrench"));
                stand.setItemSlot(EquipmentSlot.OFFHAND, wildcardTag);
            });
        scene.idle(15);

        scene.overlay().showControls(groundA.add(-0.5, 1, 0), Pointing.RIGHT, 80).withItem(wildcardTag);
        scene.overlay().showText(80)
            .text("Stor*")
            .placeNearTarget()
            .pointAt(groundA.add(-0.5, 1.5, 0));
        scene.idle(20);

        scene.overlay().showText(50)
            .text("Nametag routing supports wildcards using *...")
            .independent();
        scene.idle(55);

        jumpOntoConveyor(scene, actor3, groundA);

        scene.world().modifyEntity(actor3, Entity::discard);
        PackageWithWrench pkg3 = spawnPackageWithWrench(scene, new Vec3(JX - CO, JY, JZ + CA));

        // Same path as pkgA without the U-turn prefix: chord south → right at E → chord west → right at D → chord south → drop.
        final int P1 = 13 * S;              // chord C→E west
        final int P2 = P1 + 2 * S;          // 20° right turn at E
        final int P3 = P2 + 13 * S;         // chord E→D north
        final int P4 = P3 + 2 * S;          // 20° right turn at D
        final int P5 = P4 + 20 * S;         // chord D→A east
        final int P6 = P5 + 8;              // vertical drop straight from chord end

        for (int t = 0; t <= P6; t++) {
            double x, y = JY, z;
            if (t <= P1) {
                double p = (double) t / P1;
                x = JX - CO;
                z = (JZ + CA) + (6 - 2 * CA) * p;
            } else if (t <= P2) {
                double theta = Math.toRadians(215 + 20.0 * (t - P1) / (P2 - P1));
                x = JX + R * Math.sin(theta);
                z = (JZ + 6) + R * Math.cos(theta);
            } else if (t <= P3) {
                double p = (double) (t - P2) / (P3 - P2);
                x = (JX - CA) - (6 - 2 * CA) * p;
                z = JZ + 6 - CO;
            } else if (t <= P4) {
                double theta = Math.toRadians(125 + 20.0 * (t - P3) / (P4 - P3));
                x = (JX - 6) + R * Math.sin(theta);
                z = (JZ + 6) + R * Math.cos(theta);
            } else if (t <= P5) {
                double p = (double) (t - P4) / (P5 - P4);
                x = JX - 6 + CO;
                z = (JZ + 6 - CA) - (8 - 2 * CA) * p;
            } else {
                double p = (double) (t - P5) / (P6 - P5);
                x = JX - 6 + CO;
                y = JY + (GY - JY) * p;
                z = JZ - 2 + CA;
            }
            movePackage(scene, pkg3, x, y, z);

            if (t == P5) startFrogportAnimation(scene, storagePos);
            if (t > P5) tickFrogportAnimation(scene, storagePos);

            scene.idle(1);
        }
        discardPackage(scene, pkg3);
        spawnActor(scene, new Vec3(JX - 6 + CO, GY, JZ - 2 + CA), 135f);

        scene.overlay().showText(60)
            .text("...any matching Frogport becomes a valid destination.")
            .independent();
        scene.idle(70);
    }

    private static final double GY = 1.0, JY_CONST = 3.125;

    // Pre-jump pose then 12-tick rise for a single actor (used in stage 1 / stage 3).
    private static void jumpOntoConveyor(SceneBuilder scene, ElementLink<EntityElement> actor, Vec3 groundPos) {
        scene.world().modifyEntity(actor, e -> {
            if (e instanceof ArmorStand stand) {
                stand.setRightArmPose(new Rotations(-150, 0, -10));
                stand.setLeftArmPose(new Rotations(-150, 0, 10));
                stand.setRightLegPose(new Rotations(-20, 0, 0));
                stand.setLeftLegPose(new Rotations(20, 0, 0));
            }
        });
        scene.idle(3);

        for (int i = 0; i <= 12; i++) {
            final double y = GY + ((JY_CONST - GY - 1.0) * i / 12.0);
            final float legAngle = 20f * (1f - (float) i / 12f);
            scene.world().modifyEntity(actor, e -> {
                e.setDeltaMovement(0, 0, 0);
                e.setPos(groundPos.x, y, groundPos.z);
                if (e instanceof ArmorStand stand) {
                    stand.setRightLegPose(new Rotations(-legAngle, 0, 0));
                    stand.setLeftLegPose(new Rotations(legAngle, 0, 0));
                }
            });
            scene.idle(1);
        }
        scene.idle(5);
    }

    // Staggered jump: A starts immediately, B starts `delay` ticks later.
    // Both are updated each tick during their overlapping rise, so only one idle(1) per tick.
    private static void parallelJump(SceneBuilder scene,
                                     ElementLink<EntityElement> a, Vec3 posA,
                                     ElementLink<EntityElement> b, Vec3 posB,
                                     int delay) {
        scene.world().modifyEntity(a, e -> {
            if (e instanceof ArmorStand stand) {
                stand.setRightArmPose(new Rotations(-150, 0, -10));
                stand.setLeftArmPose(new Rotations(10, 0, 10));  // offhand stays down
                stand.setRightLegPose(new Rotations(-20, 0, 0));
                stand.setLeftLegPose(new Rotations(20, 0, 0));
            }
        });
        scene.world().modifyEntity(b, e -> {
            if (e instanceof ArmorStand stand) {
                stand.setRightArmPose(new Rotations(-150, 0, -10));
                stand.setLeftArmPose(new Rotations(10, 0, 10));  // offhand stays down
                stand.setRightLegPose(new Rotations(-20, 0, 0));
                stand.setLeftLegPose(new Rotations(20, 0, 0));
            }
        });
        scene.idle(3);

        // A rises over ticks [0, 12], B rises over ticks [delay, delay+12].
        // Both are updated in the same tick whenever their windows overlap.
        for (int t = 0; t <= 12 + delay; t++) {
            if (t <= 12) {
                final double y = GY + (JY_CONST - GY - 1.0) * t / 12.0;
                final float leg = 20f * (1f - (float) t / 12f);
                scene.world().modifyEntity(a, e -> {
                    e.setDeltaMovement(0, 0, 0);
                    e.setPos(posA.x, y, posA.z);
                    if (e instanceof ArmorStand stand) {
                        stand.setRightLegPose(new Rotations(-leg, 0, 0));
                        stand.setLeftLegPose(new Rotations(leg, 0, 0));
                    }
                });
            }
            if (t >= delay && (t - delay) <= 12) {
                final int bStep = t - delay;
                final double y = GY + (JY_CONST - GY - 1.0) * bStep / 12.0;
                final float leg = 20f * (1f - (float) bStep / 12f);
                scene.world().modifyEntity(b, e -> {
                    e.setDeltaMovement(0, 0, 0);
                    e.setPos(posB.x, y, posB.z);
                    if (e instanceof ArmorStand stand) {
                        stand.setRightLegPose(new Rotations(-leg, 0, 0));
                        stand.setLeftLegPose(new Rotations(leg, 0, 0));
                    }
                });
            }
            scene.idle(1);
        }
        scene.idle(5);
    }
}
