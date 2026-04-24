package net.takerudavis.cardboarded_conveynience.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON-based configuration for the mod.
 * Config file is saved to config/cardboarded_conveynience.json
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "cardboarded_conveynience.json";

    private static ModConfig INSTANCE;

    // Chute teleportation settings
    public int maxChutePathLength = 384;

    /**
     * Get the current config instance.
     */
    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    /**
     * Load config from file, or create default if it doesn't exist.
     */
    public static void load() {
        Path configPath = Platform.getConfigFolder().resolve(CONFIG_FILE);

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                INSTANCE = GSON.fromJson(json, ModConfig.class);
                CardboardedConveynience.LOGGER.info("Loaded config from {}", configPath);
            } catch (IOException e) {
                CardboardedConveynience.LOGGER.error("Failed to load config, using defaults", e);
                INSTANCE = new ModConfig();
                save();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
            CardboardedConveynience.LOGGER.info("Created default config at {}", configPath);
        }
    }

    /**
     * Save current config to file.
     */
    public static void save() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
        }

        Path configPath = Platform.getConfigFolder().resolve(CONFIG_FILE);

        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            CardboardedConveynience.LOGGER.error("Failed to save config", e);
        }
    }
}
