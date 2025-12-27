package net.takerudavis.cardboarded_conveynience;

import net.takerudavis.cardboarded_conveynience.advancement.ModCriteria;
import net.takerudavis.cardboarded_conveynience.config.ModConfig;
import net.takerudavis.cardboarded_conveynience.network.CardboardedNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CardboardedConveynience {
    public static final String MOD_ID = "cardboarded_conveynience";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Create: Cardboarded Conveynience initializing...");
        ModConfig.load();
        ModCriteria.register();
        CardboardedNetworking.init();
    }
}
