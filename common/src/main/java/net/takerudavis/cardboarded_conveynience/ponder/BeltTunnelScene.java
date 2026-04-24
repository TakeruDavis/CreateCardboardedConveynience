package net.takerudavis.cardboarded_conveynience.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;

import static net.takerudavis.cardboarded_conveynience.ponder.PonderSceneHelper.*;

public class BeltTunnelScene {

    public static void beltTunnel(SceneBuilder scene, SceneBuildingUtil util) {
        // Structure: 9x3x9
        // Belt: X=4, Y=1, Z=1-7, facing south (items travel +Z, camera at Z=1 side)
        // Andesite tunnel: X=4, Y=2, Z=3-5 (window at Z=4)
        // Bamboo trapdoor: X=4, Y=2, Z=2 (open in schematic; closed = forces crawling)
        scene.title("belt_tunnel", "Passing Through Tunnels");
        scene.configureBasePlate(0, 0, 9);
        scene.showBasePlate();
        scene.idle(10);

        // Show everything except the trapdoor — it gets introduced in Stage 2
        scene.world().showSection(util.select().layersFrom(1).substract(util.select().position(4, 2, 2)), Direction.DOWN);
        scene.idle(20);
        new CreateSceneBuilder(scene).world().setKineticSpeed(util.select().layersFrom(1), 16f);

        Vec3 beltStart = new Vec3(4.5, 2.0, 1.5);
        Vec3 beltEnd   = new Vec3(4.5, 2.0, 6.5);
        Vec3 pkgStart  = new Vec3(4.5, 2.0, 1.5);
        Vec3 pkgEnd    = new Vec3(4.5, 2.0, 6.5);
        double pkgLength = pkgEnd.z - pkgStart.z;
        int travelTicks = 50;

        BlockPos trapdoorPos = util.grid().at(4, 2, 2);
        BlockState trapdoorBase = BuiltInRegistries.BLOCK
            .get(ResourceLocation.fromNamespaceAndPath("minecraft", "bamboo_trapdoor"))
            .defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            .setValue(BlockStateProperties.HALF, Half.TOP);
        BlockState trapdoorOpen = trapdoorBase.setValue(BlockStateProperties.OPEN, true);
        BlockState trapdoorClosed = trapdoorBase.setValue(BlockStateProperties.OPEN, false);

        Vec3 trapdoorVec = new Vec3(4.5, 2.0, 2.5); // center of trapdoor block, belt height
        Vec3 crawlStart = trapdoorVec.add(0, 0, -0.25); // just before trapdoor
        int walkUpTicks = 15; // ticks to walk from beltStart to crawlStart

        // TODO: verify Pointing direction for brass tunnel filter slot in-game
        Vec3 filterHint = util.vector().centerOf(4, 2, 3);

        // ============================== STAGE 1 ==============================
        // Full cardboard armor, andesite tunnel — no filter
        scene.addKeyframe();

        ElementLink<EntityElement> actor1 = spawnActor(scene, beltStart, 0);
        scene.idle(20);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_1")
            .placeNearTarget()
            .pointAt(beltStart.add(0, 1, 0));
        scene.idle(55);

        scene.world().modifyEntity(actor1, Entity::discard);
        ElementLink<EntityElement> pkg1 = spawnPackage(scene, pkgStart);
        ridePackage(scene, pkg1, pkgStart, pkgLength, travelTicks);
        scene.world().modifyEntity(pkg1, Entity::discard);
        ElementLink<EntityElement> actor1End = spawnActor(scene, beltEnd, 0);

        scene.overlay().showText(60)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_2")
            .independent();
        scene.idle(70);
        scene.world().modifyEntity(actor1End, Entity::discard);
        scene.idle(10);

        // ============================== STAGE 2 ==============================
        // Leather-clad skeleton, close trapdoor → crawl through andesite tunnel
        scene.addKeyframe();

        PonderSceneHelper.CrawlingActor actor2 = spawnCrawlingActor(scene, beltStart);
        scene.idle(15);

        scene.overlay().showText(40)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_3")
            .placeNearTarget()
            .pointAt(beltStart.add(0, 1, 0));
        scene.idle(45);

        // Ensure open state, then show the trapdoor
        scene.world().setBlock(trapdoorPos, trapdoorOpen, false);
        ElementLink<WorldSectionElement> trapdoorView =
            scene.world().showIndependentSection(util.select().position(4, 2, 2), Direction.DOWN);
        scene.idle(10);

        // Walk standing from beltStart to crawlStart
        for (int i = 0; i <= walkUpTicks; i++) {
            double z = beltStart.z + ((crawlStart.z - beltStart.z) * i / walkUpTicks);
            moveCrawlingActor(scene, actor2, beltStart.x, beltStart.y, z);
            scene.idle(1);
        }

        // Close trapdoor + start crawling
        scene.world().setBlock(trapdoorPos, trapdoorClosed, false);
        setCrawling(scene, actor2, true);
        scene.idle(10);

        for (int i = 0; i <= travelTicks; i++) {
            final double z = crawlStart.z + ((beltEnd.z - crawlStart.z) * i / travelTicks);
            moveCrawlingActor(scene, actor2, crawlStart.x, crawlStart.y, z);
            scene.idle(1);
        }

        // Stand back up after passing through
        setCrawling(scene, actor2, false);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_4")
            .independent();
        scene.idle(60);
        hideCrawlingActor(scene, actor2);

        // Hide trapdoor again until Stage 5
        scene.special().hideElement(trapdoorView, Direction.UP);
        scene.world().restoreBlocks(util.select().position(4, 2, 2));
        scene.idle(10);

        // ============================== INTERMISSION ==============================
        // Replace andesite tunnels with brass tunnels
        scene.addKeyframe();

        // Replace andesite tunnels with brass, matching belt axis (Z for south-running belt)
        BlockState brassTunnel = BuiltInRegistries.BLOCK
            .get(ResourceLocation.fromNamespaceAndPath("create", "brass_tunnel"))
            .defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.Z);
        scene.world().setBlock(util.grid().at(4, 2, 3), brassTunnel, false);
        scene.world().setBlock(util.grid().at(4, 2, 4), brassTunnel, false);
        scene.world().setBlock(util.grid().at(4, 2, 5), brassTunnel, false);
        scene.idle(20);

        scene.overlay().showText(40)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_5")
            .independent();
        scene.idle(50);

        scene.overlay().showText(40)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_6")
            .independent();
        scene.idle(50);

        // ============================== STAGE 3 ==============================
        // Cardboard actor, wrench filter → rejected
        scene.addKeyframe();

        scene.overlay().showControls(filterHint, Pointing.RIGHT, 60)
            .withItem(createItem("wrench"));
        scene.idle(20);

        ElementLink<EntityElement> actor3 = spawnActor(scene, beltStart, 0);
        scene.idle(20);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_7")
            .placeNearTarget()
            .pointAt(beltStart.add(0, 1, 0));
        scene.idle(55);

        // Disguise as package, travel toward tunnel, stopped at entry
        scene.world().modifyEntity(actor3, Entity::discard);
        ElementLink<EntityElement> pkg3 = spawnPackage(scene, pkgStart);
        int approachTicks = 20;
        for (int i = 0; i <= approachTicks; i++) {
            final double z = pkgStart.z + ((crawlStart.z - pkgStart.z) * i / approachTicks);
            scene.world().modifyEntity(pkg3, e -> { e.setDeltaMovement(0, 0, 0); e.setPos(pkgStart.x, pkgStart.y, z); });
            scene.idle(1);
        }

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_8")
            .independent();
        // Pin the package in place while text is showing
        for (int i = 0; i < 60; i++) {
            scene.world().modifyEntity(pkg3, e -> { e.setDeltaMovement(0, 0, 0); e.setPos(pkgStart.x, pkgStart.y, crawlStart.z); });
            scene.idle(1);
        }
        scene.world().modifyEntity(pkg3, Entity::discard);
        scene.idle(10);

        // ============================== STAGE 4 ==============================
        // Cardboard actor, package filter → passes
        scene.addKeyframe();

        scene.overlay().showControls(filterHint, Pointing.RIGHT, 60)
            .withItem(createItem("cardboard_package_12x12"));
        scene.idle(20);

        ElementLink<EntityElement> actor4 = spawnActor(scene, beltStart, 0);
        scene.idle(20);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_9")
            .placeNearTarget()
            .pointAt(beltStart.add(0, 1, 0));
        scene.idle(55);

        scene.world().modifyEntity(actor4, Entity::discard);
        ElementLink<EntityElement> pkg4 = spawnPackage(scene, pkgStart);
        ridePackage(scene, pkg4, pkgStart, pkgLength, travelTicks);
        scene.world().modifyEntity(pkg4, Entity::discard);
        ElementLink<EntityElement> actor4End = spawnActor(scene, beltEnd, 0);

        scene.overlay().showText(60)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_10")
            .independent();
        scene.idle(70);
        scene.world().modifyEntity(actor4End, Entity::discard);
        scene.idle(10);

        // ============================== STAGE 5 ==============================
        // Leather armor + crawl, leather helmet filter → passes
        scene.addKeyframe();

        scene.overlay().showControls(filterHint, Pointing.RIGHT, 60)
            .withItem(dyedLeather(Items.LEATHER_HELMET, LEATHER_COLOR));
        scene.idle(20);

        PonderSceneHelper.CrawlingActor actor5 = spawnCrawlingActor(scene, beltStart);
        scene.idle(15);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_11")
            .placeNearTarget()
            .pointAt(beltStart.add(0, 1, 0));
        scene.idle(55);

        // Ensure open state, show trapdoor, walk to it
        scene.world().setBlock(trapdoorPos, trapdoorOpen, false);
        ElementLink<WorldSectionElement> trapdoorView5 =
            scene.world().showIndependentSection(util.select().position(4, 2, 2), Direction.DOWN);
        scene.idle(10);

        for (int i = 0; i <= walkUpTicks; i++) {
            double z = beltStart.z + ((crawlStart.z - beltStart.z) * i / walkUpTicks);
            moveCrawlingActor(scene, actor5, beltStart.x, beltStart.y, z);
            scene.idle(1);
        }

        // Close trapdoor + crawl
        scene.world().setBlock(trapdoorPos, trapdoorClosed, false);
        setCrawling(scene, actor5, true);
        scene.idle(10);

        for (int i = 0; i <= travelTicks; i++) {
            final double z = crawlStart.z + ((beltEnd.z - crawlStart.z) * i / travelTicks);
            moveCrawlingActor(scene, actor5, crawlStart.x, crawlStart.y, z);
            scene.idle(1);
        }

        setCrawling(scene, actor5, false);

        scene.overlay().showText(60)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_12")
            .independent();
        scene.idle(70);
        hideCrawlingActor(scene, actor5);

        scene.special().hideElement(trapdoorView5, Direction.UP);
        scene.world().restoreBlocks(util.select().position(4, 2, 2));
        scene.idle(10);

        // ============================== STAGE 6 ==============================
        // Wrench filter — two actors staggered, either hand works
        scene.addKeyframe();

        scene.overlay().showControls(filterHint, Pointing.RIGHT, 60)
            .withItem(createItem("wrench"));
        scene.idle(20);

        int staggerTicks = travelTicks / 2; // second enters while first is halfway
        Vec3 behindStart = beltStart.add(0, 0, -1.5);

        // Both spawn standing — first at beltStart, second behind
        PonderSceneHelper.CrawlingActor actor6Main = spawnCrawlingActor(scene, beltStart,
            mob -> mob.setItemSlot(EquipmentSlot.MAINHAND, createItem("wrench")));
        PonderSceneHelper.CrawlingActor actor6Off = spawnCrawlingActor(scene, behindStart,
            mob -> mob.setItemSlot(EquipmentSlot.OFFHAND, createItem("wrench")));
        scene.idle(15);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_13")
            .placeNearTarget()
            .pointAt(beltStart.add(0, 1, 0));

        // Clockwise spin with a pause at 180° so both wrenches face the camera.
        // During the hold phase we still tick the offset every frame so the
        // renderer's prev/current interpolation settles and doesn't stutter.
        int spinIn = 20, spinHold = 15, spinOut = 20;
        for (int i = 0; i <= spinIn; i++) {
            float angle = 180f * i / spinIn;
            setCrawlingActorYRot(scene, actor6Main, angle);
            setCrawlingActorYRot(scene, actor6Off, angle);
            scene.idle(1);
        }
        for (int i = 0; i < spinHold; i++) {
            setCrawlingActorYRot(scene, actor6Main, 180f);
            setCrawlingActorYRot(scene, actor6Off, 180f);
            scene.idle(1);
        }
        for (int i = 0; i <= spinOut; i++) {
            float angle = 180f + 180f * i / spinOut;
            setCrawlingActorYRot(scene, actor6Main, angle);
            setCrawlingActorYRot(scene, actor6Off, angle);
            scene.idle(1);
        }
        // Settle: one more tick at 360° so prev catches current before scene continues.
        setCrawlingActorYRot(scene, actor6Main, 360f);
        setCrawlingActorYRot(scene, actor6Off, 360f);
        scene.idle(1);

        // Ensure open state, show trapdoor, walk first actor to it
        scene.world().setBlock(trapdoorPos, trapdoorOpen, false);
        ElementLink<WorldSectionElement> trapdoorView6 =
            scene.world().showIndependentSection(util.select().position(4, 2, 2), Direction.DOWN);
        scene.idle(10);

        for (int i = 0; i <= walkUpTicks; i++) {
            double z = beltStart.z + ((crawlStart.z - beltStart.z) * i / walkUpTicks);
            moveCrawlingActor(scene, actor6Main, beltStart.x, beltStart.y, z);
            scene.idle(1);
        }

        // Close trapdoor, first actor starts crawling
        scene.world().setBlock(trapdoorPos, trapdoorClosed, false);
        setCrawling(scene, actor6Main, true);
        scene.idle(10);

        // First leads; second walks up then crawls once it reaches the trapdoor
        for (int i = 0; i <= travelTicks + walkUpTicks; i++) {
            // First actor crawling through, then walks standing to make room
            if (i <= travelTicks) {
                final double z = crawlStart.z + ((beltEnd.z - crawlStart.z) * i / travelTicks);
                moveCrawlingActor(scene, actor6Main, crawlStart.x, crawlStart.y, z);
            }
            if (i == travelTicks) {
                setCrawling(scene, actor6Main, false);
            }
            if (i > travelTicks) {
                int extra = i - travelTicks;
                final double z = beltEnd.z + (1.5 * extra / walkUpTicks);
                moveCrawlingActor(scene, actor6Main, crawlStart.x, crawlStart.y, z);
            }

            // Second actor: walk standing from behind to crawlStart
            if (i < walkUpTicks) {
                double z = behindStart.z + ((crawlStart.z - behindStart.z) * i / walkUpTicks);
                moveCrawlingActor(scene, actor6Off, behindStart.x, behindStart.y, z);
            }
            // Open trapdoor before second actor arrives, close when they reach the spot
            if (i == walkUpTicks - 5) {
                scene.world().setBlock(trapdoorPos, trapdoorOpen, false);
            }
            if (i == walkUpTicks) {
                scene.world().setBlock(trapdoorPos, trapdoorClosed, false);
                setCrawling(scene, actor6Off, true);
            }
            // Second actor crawling through
            if (i >= walkUpTicks) {
                int j = i - walkUpTicks;
                final double z = crawlStart.z + ((beltEnd.z - crawlStart.z) * j / travelTicks);
                moveCrawlingActor(scene, actor6Off, crawlStart.x, crawlStart.y, z);
            }
            if (i == travelTicks + walkUpTicks) {
                setCrawling(scene, actor6Off, false);
            }
            scene.idle(1);
        }

        scene.overlay().showText(60)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_14")
            .independent();
        scene.idle(70);

        hideCrawlingActor(scene, actor6Main);
        hideCrawlingActor(scene, actor6Off);
        scene.special().hideElement(trapdoorView6, Direction.UP);
        scene.world().restoreBlocks(util.select().position(4, 2, 2));
        scene.idle(10);

        // ============================== STAGE 7 ==============================
        // Cardboard actor with wrench, disguised as package — wrench filter rejects
        scene.addKeyframe();

        scene.overlay().showControls(filterHint, Pointing.RIGHT, 60)
            .withItem(createItem("wrench"));
        scene.idle(20);

        ElementLink<EntityElement> actor7 = spawnActor(scene, beltStart, 0,
            stand -> stand.setItemSlot(EquipmentSlot.MAINHAND, createItem("wrench")));
        scene.idle(20);

        scene.overlay().showText(50)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_15")
            .placeNearTarget()
            .pointAt(beltStart.add(0, 1, 0));
        scene.idle(55);

        // Disguise as package, travel toward tunnel, rejected at entry
        scene.world().modifyEntity(actor7, Entity::discard);
        ElementLink<EntityElement> pkg7 = spawnPackage(scene, pkgStart);
        int approachTicks7 = 20;
        for (int i = 0; i <= approachTicks7; i++) {
            final double z = pkgStart.z + ((crawlStart.z - pkgStart.z) * i / approachTicks7);
            scene.world().modifyEntity(pkg7, e -> { e.setDeltaMovement(0, 0, 0); e.setPos(pkgStart.x, pkgStart.y, z); });
            scene.idle(1);
        }

        scene.overlay().showText(60)
            .text("cardboarded_conveynience.ponder.belt_tunnel.text_16")
            .independent();
        // Pin the package in place while text is showing
        for (int i = 0; i < 60; i++) {
            scene.world().modifyEntity(pkg7, e -> { e.setDeltaMovement(0, 0, 0); e.setPos(pkgStart.x, pkgStart.y, crawlStart.z); });
            scene.idle(1);
        }
        scene.world().modifyEntity(pkg7, Entity::discard);
        scene.idle(10);
    }

    // ---- Private helpers ----

    private static void ridePackage(SceneBuilder scene, ElementLink<EntityElement> pkg,
                                    Vec3 from, double length, int ticks) {
        for (int i = 0; i <= ticks; i++) {
            final double z = from.z + (length * i / ticks);
            scene.world().modifyEntity(pkg, e -> {
                e.setDeltaMovement(0, 0, 0);
                e.setPos(from.x, from.y, z);
            });
            scene.idle(1);
        }
    }

    private static ItemStack dyedLeather(Item item, int color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color, true));
        return stack;
    }

    private static final int LEATHER_COLOR = 0x4A3728; // dark brown — scrappy rogue look
}
