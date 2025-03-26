package net.TakeruDavis.create_cardboarded_conveynience.utils;

import com.simibubi.create.foundation.render.PlayerSkyhookRenderer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;

public class SkyhookHelper {

    private static Set<UUID> hangingPlayers;

    private static void initLinked() {
        try {
            Field f = PlayerSkyhookRenderer.class.getDeclaredField("hangingPlayers");
            f.setAccessible(true);
            hangingPlayers = (Set<UUID>) f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static boolean isPlayerHanging(Player player) {
        if (hangingPlayers == null) {
            initLinked();
        }

        return hangingPlayers.contains(player.getUUID());
    }

}
