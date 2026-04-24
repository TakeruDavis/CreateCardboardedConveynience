package net.takerudavis.cardboarded_conveynience.ponder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

import static net.takerudavis.cardboarded_conveynience.ponder.PonderSceneHelper.*;

public class ChuteTeleportScene {

    public static void chuteTeleport(SceneBuilder scene, SceneBuildingUtil util) {
        // Structure: 13x8x11
        // Vertical column: X=5, Y=4-7, Z=5
        // Diagonal staircase: [6,5,5] [7,6,5] [8,7,5] — enters from the east
        // Both paths share the same exit below [5,4,5] at Y≈3.4
        scene.title("chute_teleport", "Dropping Through Chutes");
        scene.configureBasePlate(3, 3, 7);
        scene.showBasePlate();
        scene.idle(10);

        Consumer<ArmorStand> crouching = crouching();

        // Only the vertical column blocks — no floor, no staircase.
        // Staircase blocks at X=6-8 are untouched until phase 2.
        ElementLink<WorldSectionElement> columnView = scene.world().showIndependentSection(
            util.select().fromTo(5, 4, 5, 5, 7, 5), Direction.DOWN);
        // Default state is facing=down, shape=normal — looks like a plain vertical top chute
        scene.world().setBlock(util.grid().at(5, 4, 5),
            BuiltInRegistries.BLOCK.get(new ResourceLocation("create", "chute")).defaultBlockState(), false);
        scene.idle(20);

        // Package exits below chute [5,4,5] at Y=3.4, then falls to the base plate surface at Y=1.
        // Actor appears where the package lands, not where it first spawns.
        Vec3 exitPos = new Vec3(5.5, 3.4, 5.5);
        Vec3 actorExitPos = new Vec3(5.5, 1.0, 5.5);

        // --- Phase 1: vertical entry ---
        scene.addKeyframe();

        Vec3 topEntry = util.vector().centerOf(5, 8, 5);
        ElementLink<EntityElement> actor1 = spawnActor(scene, topEntry, 180, crouching);
        scene.idle(10);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.chute_teleport.text_1")
            .placeNearTarget()
            .pointAt(topEntry.add(0, 1, 0));
        scene.idle(55);

        // Actor becomes package before entering
        scene.world().modifyEntity(actor1, Entity::discard);
        ElementLink<EntityElement> pkg1 = spawnPackage(scene, topEntry);
        scene.idle(15);

        // Package teleports — appears at exit
        scene.world().modifyEntity(pkg1, Entity::discard);
        ElementLink<EntityElement> pkg1Exit = spawnPackage(scene, exitPos);
        scene.idle(15);

        // Package becomes actor at exit
        scene.world().modifyEntity(pkg1Exit, Entity::discard);
        ElementLink<EntityElement> actor1Exit = spawnActor(scene, actorExitPos, 180, crouching);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.chute_teleport.text_2")
            .independent();
        scene.idle(60);

        scene.world().modifyEntity(actor1Exit, Entity::discard);
        scene.idle(10);

        // --- Pan toward the staircase ---
        // Slide the column 2 blocks west; staircase will be merged into the section
        // so it inherits the same offset and connects visually to the column
        scene.world().moveSection(columnView, new Vec3(-2, 0, 0), 20);
        scene.idle(25);

        // --- Build staircase bottom to top (merged into columnView for correct offset) ---
        // Restore the bottom chute to its original NBT state (shape=intersection, connecting to the diagonal)
        scene.world().restoreBlocks(util.select().position(5, 4, 5));
        scene.world().showSectionAndMerge(util.select().position(6, 5, 5), Direction.EAST, columnView);
        scene.idle(8);
        scene.world().showSectionAndMerge(util.select().position(7, 6, 5), Direction.EAST, columnView);
        scene.idle(8);
        scene.world().showSectionAndMerge(util.select().position(8, 7, 5), Direction.EAST, columnView);
        scene.idle(8);

        // --- Phase 2: diagonal entry ---
        scene.addKeyframe();

        // After the -2 X pan, the column renders at visual X=3 (world X=5 + offset -2).
        // Entities don't inherit section offsets, so exit positions must be shifted by -2 in X
        // to visually align with the column exit.
        Vec3 exitPos2 = new Vec3(exitPos.x - 2, exitPos.y, exitPos.z);
        Vec3 actorExitPos2 = new Vec3(actorExitPos.x - 2, actorExitPos.y, actorExitPos.z);

        // Staircase entry [8,7,5] renders at visual X=6 after the -2 pan.
        // Entity at world X=6.5 renders at visual X=6.5 — above the staircase top.
        Vec3 stairsEntry = topEntry.add(1, 0, 0);
        ElementLink<EntityElement> actor2 = spawnActor(scene, stairsEntry, 90, crouching);
        scene.idle(10);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.chute_teleport.text_3")
            .placeNearTarget()
            .pointAt(stairsEntry.add(0, 1, 0));
        scene.idle(55);

        scene.world().modifyEntity(actor2, Entity::discard);
        ElementLink<EntityElement> pkg2 = spawnPackage(scene, stairsEntry);
        scene.idle(5);

        scene.world().modifyEntity(pkg2, Entity::discard);
        ElementLink<EntityElement> pkg2Exit = spawnPackage(scene, exitPos2);
        scene.idle(15);

        scene.world().modifyEntity(pkg2Exit, Entity::discard);
        ElementLink<EntityElement> actor2Exit = spawnActor(scene, actorExitPos2, 90, crouching);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.chute_teleport.text_4")
            .independent();
        scene.idle(60);

        // --- Phase 3: smart chute ---
        scene.addKeyframe();

        scene.world().modifyEntity(actor2Exit, Entity::discard);

        // Shift column back to centre first so staircase blocks visually realign
        // with their world positions — otherwise destroyBlock fires particles at
        // the world coords (6-8, 5-7, 5) while the blocks are rendered 2 west.
        scene.world().moveSection(columnView, new Vec3(2, 0, 0), 20);
        scene.idle(25);

        // Remove staircase top to bottom — particles now match visual position
        scene.world().destroyBlock(util.grid().at(8, 7, 5));
        scene.idle(8);
        scene.world().destroyBlock(util.grid().at(7, 6, 5));
        scene.idle(8);
        scene.world().destroyBlock(util.grid().at(6, 5, 5));
        scene.idle(8);

        // Bottom chute no longer connects to the diagonal — restore to plain vertical
        scene.world().setBlock(util.grid().at(5, 4, 5),
            BuiltInRegistries.BLOCK.get(new ResourceLocation("create", "chute")).defaultBlockState(), false);

        // Swap the top chute for a smart chute (unpowered — powered state is closed)
        scene.world().setBlock(util.grid().at(5, 7, 5),
            BuiltInRegistries.BLOCK.get(new ResourceLocation("create", "smart_chute"))
                .defaultBlockState().setValue(BlockStateProperties.POWERED, false), false);
        scene.idle(10);

        // --- Phase 3a: wrong filter — player is rejected at entry ---

        Vec3 filterPos = util.vector().centerOf(5, 7, 5);
        scene.overlay().showControls(filterPos, Pointing.LEFT, 80)
            .withItem(createItem("wrench"));
        scene.idle(20);

        ElementLink<EntityElement> actor3 = spawnActor(scene, topEntry, 180, crouching);
        scene.idle(10);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.chute_teleport.text_5")
            .placeNearTarget()
            .pointAt(topEntry.add(0, 1, 0));
        scene.idle(55);

        // Convert to package — rejected at entry, undisguise back at the top
        scene.world().modifyEntity(actor3, Entity::discard);
        ElementLink<EntityElement> pkg3 = spawnPackage(scene, topEntry);
        scene.idle(20);

        scene.world().modifyEntity(pkg3, Entity::discard);
        ElementLink<EntityElement> actor3Exit = spawnActor(scene, topEntry, 180, crouching);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.chute_teleport.text_6")
            .independent();
        scene.idle(60);

        // --- Phase 3b: correct filter — same actor, now routed ---
        scene.addKeyframe();

        scene.overlay().showControls(filterPos, Pointing.LEFT, 80)
            .withItem(createItem("cardboard_package_12x12"));
        scene.idle(20);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.chute_teleport.text_7")
            .placeNearTarget()
            .pointAt(topEntry.add(0, 1, 0));
        scene.idle(55);

        scene.world().modifyEntity(actor3Exit, Entity::discard);
        ElementLink<EntityElement> pkg4 = spawnPackage(scene, topEntry);
        scene.idle(5);

        scene.world().modifyEntity(pkg4, Entity::discard);
        ElementLink<EntityElement> pkg4Exit = spawnPackage(scene, exitPos);
        scene.idle(15);

        scene.world().modifyEntity(pkg4Exit, Entity::discard);
        ElementLink<EntityElement> actor4Exit = spawnActor(scene, actorExitPos, 180, crouching);

        scene.overlay().showText(60)
            .text("cardboarded_conveynience.ponder.chute_teleport.text_8")
            .independent();
        scene.idle(70);

        // --- Phase 3d: redstone closes the smart chute ---
        scene.addKeyframe();

        scene.world().modifyEntity(actor4Exit, Entity::discard);

        BlockPos leverPos = util.grid().at(4, 7, 5);
        scene.world().setBlock(leverPos,
            Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, Direction.WEST)
                .setValue(LeverBlock.POWERED, false), false);
        scene.world().showSectionAndMerge(util.select().position(4, 7, 5), Direction.WEST, columnView);
        scene.idle(20);

        scene.world().setBlock(leverPos,
            Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, Direction.WEST)
                .setValue(LeverBlock.POWERED, true), false);
        scene.world().setBlock(util.grid().at(5, 7, 5),
            BuiltInRegistries.BLOCK.get(new ResourceLocation("create", "smart_chute"))
                .defaultBlockState().setValue(BlockStateProperties.POWERED, true), false);
        scene.idle(10);

        scene.overlay().showText(60)
            .text("cardboarded_conveynience.ponder.chute_teleport.text_9")
            .placeNearTarget()
            .pointAt(util.vector().centerOf(5, 7, 5));
        scene.idle(65);

        ElementLink<EntityElement> actor5 = spawnActor(scene, topEntry, 180, crouching);
        scene.idle(10);

        scene.world().modifyEntity(actor5, Entity::discard);
        ElementLink<EntityElement> pkg5 = spawnPackage(scene, topEntry);
        scene.idle(20);

        scene.world().modifyEntity(pkg5, Entity::discard);
        spawnActor(scene, topEntry, 180, crouching);

        scene.overlay().showText(60)
            .text("cardboarded_conveynience.ponder.chute_teleport.text_10")
            .independent();
        scene.idle(70);
    }
}
